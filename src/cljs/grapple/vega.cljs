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

(defrecord Vega [spec]
  Renderable
  (render [_]
    (r/create-class
      {:reagent-render
       (fn [] [:span])
       :component-did-mount
       (fn [this]
         (render-vega (r/dom-node this) spec))
       :component-will-update
       (fn [this _]
         (render-vega (r/dom-node this) spec))})))
