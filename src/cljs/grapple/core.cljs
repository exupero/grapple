(ns grapple.core
  (:require-macros [grapple.util :refer [spy]])
  (:require [reagent.core :as r]
            [cognitect.transit :as transit]
            [re-frame.core :as rf]
            grapple.co-fx
            grapple.effects
            grapple.events
            grapple.subs
            grapple.views
            grapple.renderable.table
            grapple.renderable.vega))

(def default-tags
  [{:tag 'grapple.plot.Vega
    :reader #(grapple.renderable.vega/->Vega (:spec %))
    :record grapple.renderable.vega/Vega}
   {:tag 'grapple.table.Table
    :reader #(grapple.renderable.table/->Table (:headings %) (:rows %))
    :record grapple.renderable.table/Table}])

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
