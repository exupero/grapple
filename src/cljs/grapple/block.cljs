(ns grapple.block
  (:require-macros [grapple.util :refer [spy]])
  (:require [clojure.string :as string]
            [cljs.reader :as edn]
            [re-frame.core :as rf]
            [cljs-uuid-utils.core :as uuid]
            [grapple.codemirror :as cm]
            [grapple.render :as r]))

;; Components

(defn block [{:keys [block/id block/active? block/content block/results] :as b}]
  [:div
   [cm/codemirror id active? content]
   (when (seq results)
     [:div.results
      (for [{:keys [id] :as cell} results]
        [:div {:key id}
         [(r/->component cell)]])])])

;; Events

(rf/reg-event-db :block/codemirror
  (fn [db [_ id cm]]
    (assoc-in db [:page/blocks id :block/codemirror] cm)))

(rf/reg-event-fx :block/eval
  (fn [{:keys [db]} [_ id content]]
    {:block/eval {:eval/block-id id :eval/content content}}))

(rf/reg-event-fx :block/edit
  (fn [{:keys [db]} [_ id]]
    {:db (assoc-in db [:page/blocks id :block/active?] true)}))

;; Effects

(rf/reg-fx :block/eval
  (fn [{:keys [eval/block-id eval/content]}]
    (rf/dispatch [:clojurescript/eval block-id content])
    (rf/dispatch [:nav/ensure-focus-next block-id])))
