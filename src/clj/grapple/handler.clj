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
            [grapple.middleware :refer [wrap-middleware]]
            grapple.plot)
  (:import [java.io ByteArrayOutputStream]))

(def mount-target
  [:div#app [:h3 "Loading..."]])

(defn head []
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1"}]
   (include-css "/css/jslib/codemirror-5.3.0.css")
   (include-css "/css/jslib/codemirror-themes/neat.css")
   (include-css (if (env :dev) "/css/site.css" "/css/site.min.css"))
   [:script {:type "text/javascript"} "window.antiForgeryToken = '" *anti-forgery-token* "';"]])

(defn notebook-page []
  (html5
    (head)
    [:body {:class "body-container"}
     mount-target
     (include-js "//cdnjs.cloudflare.com/ajax/libs/mathjax/2.7.2/MathJax.js?config=TeX-MML-AM_CHTML")
     (include-js "/js/app.js")]))

(defn transit-response [body]
  (let [out (ByteArrayOutputStream. 4096)
        w (transit/writer out :json)]
    (transit/write w body)
    {:status 200
     :body (.toString out)}))

(defn nrepl-init-endpoint [req]
  (let [sid (with-open [conn (nrepl/connect :port 7888)]
              (-> (nrepl/client conn 1000)
                (nrepl/message {:op "clone"})
                first :new-session))]
    (transit-response sid)))

(defn nrepl-eval-endpoint [req]
  (let [rdr (transit/reader (req :body) :json)
        {:keys [code session-id eval-id]} (transit/read rdr)]
    (transit-response
      (with-open [conn (nrepl/connect :port 7888)]
        (-> (nrepl/client conn 1000)
          (nrepl/message
            {:op "eval"
             :code code
             :session session-id
             :id eval-id})
          doall)))))

(defn save-notebook [req]
  (let [rdr (transit/reader (req :body) :json)
        {:keys [filename blocks]} (transit/read rdr)]
    (spit (str "./" filename) (pr-str blocks))
    (transit-response {})))

(defn load-notebook [req]
  (let [rdr (transit/reader (req :body) :json)
        {:keys [filename]} (transit/read rdr)
        blocks (edn/read-string
                 {'grapple.plot.Vega grapple.plot/->Vega}
                 (slurp (str "./" filename)))]
    (transit-response blocks)))

(defroutes routes
  (GET "/" [] (notebook-page))
  (POST "/api/init" req (nrepl-init-endpoint req))
  (POST "/api/eval" req (nrepl-eval-endpoint req))
  (POST "/api/save" req (save-notebook req))
  (POST "/api/load" req (load-notebook req))
  
  (resources "/")
  (not-found "Not Found"))

(defonce nrepl-server (atom nil))

(when-not @nrepl-server
  (let [middleware (map resolve cider/cider-middleware)]
    (reset! nrepl-server
            (start-server
              :port 7888
              :handler (apply default-handler middleware)))))

(def app (wrap-middleware #'routes))
