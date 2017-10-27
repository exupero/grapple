(ns grapple.effects
  (:require-macros [grapple.util :refer [spy]])
  (:require [re-frame.core :as rf]
            [ajax.core :as http]
            [taoensso.sente :as sente]
            [taoensso.sente.packers.transit :as sente-transit]
            cljsjs.codemirror
            cljsjs.codemirror.mode.clojure
            cljsjs.codemirror.mode.markdown
            cljsjs.codemirror.addon.edit.closebrackets
            cljsjs.codemirror.addon.edit.matchbrackets
            [grapple.evaluate :refer [write-handlers]]))

(def packer (sente-transit/->TransitPacker :json {:handlers write-handlers} {}))
(defonce channel-socket (sente/make-channel-socket! "/ws" {:type :auto :packer packer}))
(defonce chsk       (channel-socket :chsk))
(defonce ch-chsk    (channel-socket :ch-recv))
(defonce chsk-send! (channel-socket :send-fn))
(defonce chsk-state (channel-socket :state))

(defmulti event-msg-handler :id)

(defmethod event-msg-handler :chsk/handshake [{:keys [?data]}]
  (rf/dispatch [:clojure/init]))

(defmethod event-msg-handler :chsk/recv [{:keys [id ?data]}]
  (let [[event data] ?data]
    (condp = event
      :eval/result (rf/dispatch [:eval/result data]))))

(defmethod event-msg-handler :chsk/state [_])

(rf/reg-fx
  :ws/init
  (fn [_]
    (sente/start-client-chsk-router! ch-chsk event-msg-handler)))

(rf/reg-fx
  :mathjax/init
  (fn [_]
    (js/MathJax.Hub.Config
      (clj->js {:messageStyle "none"
                :showProcessingMessages false
                :skipStartupTypeset true
                :tex2jax {:inlineMath [["@@" "@@"]]}}))
    (js/MathJax.Hub.Configured)))

(rf/reg-fx
  :clojure/init
  (fn [{:keys [init/on-success]}]
    (chsk-send! [:clojure/init] js/Number.MAX_SAFE_INTEGER on-success)))

(rf/reg-fx
  :clojure/eval
  (fn [{:keys [eval/code eval/session-id eval/eval-id]}]
    (chsk-send! [:clojure/eval {:code code :session-id session-id :eval-id eval-id}])))

(rf/reg-fx
  :codemirror/init
  (fn [{:keys [codemirror/id codemirror/node codemirror/config
               codemirror/focus? codemirror/on-success]}]
    (let [cm (js/CodeMirror.fromTextArea node (clj->js config))]
      (.on cm "focus" #(rf/dispatch [:blocks/activate id]))
      (when focus?
        (.focus cm)
        (.setCursor cm (.lineCount cm) 0))
      (on-success cm))))

(rf/reg-fx
  :codemirror/focus
  (fn [{cm :focus/codemirror :keys [focus/position]}]
    (when cm
      (.focus cm)
      (condp = position
        :line/start (.execCommand cm "goDocStart")
        :line/end (.execCommand cm "goDocEnd")
        nil))))

(rf/reg-fx
  :page/save
  (fn [{:keys [save/filename save/blocks save/on-success]}]
    (chsk-send! [:page/save {:filename filename :blocks blocks}]
                (* 5 1000) on-success)))

(rf/reg-fx
  :page/load
  (fn [{:keys [load/filename load/on-success]}]
    (chsk-send! [:page/load {:filename filename}] (* 5 1000) on-success)))

(rf/reg-fx
  :action/defer
  (fn [{:keys [defer/message defer/seconds]}]
    (js/setTimeout
      #(rf/dispatch message)
      (* seconds 1000))))
