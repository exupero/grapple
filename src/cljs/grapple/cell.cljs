(ns grapple.cell
  (:require [cljs-uuid-utils.core :as uuid]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [grapple.render :refer [Renderable ->Error ->component]]))

(def pending ::pending)
(def interrupted ::interrupted)

(defrecord Cell [id opts value]
  Renderable
  (->component [_]
    (r/create-class
      {:reagent-render
       (fn []
         (cond
           (= pending value)
           (let [{[_ form] :form :keys [interruptible?]} opts
                 cancel (when interruptible?
                          [:span.cancel {:on-click #(rf/dispatch [:clojure/interrupt id])}])]
             (if-let [f (opts :form)]
               [:div.result__loading cancel "Evaluating " [:code (pr-str form)]]
               [:div.result__loading cancel "Evaluating..."]))
           (= interrupted value)
           [:div.result__interrupted "Interrupted"]
           (contains? value :error)
           [(->Error (value :error))]
           :else
           [(->component (value :value))]))})))

(defn cell
  ([opts] (cell opts pending))
  ([opts value] (cell (uuid/make-random-uuid) opts value))
  ([id opts value]
   (let [c (->Cell id opts value)]
     (rf/dispatch [:page/new-cell id c])
     c)))

(defn pending? [cell]
  (= pending (:value cell)))

(defn update-cell [db eval-id value source]
  (update-in db [:page/cells eval-id]
             (fn [cell]
               (if (= source (-> cell :opts :source))
                 (assoc cell :value value)
                 cell))))
