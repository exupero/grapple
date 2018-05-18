(ns grapple.core
  (:require-macros [grapple.util :refer [spy]])
  (:require [reagent.core :as r]
            [cognitect.transit :as transit]
            [re-frame.core :as rf]
            grapple.clojure
            grapple.clojurescript
            grapple.co-fx
            grapple.effects
            grapple.events
            grapple.subs
            grapple.views))

(defn mount-root []
  (r/render [grapple.views/page] (.getElementById js/document "app")))

(defn init! []
  (rf/dispatch-sync
    [:page/init
     {:init/tag-readers {'namespace grapple.render/->VarNamespace
                         'grapple.render.Renderable (comp grapple.render/->Generic :spec)}
      :init/tag-writers {grapple.render/Generic (transit/write-handler (constantly "grapple.render.Renderable") identity)}}])
  (mount-root))
