(ns grapple.middleware
  (:require [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            #_[ring.middleware.reload :refer [wrap-reload]]
            ring.middleware.keyword-params
            ring.middleware.params
            #_[prone.middleware :refer [wrap-exceptions]]))

(defn wrap-middleware [handler]
  (-> handler
    (wrap-defaults site-defaults)
    #_wrap-exceptions
    #_wrap-reload
    ring.middleware.keyword-params/wrap-keyword-params
    ring.middleware.params/wrap-params))
