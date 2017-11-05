(ns grapple.plot
  (:require [grapple.render :refer [->Renderable]])
  (:import [java.util UUID]))

(def gen-id #(str (UUID/randomUUID)))

(defn vega [spec]
  (->Renderable
    {:data (assoc spec :$schema "https://vega.github.io/schema/vega/v3.0.json")
     :scripts ["https://vega.github.io/vega/vega.min.js"]
     :dom [:span]
     :update "new vega.View(vega.parse(data)).renderer('svg').initialize(node).run()"}))

(defn scatter [pairs]
  (let [id (gen-id)]
    (vega {:width 250 :height 250
           :data [{:name id
                   :values (map (fn [[x y]] {:x x :y y}) pairs)}]
           :axes [{:scale :x :domain true :labels true :orient :bottom}
                  {:scale :y :domain true :labels true :orient :left}]
           :scales [{:name :x :type :linear :nice true :zero true :range :width
                     :domain {:data id :field "x"}}
                    {:name "y" :type :linear :nice true :zero true :range :height
                     :domain {:data id :field "y"}}]
           :marks [{:type :symbol :from {:data id}
                    :encode {:update {:x {:scale :x :field :x}
                                      :y {:scale :y :field :y}
                                      :size {:value 100}
                                      :shape {:value :circle}
                                      :fill {:value :steelblue}}}}]})))

(defn contour
  ([pairs] (contour pairs nil))
  ([pairs {:keys [show-points? levels]
           :or {show-points? false levels 5}}]
   (let [data-id (gen-id)
         contours-id (gen-id)
         w 250
         h 250]
     (vega {:width w :height h
            :data [{:name data-id
                    :values (map (fn [[x y]] {:x x :y y}) pairs)}
                   {:name contours-id :source data-id
                    :transform [{:type :contour :size [w h] :count levels
                                 :x {:expr "scale('x', datum.x)"}
                                 :y {:expr "scale('y', datum.y)"}}]}]
            :axes [{:scale :x :domain true :labels true :orient :bottom}
                   {:scale :y :domain true :labels true :orient :left}]
            :scales [{:name :x :type :linear :nice true :zero true :range :width
                      :domain {:data data-id :field :x}}
                     {:name :y :type :linear :nice true :zero true :range :height
                      :domain {:data data-id :field :y}}
                     {:name :color :type :sequential :zero true :range :heatmap
                      :domain {:data contours-id :field :value}}]
            :config {:range {:heatmap {:scheme :greenblue}}}
            :marks (remove nil? [{:type :path :from {:data contours-id}
                                  :encode {:enter {:stroke {:value "#888"}
                                                   :strokeWidth {:value 1}
                                                   :fill {:scale :color :field :value}
                                                   :fillOpacity {:value 0.35}}}
                                  :transform [{:type :geopath :field :datum}]}
                                 (when show-points?
                                   {:type :symbol :from {:data data-id}
                                    :encode {:update {:x {:scale :x :field :x}
                                                      :y {:scale :y :field :y}
                                                      :size {:value 4}
                                                      :fill {:value :black}}}})])}))))

(defn compose [& plots]
  (reduce
    (fn [acc plot]
      (let [plot (get-in plot [:spec :data])]
        (update-in acc [:spec :data]
                   #(-> %
                      (update :data conj (get-in plot [:data 0]))
                      (update :marks conj (get-in plot [:marks 0]))))))
    (update-in (first plots) [:spec :data] merge
               {:autosize "none"
                :padding {:top 10 :left 55 :bottom 40 :right 10}})
    (rest plots)))
