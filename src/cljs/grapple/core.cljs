(ns grapple.core
  (:require-macros [grapple.util :refer [spy]])
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            grapple.co-fx
            grapple.effects
            grapple.events
            grapple.subs
            grapple.views))

;; -------------------------
;; Initialize app

(defn mount-root []
  (r/render [grapple.views/notebook] (.getElementById js/document "app")))

(defn init! []
  (rf/dispatch-sync [:page/init])
  (mount-root))
