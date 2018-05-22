(ns grapple.handler
  (:require [clojure.java.io :as io]
            [clojure.tools.nrepl :as nrepl]
            [clojure.tools.nrepl.server :refer [start-server stop-server default-handler]]
            [clojure.edn :as edn]
            [compojure.core :refer [GET POST routes]]
            [compojure.route :refer [not-found resources]]
            [hiccup.page :refer [include-js include-css html5]]
            [config.core :refer [env]]
            [cognitect.transit :as transit]
            [cider.nrepl :as cider]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer [get-sch-adapter]]
            [taoensso.sente.packers.transit :as sente-transit]
            [grapple.middleware :refer [wrap-middleware]]
            [grapple.namespace :refer [required]]
            [grapple.util :refer [spy]])
  (:import [java.io ByteArrayOutputStream]))

(declare nrepl-connection)

(def mount-target
  [:div#app [:h3 "Loading..."]])

(defn head []
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1"}]
   (include-css "/css/jslib/codemirror-5.3.0.css")
   (include-css "/css/jslib/codemirror-themes/neat.css")
   (include-css "/css/screen.css")])

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
  (with-open [conn (@nrepl-connection)]
    (-> (nrepl/client conn Long/MAX_VALUE)
      (nrepl/message {:op :clone})
      first :new-session ?reply-fn)))

(defmethod event :clojure/eval [{:keys [send-fn uid ?data]}]
  (let [{:keys [code session-id eval-id return]} ?data]
    (with-open [conn (@nrepl-connection)]
      (let [results (-> (nrepl/client conn Long/MAX_VALUE)
                      (nrepl/message
                        {:op :eval
                         :code code
                         :session session-id
                         :id eval-id}))]
        (doseq [result results]
          (send-fn uid [:clojure/result {:return return :result result}]))))))

(defmethod event :clojure/interrupt [{:keys [send-fn uid ?data]}]
  (let [{:keys [session-id eval-id return]} ?data]
    (with-open [conn (@nrepl-connection)]
      (loop []
        (let [results (-> (nrepl/client conn Long/MAX_VALUE)
                        (nrepl/message
                          {:op :interrupt
                           :session session-id
                           :interrupt-id eval-id})
                        doall)
              statuses (into #{} (mapcat :status) results)]
          (when-not (contains? statuses "interrupt-id-mismatch")
            (recur))))
      (send-fn uid [:clojure/interrupted {:return return}]))))

(defmethod event :clojure/stacktrace [{:keys [send-fn uid ?data]}]
  (let [{:keys [session-id eval-id return]} ?data]
    (with-open [conn (@nrepl-connection)]
      (let [results (-> (nrepl/client conn Long/MAX_VALUE)
                      (nrepl/message
                        {:op :stacktrace
                         :session session-id}))]
        (doseq [result results]
          (send-fn uid [:clojure/stacktrace {:return return :result result}]))))))

(defmethod event :clojurescript/dependency [{:keys [?reply-fn ?data]}]
  (let [{:keys [path macros]} ?data
        file (io/file (format "./src/hosted/%s.%s"
                              path  (if macros "clj" "cljs")))]
    (if (.exists file)
      (?reply-fn {:lang :clj :source (slurp file)})
      (?reply-fn nil))))

(defmethod event :page/save [{:keys [?reply-fn ?data]}]
  (let [{:keys [filename blocks]} ?data]
    (spit (str "./" filename) (pr-str blocks))
    (?reply-fn true)))

(defmethod event :page/load [{:keys [?reply-fn ?data]}]
  (let [{:keys [filename]} ?data
        blocks (edn/read-string (slurp (str "./" filename)))]
    (?reply-fn blocks)))

(defn router [ring-ajax-get-or-ws-handshake ring-ajax-post]
  (routes
    (GET "/" [] (notebook-page))
    (GET  "/ws" req (ring-ajax-get-or-ws-handshake req))
    (POST "/ws" req (ring-ajax-post req))
    (resources "/")
    (not-found "Not Found")))

(def packer (sente-transit/->TransitPacker :json {:handlers {}} {}))

(defonce nrepl-connection (atom nil))
(defonce nrepl-server (atom nil))
(defonce app (atom nil))
(defonce channel-socket (atom nil))

(defn start-notebook [nrepl-port]
  (when-not (or @app @channel-socket)
    (let [chsk (sente/make-channel-socket! (get-sch-adapter) {:packer packer})]
      (reset! channel-socket chsk)
      (reset! app (wrap-middleware (router (chsk :ajax-get-or-ws-handshake-fn) (chsk :ajax-post-fn))))
      (sente/start-chsk-router! (chsk :ch-recv) event {:simple-auto-threading? true})))
  (when-not @nrepl-server
    (reset! nrepl-server (start-server
                           :port nrepl-port
                           :handler (apply default-handler (map resolve cider/cider-middleware))))
    (reset! nrepl-connection #(nrepl/connect :port nrepl-port))))

(defn stop-notebook []
  (when-let [n @nrepl-server]
    (stop-server n)
    (reset! nrepl-server nil))
  (when @app (reset! app nil))
  (when @channel-socket
    (reset! channel-socket nil)))
