(ns grapple.vega
  (:require-macros [grapple.util :refer [spy]])
  (:require [reagent.core :as r]
            cljsjs.vega
            [grapple.render :refer [Renderable]]))

(defn render-vega [node spec]
  (-> (clj->js spec)
    js/vega.parse
    js/vega.View.
    (.renderer "svg")
    (.initialize node)
    .run))

(defn vega-plot [spec]
  (r/create-class
    {:reagent-render
     (fn [] [:span.graph])
     :component-did-mount
     (fn [this]
       (render-vega (r/dom-node this) spec))
     :component-will-update
     (fn [this [_ spec]]
       (render-vega (r/dom-node this) spec))}))

(defrecord Vega [spec]
  Renderable
  (render [_]
    [vega-plot spec]))
