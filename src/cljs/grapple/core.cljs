(ns grapple.core
  (:require-macros [grapple.util :refer [spy]])
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            grapple.co-fx
            grapple.effects
            grapple.events
            grapple.subs
            grapple.views))

(defn mount-root []
  (r/render [grapple.views/page] (.getElementById js/document "app")))

(defn init-mathjax []
  (js/MathJax.Hub.Config
    (clj->js {:messageStyle "none"
              :showProcessingMessages false
              :skipStartupTypeset true
              :tex2jax {:inlineMath [["@@" "@@"]]}}))
  (js/MathJax.Hub.Configured))

(defn init! []
  (init-mathjax)
  (rf/dispatch-sync [:page/init])
  (mount-root))
