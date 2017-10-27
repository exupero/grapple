(ns grapple.middleware
  (:require [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            ring.middleware.keyword-params
            ring.middleware.params))

(defn wrap-middleware [handler]
  (-> handler
    (wrap-defaults site-defaults)
    ring.middleware.keyword-params/wrap-keyword-params
    ring.middleware.params/wrap-params))
