(ns grapple.clojurescript
  (:require-macros [grapple.util :refer [spy]])
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [cljs.js :refer [empty-state eval js-eval]]
            [cljs-uuid-utils.core :as uuid]
            [cljs.tools.reader :as edn]
            [cljs.tools.reader.reader-types :refer [string-push-back-reader]]
            [grapple.render :as render]))

(defrecord Yield [block-id cell-id value]
  IFn
  (-invoke [_]
    (rf/dispatch [:cell/update block-id cell-id value])))

(defn data-readers [block-id cell-id]
  {'clj (fn [form]
          (let [eval-id (uuid/make-random-uuid)]
            (rf/dispatch [:clojure/eval block-id eval-id (pr-str form)])
            (render/cell eval-id)))
   'md render/->Markdown
   'reagent render/->Reagent
   'yield #(list (->Yield block-id cell-id %))})

(defonce state (empty-state))

(defn read-forms [block-id s]
  (when (and s (not (identical? s "")))
    (let [reader (string-push-back-reader s)]
      (loop [forms []]
        (let [cell-id (uuid/make-random-uuid)]
          (binding [edn/*data-readers* (data-readers block-id cell-id)]
            (let [form (edn/read {:eof :eof} reader)]
              (if (= :eof form)
                forms
                (recur (conj forms {:cell-id cell-id :form form}))))))))))

(defn eval-str [id code]
  (doseq [{:keys [cell-id form]} (read-forms id code)]
    (eval state
          form
          {:eval js-eval
           :source-map true
           :context :expr}
          #(rf/dispatch [:clojurescript/result id (render/cell cell-id (:value %))]))))

;; Events

(rf/reg-event-fx :clojurescript/eval
  (fn [{:keys [db]} [_ id content]]
    {:db (update-in db [:page/blocks id] merge
                    {:block/content content
                     :block/results []})
     :clojurescript/eval {:eval/id id
                          :eval/code content}}))

(rf/reg-event-db :clojurescript/result
  (fn [db [_ id result]]
    (update-in db [:page/blocks id :block/results] conj result)))

(rf/reg-event-db :cell/update
  (fn [db [_ block-id cell-id value]]
    (render/update-cell db block-id cell-id value)))

;; Effects

(rf/reg-fx :clojurescript/eval
  (fn [{:keys [eval/id eval/code]}]
    (eval-str id code)))
