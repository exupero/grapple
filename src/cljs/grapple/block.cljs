(ns grapple.block
  (:require-macros [grapple.util :refer [spy]])
  (:require [clojure.string :as string]
            [cljs.reader :as edn]
            [re-frame.core :as rf]
            [cljs-uuid-utils.core :as uuid]
            [grapple.codemirror :as cm]
            [grapple.render :as r]))

;; Components

(defn block [{:keys [block/results] :as b}]
  [:div
   [cm/codemirror b]
   [:div.results
    (when-let [values (seq (filter #(contains? % :value) results))]
      [:div.result__values {:key :value}
       (for [[i r] (map-indexed vector values)]
         [:div {:key i}
          [(-> r r/->renderable r/->component)]])])]])

;; Events

(rf/reg-event-db :block/codemirror
  (fn [db [_ id cm]]
    (assoc-in db [:page/blocks id :block/codemirror] cm)))

(rf/reg-event-fx :block/eval
  (fn [{:keys [db]} [_ id content]]
    {:block/eval {:eval/block-id id :eval/content content}}))

(rf/reg-event-fx :block/interrupt
  (fn [{:keys [db]} [_ id]]
    (when (get-in db [:page/blocks id :block/processing?])
      {:clojure/interrupt {:interrupt/eval-id (uuid/uuid-string id)
                           :interrupt/session-id (db :page/session-id)}})))

(rf/reg-event-fx :block/edit
  (fn [{:keys [db]} [_ id]]
    {:db (assoc-in db [:page/blocks id :block/active?] true)}))

;; Effects

(rf/reg-fx :block/eval
  (fn [{:keys [eval/block-id eval/content]}]
    (rf/dispatch [:clojurescript/eval block-id content])
    (rf/dispatch [:nav/ensure-focus-next block-id])))
