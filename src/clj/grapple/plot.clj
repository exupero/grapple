(ns grapple.plot)

(defrecord Vega [spec])

(defn scatter
  ([pts] (scatter pts nil))
  ([pts {:keys [width height]
         :or {width 300 height 300}}]
   (->Vega
     {:$schema "https://vega.github.io/schema/vega/v3.0.json"
      :width width
      :height width
      :data [{:name "source" :url "https://vega.github.io/vega/data/cars.json"}]
      :axes [{:scale "x" :grid true :domain false :orient "bottom" :tickCount 5}
             {:scale "y" :grid true :domain false :orient "left" :tickCount 5}]
      :scales [{:name "x" :type "linear" :round true :nice true :zero true :range "width"
                :domain {:data "source" :field "Horsepower"}}
               {:name "y" :type "linear" :round true :nice true :zero true :range "height"
                :domain {:data "source" :field "Miles_per_Gallon"}}
               {:name "size" :type "linear" :round true :nice false :zero true :range [4 361]
                :domain {:data "source" :field "Acceleration"}}]
      :marks [{:name "marks" :type "symbol" :from {:data "source"}
               :encode {:update {:x {:scale "x" :field "Horsepower"}
                                 :y {:scale "y" :field "Miles_per_Gallon"}
                                 :size {:scale "size" :field "Acceleration"}
                                 :shape {:value "circle"}
                                 :strokeWidth {:value 2}
                                 :opacity {:value 0.5}
                                 :stroke {:value "#4682b4"}
                                 :fill {:value "transparent"}}}}]})))
