(ns grapple.clojure
  (:require-macros [grapple.util :refer [spy]])
  (:require [clojure.string :as string]
            [reagent.core :as r]
            [cljs-uuid-utils.core :as uuid]
            [taoensso.sente :as sente]
            [taoensso.sente.packers.transit :as sente-transit]
            [re-frame.core :as rf]
            [grapple.codemirror :as cm]
            [grapple.render :refer [update-cell]]))

(defonce chsk-send! (atom nil))

;; Events

(rf/reg-event-fx :chsk/ws-ping
  (fn [_ _]
    {}))

(rf/reg-event-fx :clojure/init-session
  (fn [_ _]
    {:clojure/init-session {:init/on-success #(rf/dispatch [:page/session-id %])}}))

(rf/reg-event-fx :clojure/eval
  [(rf/inject-cofx :generator/uuid)]
  (fn [{:keys [db] generate-uuid :generator/uuid} [_ block-id eval-id content]]
    (when-not (string/blank? content)
      {:clojure/eval {:eval/code content
                      :eval/session-id (db :page/session-id)
                      :eval/eval-id (uuid/uuid-string block-id)
                      :eval/return {:block-id block-id :eval-id eval-id}}})))

(rf/reg-event-fx :clojure/result
  (fn [{:keys [db]} [_ {{:keys [block-id eval-id]} :return :keys [result]}]]
    (if (contains? result :ex)
      {:clojure/stacktrace {:stacktrace/eval-id eval-id
                            :stacktrace/session-id (db :page/session-id)}}
      (let [db (assoc db :page/session-id (result :session))]
        (cond
          (contains? (set (result :status)) "done") nil
          eval-id {:db (update-cell db block-id eval-id result)}
          :else {:db (update-in db [:page/blocks block-id :block/results] conj result)})))))

(rf/reg-event-db :clojure/interrupted
  (fn [db [_ {{:keys [block-id eval-id]} :return}]]
    (let [block-id (uuid/make-uuid-from eval-id)]
      (assoc-in db [:page/blocks block-id :block/processing?] false))))

(rf/reg-event-fx :clojure/stacktrace
  (fn [{:keys [db]} [_ {{:keys [block-id eval-id]} :return :keys [result]}]]
    (when-not (contains? (set (result :status)) "done")
      {:db (update-in db [:page/blocks block-id :block/results] conj result)})))

;; Effects

(defmulti event-message-handler :id)

(defmethod event-message-handler :chsk/handshake [_]
  (rf/dispatch [:clojure/init-session]))

(defmethod event-message-handler :chsk/state [_])

(defmethod event-message-handler :chsk/recv [{:keys [?data]}]
  (rf/dispatch ?data))

(rf/reg-fx :clojure/init
  (fn [{:keys [ws-init/write-handlers]}]
    (let [packer (sente-transit/->TransitPacker :json {:handlers write-handlers} {})
          {:keys [ch-recv send-fn]} (sente/make-channel-socket! "/ws" {:type :auto :packer packer})]
      (reset! chsk-send! send-fn)
      (sente/start-client-chsk-router! ch-recv event-message-handler))))

(rf/reg-fx :clojure/init-session
  (fn [{:keys [init/on-success]}]
    (@chsk-send! [:clojure/init] js/Number.MAX_SAFE_INTEGER on-success)))

(rf/reg-fx :clojure/eval
  (fn [{:keys [eval/code eval/session-id eval/eval-id eval/return]}]
    (@chsk-send! [:clojure/eval {:code code
                                 :session-id session-id
                                 :eval-id eval-id
                                 :return return}])))

(rf/reg-fx :clojure/interrupt
  (fn [{:keys [interrupt/session-id interrupt/eval-id interrupt/return]}]
    (@chsk-send! [:clojure/interrupt {:session-id session-id
                                      :eval-id eval-id
                                      :return return}])))

(rf/reg-fx :clojure/stacktrace
  (fn [{:keys [stacktrace/session-id stacktrace/eval-id stacktrace/return]}]
    (@chsk-send! [:clojure/stacktrace {:session-id session-id
                                       :eval-id eval-id
                                       :return return}])))

(rf/reg-fx :clojurescript/load-dependency
  (fn [{:keys [dependency/definition dependency/on-success]}]
    (@chsk-send! [:clojurescript/dependency definition] 5000 on-success)))

(rf/reg-fx :page/save
  (fn [{:keys [save/filename save/blocks save/on-success]}]
    (@chsk-send! [:page/save {:filename filename :blocks blocks}] 5000 on-success)))

(rf/reg-fx :page/load
  (fn [{:keys [load/filename load/on-success]}]
    (@chsk-send! [:page/load {:filename filename}] 5000 on-success)))
