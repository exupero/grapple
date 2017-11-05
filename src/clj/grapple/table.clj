(ns grapple.table
  (:require [grapple.render :refer [->Renderable]])
  (:import [grapple.render Renderable]))

(def cell-style
  {:border "1px solid hsl(0, 0%, 73%)"
   :padding "0.3rem 0.5rem"})

(defn matrix [rows]
  (->Renderable
    {:dom [:table {:style {:border-collapse "collapse"}}
           [:tbody {:key "body"}
            (for [[i row] (map-indexed vector rows)]
              [:tr {:key i}
               (for [[j cell] (map-indexed vector row)]
                 [:td
                  {:key j
                   :style cell-style}
                  (if (instance? Renderable cell)
                    cell (pr-str cell))])])]]}))

(defn table [maps]
  (let [headings (keys (first maps))
        rows (map (fn [m]
                    (map #(get m %) headings))
                  maps)]
    (->Renderable
      {:dom [:table {:style {:border-collapse "collapse"}}
             [:thead {:key "headings"}
              [:tr
               (for [[i heading] (map-indexed vector headings)]
                 [:td
                  {:key i
                   :style (merge cell-style
                                 {:background "hsl(210, 80%, 95%)"})}
                  (if (instance? Renderable heading)
                    heading (pr-str heading))])]]
             [:tbody {:key "body"}
              (for [[i row] (map-indexed vector rows)]
                [:tr {:key i}
                 (for [[j cell] (map-indexed vector row)]
                   [:td
                    {:key j
                     :style cell-style}
                    (if (instance? Renderable cell)
                      cell (pr-str cell))])])]]})))
