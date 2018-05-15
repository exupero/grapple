(ns grapple.block.clojure
  (:require-macros [grapple.util :refer [spy]])
  (:require [cljs.reader :as edn]
            [clojure.string :as string]
            [cljs-uuid-utils.core :as uuid]
            [taoensso.sente :as sente]
            [taoensso.sente.packers.transit :as sente-transit]
            [re-frame.core :as rf]
            [grapple.block :as b]
            [grapple.codemirror :as cm]
            [grapple.results :as r]
            [grapple.serialization :refer [with-evaled]]))

(def block
  {:block/type :block-type/clojure
   :block/abbr "clj"
   :block/codemirror-mode "clojure"
   :block/eval-event :clojure/eval
   :block/content ""
   :block/active? false})

(defmethod b/create :block-type/clojure []
  block)

(defmethod b/convert :block-type/clojure [b _]
  (merge b (dissoc block :block/content :block/active?)))

(defonce chsk-send! (atom nil))

;; Components

(defmethod b/render :block-type/clojure [{:keys [block/results] :as b}]
  [:div
   [cm/codemirror b]
   (when results [r/results results])])

;; Events

(rf/reg-event-fx :chsk/ws-ping
  (fn [_ _]
    {}))

(rf/reg-event-fx :clojure/init-session
  (fn [_ _]
    {:clojure/init-session {:init/on-success #(rf/dispatch [:page/session-id %])}}))

(rf/reg-event-fx :clojure/eval
  [(rf/inject-cofx :generator/uuid)]
  (fn [{:keys [db] generate-uuid :generator/uuid} [_ id content]]
    (let [{:keys [block/load-scripts?]} (get-in db [:page/blocks id])]
      (when-not (string/blank? content)
        {:db (update-in db [:page/blocks id] merge
                        {:block/content content
                         :block/processing? true
                         :block/results []})
         :clojure/eval {:eval/code content
                        :eval/session-id (db :page/session-id)
                        :eval/eval-id (uuid/uuid-string id)
                        :eval/load-scripts? load-scripts?}}))))

(rf/reg-event-fx :clojure/result
  (fn [{:keys [db]} [_ {:keys [eval-id result]}]]
    (if (contains? result :ex)
      {:clojure/stacktrace {:stacktrace/eval-id eval-id
                            :stacktrace/session-id (db :page/session-id)}}
      (let [block-id (uuid/make-uuid-from eval-id)
            db (assoc db :page/session-id (:session result))]
        (if (contains? (set (result :status)) "done")
          {:db (assoc-in db [:page/blocks block-id :block/processing?] false)}
          {:db (update-in db [:page/blocks block-id :block/results] conj (with-evaled result))})))))

(rf/reg-event-db :clojure/interrupted
  (fn [db [_ {:keys [eval-id]}]]
    (let [block-id (uuid/make-uuid-from eval-id)]
      (assoc-in db [:page/blocks block-id :block/processing?] false))))

(rf/reg-event-fx :clojure/stacktrace
  (fn [{:keys [db]} [_ {:keys [eval-id result]}]]
    (let [block-id (uuid/make-uuid-from eval-id)]
      (if (contains? (set (:status result)) "done")
        {:db (assoc-in db [:page/blocks block-id :block/processing?] false)}
        {:db (update-in db [:page/blocks block-id :block/results] conj result)}))))

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
  (fn [{:keys [eval/code eval/session-id eval/eval-id eval/load-scripts?]}]
    (@chsk-send! [:clojure/eval {:code code
                                 :session-id session-id
                                 :eval-id eval-id
                                 :load-scripts? load-scripts?}])))

(rf/reg-fx :clojure/result
  (fn [{:keys [eval/code eval/session-id eval/eval-id]}]
    (@chsk-send! [:clojure/eval {:code code :session-id session-id :eval-id eval-id}])))

(rf/reg-fx :clojure/interrupt
  (fn [{:keys [interrupt/session-id interrupt/eval-id]}]
    (@chsk-send! [:clojure/interrupt {:session-id session-id :eval-id eval-id}])))

(rf/reg-fx :clojure/stacktrace
  (fn [{:keys [stacktrace/eval-id stacktrace/session-id]}]
    (@chsk-send! [:clojure/stacktrace {:eval-id eval-id :session-id session-id}])))
