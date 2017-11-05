(ns grapple.effects
  (:require-macros [grapple.util :refer [spy]])
  (:require [clojure.string :as string]
            [cljs.reader :as edn]
            [re-frame.core :as rf]
            [ajax.core :as http]
            [taoensso.sente :as sente]
            [taoensso.sente.packers.transit :as sente-transit]
            cljsjs.codemirror
            cljsjs.codemirror.mode.clojure
            cljsjs.codemirror.mode.markdown
            cljsjs.codemirror.addon.edit.closebrackets
            cljsjs.codemirror.addon.edit.matchbrackets))

(defonce chsk-send! (atom nil))
(defonce tag-readers (atom nil))

(defn with-evaled [{:keys [value] :as result}]
  (cond
    (nil? value) result
    (string/starts-with? value "#'") result
    :else (assoc result :result/evaled (edn/read-string {:readers @tag-readers} value))))

(defmulti event first)

(defmethod event :default [data]
  (rf/dispatch data))

(defmethod event :eval/result [[ev result]]
  (rf/dispatch [ev (update result :result with-evaled)]))

(defmulti event-msg-handler :id)

(defmethod event-msg-handler :chsk/handshake [{:keys [?data]}]
  (rf/dispatch [:clojure/init]))

(defmethod event-msg-handler :chsk/recv [{:keys [id ?data]}]
  (event ?data))

(defmethod event-msg-handler :chsk/state [_])

(rf/reg-fx
  :ws/init
  (fn [{:keys [ws-init/write-handlers ws-init/read-handlers]}]
    (reset! tag-readers read-handlers)
    (let [packer (sente-transit/->TransitPacker :json {:handlers write-handlers} {})
          {:keys [ch-recv send-fn]} (sente/make-channel-socket! "/ws" {:type :auto :packer packer})]
      (reset! chsk-send! send-fn)
      (sente/start-client-chsk-router! ch-recv event-msg-handler))))

(rf/reg-fx
  :mathjax/init
  (fn [_]
    (js/MathJax.Hub.Config
      (clj->js {:messageStyle "none"
                :showProcessingMessages false
                :skipStartupTypeset true
                :tex2jax {:inlineMath [["@@" "@@"]]}}))
    (js/MathJax.Hub.Configured)))

(defn add-script! [script on-success on-error]
  (let [node (doto (js/document.createElement "script")
               (.setAttribute "type" "text/javascript")
               (.setAttribute "charset" "utf8")
               (.setAttribute "async" true)
               (.setAttribute "src" script))]
    (set! (.-onload node)
          (fn []
            (this-as this
              (set! (.-onload this) nil)
              (set! (.-onerror this) nil)
              (on-success this))))
    (set! (.-onerror node)
          (fn []
            (this-as this
              (set! (.-onload this) nil)
              (set! (.-onerror this) nil)
              (on-error this))))
    (.appendChild js/document.head node)))

(defn add-scripts! [scripts on-success on-error]
  (if (seq scripts)
    (add-script!
      (first scripts)
      #(add-scripts! (rest scripts) on-success on-error)
      on-error)
    (on-success)))

(rf/reg-fx
  :scripts/load
  (fn [{:keys [load/scripts load/on-success load/on-error]}]
    (add-scripts! scripts on-success on-error)))

(rf/reg-fx
  :clojure/init
  (fn [{:keys [init/on-success]}]
    (@chsk-send! [:clojure/init] js/Number.MAX_SAFE_INTEGER on-success)))

(rf/reg-fx
  :clojure/eval
  (fn [{:keys [eval/code eval/session-id eval/eval-id]}]
    (@chsk-send! [:clojure/eval {:code code :session-id session-id :eval-id eval-id}])))

(rf/reg-fx
  :clojure/interrupt
  (fn [{:keys [interrupt/session-id interrupt/eval-id]}]
    (@chsk-send! [:clojure/interrupt {:session-id session-id :eval-id eval-id}])))

(rf/reg-fx
  :clojure/stacktrace
  (fn [{:keys [stacktrace/eval-id stacktrace/session-id]}]
    (@chsk-send! [:clojure/stacktrace {:eval-id eval-id :session-id session-id}])))

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
    (@chsk-send! [:page/save {:filename filename :blocks blocks}]
                (* 5 1000) on-success)))

(rf/reg-fx
  :page/load
  (fn [{:keys [load/filename load/on-success]}]
    (@chsk-send!
       [:page/load {:filename filename}]
       (* 5 1000)
       (fn [blocks]
         (on-success
           (map (fn [{:keys [block/id block/results] :as block}]
                  (update block :block/results #(map with-evaled %)))
                blocks))))))

(rf/reg-fx
  :action/execute
  (fn [f]
    (f)))

(rf/reg-fx
  :action/defer
  (fn [{:keys [defer/message defer/seconds]}]
    (js/setTimeout
      #(rf/dispatch message)
      (* seconds 1000))))
