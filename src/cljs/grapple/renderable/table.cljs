(ns grapple.renderable.table
  (:require [reagent.core :as r]
            [grapple.render :refer [Renderable]]))

(defrecord Table [headings rows]
  Renderable
  (render [_]
    (r/create-class
      {:reagent-render
       (fn [_]
         [:table {:style {:border-collapse "collapse"}}
          (when headings
            [:thead {:key "headings"}
             [:tr
              (for [[i heading] (map-indexed vector headings)]
                [:td
                 {:key i
                  :style {:background "hsl(210, 80%, 95%)"
                          :border "1px solid hsl(0, 0%, 73%)"
                          :padding "0.3rem 0.5rem"
                          :text-align "center"}}
                 (pr-str heading)])]])
          [:tbody {:key "body"}
           (for [[i row] (map-indexed vector rows)]
             [:tr {:key i}
              (for [[j cell] (map-indexed vector row)]
                [:td
                 {:key j
                  :style {:border "1px solid hsl(0, 0%, 73%)"
                          :padding "0.3rem 0.5rem"}}
                 cell])])]])})))
