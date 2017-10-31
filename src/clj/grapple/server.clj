(ns grapple.server
  (:require [org.httpkit.server :as server]
            [grapple.handler :refer [start-notebook stop-notebook app]])
  (:gen-class))

(defonce server (atom nil))

(defn start-server [opts]
  (start-notebook (or (opts :nrepl-port) 3001))
  (let [port (or (opts :port) 3000)]
    (reset! server (server/run-server @app {:port port :join? false}))
    (println "Started server on port" port)))

(defn stop-server []
  (when-let [s @server]
    (.stop s)
    (reset! server nil))
  (stop-notebook))

(defn -main [& args]
  (let [opts (-> (apply hash-map args)
               (update :port #(when % (Integer/parseInt %)))
               (update :nrepl-port #(when % (Integer/parseInt %))))]
    (start-server opts)))
