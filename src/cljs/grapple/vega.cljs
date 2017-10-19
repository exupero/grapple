(ns grapple.vega
  (:require [reagent.core :as r]
            cljsjs.vega
            [grapple.render :refer [Renderable]]))

(defn vega-plot [spec]
  (r/create-class
    {:reagent-render
     (fn [] [:div.graph])
     :component-did-mount
     (fn [this]
       (-> (clj->js spec)
         js/vega.parse
         js/vega.View.
         (.renderer "svg")
         (.initialize (r/dom-node this))
         .run))}))

(defrecord Vega [spec]
  Renderable
  (render [_]
    [vega-plot spec]))
