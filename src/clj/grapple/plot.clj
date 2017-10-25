(ns grapple.plot
  (:import [java.util UUID]))

(defrecord Vega [spec])

(defn scatter [pairs]
  (let [id (str (UUID/randomUUID))]
    (->Vega
      {:$schema "https://vega.github.io/schema/vega/v3.0.json"
       :width 250
       :height 250
       :autosize "none"
       :padding {:top 10 :left 55 :bottom 40 :right 10}
       :data [{:name id
               :values (map (fn [[x y]] {:x x :y y}) pairs)}]
       :axes [{:scale "x" :domain true :labels true :orient "bottom"}
              {:scale "y" :domain true :labels true :orient "left"}]
       :scales [{:name "x" :type "linear" :nice true :zero true :range "width"
                 :domain {:data id :field "x"}}
                {:name "y" :type "linear" :nice true :zero true :range "height"
                 :domain {:data id :field "y"}}]
       :marks [{:type "symbol" :from {:data id}
                :encode {:update {:x {:scale "x" :field "x"}
                                  :y {:scale "y" :field "y"}
                                  :size {:value 100}
                                  :shape {:value "circle"}
                                  :fill {:value "steelblue"}}}}]})))

(defn compose [& plots]
  (reduce
    (fn [acc plot]
      (-> acc
        (update-in [:spec :data] conj (get-in plot [:spec :data 0]))
        (update-in [:spec :marks] conj (get-in plot [:spec :marks 0]))))
    plots))
