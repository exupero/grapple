(ns grapple.handler
  (:require [clojure.tools.nrepl :as nrepl]
            [clojure.tools.nrepl.server :refer [start-server default-handler]]
            [clojure.edn :as edn]
            [compojure.core :refer [GET POST defroutes]]
            [compojure.route :refer [not-found resources]]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
            [hiccup.page :refer [include-js include-css html5]]
            [config.core :refer [env]]
            [cognitect.transit :as transit]
            [cider.nrepl :as cider]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer [get-sch-adapter]]
            [taoensso.sente.packers.transit :as sente-transit]
            [grapple.middleware :refer [wrap-middleware]]
            grapple.plot
            [grapple.util :refer [spy]])
  (:import [java.io ByteArrayOutputStream]))

(def packer (sente-transit/->TransitPacker :json {:handlers {}} {}))
(defonce channel-socket (sente/make-channel-socket! (get-sch-adapter) {:packer packer}))
(defonce ring-ajax-post                (channel-socket :ajax-post-fn))
(defonce ring-ajax-get-or-ws-handshake (channel-socket :ajax-get-or-ws-handshake-fn))
(defonce ch-chsk                       (channel-socket :ch-recv))
(defonce chsk-send!                    (channel-socket :send-fn))
(defonce connected-uids                (channel-socket :connected-uids))

(def mount-target
  [:div#app [:h3 "Loading..."]])

(defn head []
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1"}]
   (include-css "/css/jslib/codemirror-5.3.0.css")
   (include-css "/css/jslib/codemirror-themes/neat.css")
   (include-css (if (env :dev) "/css/site.css" "/css/site.min.css"))])

(defn notebook-page []
  (html5
    (head)
    [:body {:class "body-container"}
     mount-target
     (include-js "//cdnjs.cloudflare.com/ajax/libs/mathjax/2.7.2/MathJax.js?config=TeX-MML-AM_CHTML")
     (include-js "/js/app.js")]))

(defmulti event :id)

(defmethod event :chsk/uidport-open [_])

(defmethod event :chsk/uidport-close [_])

(defmethod event :chsk/ws-ping [_])

(defmethod event :clojure/init [{:keys [?reply-fn]}]
  (with-open [conn (nrepl/connect :port 7888)]
    (-> (nrepl/client conn Long/MAX_VALUE)
      (nrepl/message {:op "clone"})
      first :new-session ?reply-fn)))

(defmethod event :clojure/eval [{:keys [send-fn uid ?data]}]
  (let [{:keys [code session-id eval-id]} ?data]
    (with-open [conn (nrepl/connect :port 7888)]
      (let [results (-> (nrepl/client conn Long/MAX_VALUE)
                      (nrepl/message
                        {:op "eval"
                         :code code
                         :session session-id
                         :id eval-id}))]
        (doseq [result results]
          (send-fn uid [:eval/result {:eval-id eval-id :result result}]))))))

(defmethod event :page/save [{:keys [?reply-fn ?data]}]
  (let [{:keys [filename blocks]} ?data]
    (spit (str "./" filename) (pr-str blocks))
    (?reply-fn true)))

(defmethod event :page/load [{:keys [?reply-fn ?data]}]
  (let [{:keys [filename]} ?data
        blocks (edn/read-string
                 {'grapple.plot.Vega grapple.plot/->Vega}
                 (slurp (str "./" filename)))]
    (?reply-fn blocks)))

(defroutes routes
  (GET "/" [] (notebook-page))
  (GET  "/ws" req (ring-ajax-get-or-ws-handshake req))
  (POST "/ws" req (ring-ajax-post req))
  (resources "/")
  (not-found "Not Found"))

(defonce nrepl-server (atom nil))

(when-not @nrepl-server
  (let [middleware (map resolve cider/cider-middleware)]
    (reset! nrepl-server
            (start-server
              :port 7888
              :handler (apply default-handler middleware)))))

(defonce router
  (sente/start-chsk-router! ch-chsk event))

(def app (wrap-middleware #'routes))
