(ns grapple.middleware
  (:require [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            ring.middleware.keyword-params
            ring.middleware.params
            [prone.middleware :refer [wrap-exceptions]]
            [ring.middleware.reload :refer [wrap-reload]]))

(defn wrap-middleware [handler]
  (-> handler
      (wrap-defaults site-defaults)
      wrap-exceptions
      wrap-reload
      ring.middleware.keyword-params/wrap-keyword-params
      ring.middleware.params/wrap-params))
