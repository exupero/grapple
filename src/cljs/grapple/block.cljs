(ns grapple.block
  (:require-macros [grapple.util :refer [spy]])
  (:require [clojure.string :as string]
            [re-frame.core :as rf]
            [cljs-uuid-utils.core :as uuid]))

(defmulti convert (fn [_ t] t))

;; Components

(defmulti render :block/type)

;; Events

(rf/reg-event-db :block/codemirror
  (fn [db [_ id cm]]
    (assoc-in db [:page/blocks id :block/codemirror] cm)))

(rf/reg-event-fx :block/eval
  (fn [{:keys [db]} [_ id content]]
    {:block/eval {:eval/block-id id
                  :eval/content content
                  :eval/event-name (get-in db [:page/blocks id :block/eval-event])}}))

(rf/reg-event-fx :block/interrupt
  (fn [{:keys [db]} [_ id]]
    (when (get-in db [:page/blocks id :block/processing?])
      {:clojure/interrupt {:interrupt/eval-id (uuid/uuid-string id)
                           :interrupt/session-id (db :page/session-id)}})))

(rf/reg-event-fx :block/edit
  (fn [{:keys [db]} [_ id]]
    {:db (assoc-in db [:page/blocks id :block/active?] true)}))

(rf/reg-event-db :block/type
  (fn [db [_ id t]]
    (update-in db [:page/blocks id] convert t)))

;; Effects

(rf/reg-fx :block/eval
  (fn [{:keys [eval/block-id eval/content eval/event-name]}]
    (rf/dispatch [event-name block-id content])))
