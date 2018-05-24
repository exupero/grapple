(ns grapple.clojurescript
  (:require-macros [grapple.util :refer [spy]])
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [cljs.js :refer [empty-state eval js-eval]]
            [cljs-uuid-utils.core :as uuid]
            [cljs.tools.reader :as edn]
            [cljs.tools.reader.reader-types :refer [string-push-back-reader]]
            [grapple.cell :as cell]
            [grapple.render :as render]))

(defrecord Yield [cell-id value]
  IFn
  (-invoke [_]
    (rf/dispatch [:cell/update cell-id value])))

(defn data-readers [cell-id]
  {'clj (fn [form]
          (let [cell (cell/cell {:form (list 'quote form) :interruptible? true :source :clj})]
            (rf/dispatch [:clojure/eval (:id cell) (pr-str form)])
            cell))
   'md #(cell/cell {:form (list 'quote %) :source :cljs} {:value (render/->Markdown %)})
   'reagent #(cell/cell {:form (list 'quote %) :source :cljs} {:value (render/->Reagent %)})
   'yield #(list (->Yield cell-id %))})

(defonce state (empty-state))

(defn str->cells [s]
  (when (and s (not (identical? s "")))
    (let [reader (string-push-back-reader s)]
      (loop [cells []]
        (let [cell-id (uuid/make-random-uuid)]
          (binding [edn/*data-readers* (data-readers cell-id)]
            (let [form (edn/read {:eof :eof} reader)]
              (cond
                (= :eof form) cells
                (instance? cell/Cell form) (recur (conj cells form))
                :else (recur (conj cells (cell/cell cell-id {:form (list 'quote form) :source :cljs} cell/pending)))))))))))

(defn eval-cells [cells]
  (doseq [{{:keys [form]} :opts :keys [id value] :as cell} cells]
    (when (and (cell/pending? cell)
               (= :cljs (-> cell :opts :source)))
      (eval state
            (second form)
            {:eval js-eval
             :source-map true
             :context :expr
             :load #(rf/dispatch [:clojurescript/load-dependency %1 %2])}
            (fn [result]
              (if (-> result :value meta :reagent)
                (rf/dispatch [:cell/update id (update result :value render/->Reagent) :cljs])
                (rf/dispatch [:cell/update id result :cljs])))))))

;; Events

(rf/reg-event-fx :clojurescript/eval
  (fn [{:keys [db]} [_ block-id content]]
    (let [cells (str->cells content)]
      {:db (-> db
             (update :page/cells merge (into {} (map (juxt :id identity)) cells))
             (update-in [:page/blocks block-id] merge
                        {:block/content content
                         :block/results (map :id cells)}))
       :clojurescript/eval {:eval/cells cells}})))

(rf/reg-event-db :cell/update
  (fn [db [_ cell-id value source]]
    (cell/update-cell db cell-id value source)))

(rf/reg-event-fx :clojurescript/load-dependency
  (fn [_ [_ dep cb]]
    {:clojurescript/load-dependency {:dependency/definition dep
                                     :dependency/on-success cb}}))

;; Effects

(rf/reg-fx :clojurescript/eval
  (fn [{:keys [eval/cells]}]
    (eval-cells cells)))
