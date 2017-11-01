(ns grapple.core
  (:require-macros [grapple.util :refer [spy]])
  (:require [reagent.core :as r]
            [cognitect.transit :as transit]
            [re-frame.core :as rf]
            grapple.co-fx
            grapple.effects
            grapple.events
            grapple.subs
            grapple.vega
            grapple.views))

(def default-tags
  [{:tag 'grapple.plot.Vega
    :reader #(grapple.vega/->Vega (:spec %))
    :record grapple.vega/Vega}])

(defn mount-root []
  (r/render [grapple.views/page] (.getElementById js/document "app")))

(defn init! [tags]
  (let [tag-readers (into {} (map (juxt :tag :reader)) tags)
        tag-writers (into {} (map (fn [{:keys [record tag]}]
                                    [record
                                     (transit/write-handler (constantly (name tag)) identity)]))
                          tags)]
    (rf/dispatch-sync
      [:page/init
       {:init/tag-readers tag-readers
        :init/tag-writers tag-writers}]))
  (mount-root))
