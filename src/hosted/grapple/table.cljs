(ns grapple.table)

(def cell-style
  {:border "1px solid hsl(0, 0%, 65%)"
   :padding "0.3rem 0.5rem"})

(defn matrix [rows]
  ^:reagent
  [:table {:style {:border-collapse "collapse"}}
   [:tbody {:key "body"}
    (for [[i row] (map-indexed vector rows)]
      [:tr {:key i}
       (for [[j cell] (map-indexed vector row)]
         [:td
          {:key j
           :style cell-style}
          cell])])]])

(defn table [maps]
  (let [headings (keys (first maps))
        rows (map (fn [m]
                    (map #(get m %) headings))
                  maps)]
    ^:reagent
    [:table {:style {:border-collapse "collapse"}}
     [:thead {:key "headings"}
      [:tr
       (for [[i heading] (map-indexed vector headings)]
         [:td
          {:key i
           :style (merge cell-style
                         {:background "hsl(0, 0%, 96%)"})}
          heading])]]
     [:tbody {:key "body"}
      (for [[i row] (map-indexed vector rows)]
        [:tr {:key i}
         (for [[j cell] (map-indexed vector row)]
           [:td
            {:key j
             :style cell-style}
            cell])])]]))
