(ns grapple.effects
  (:require-macros [grapple.util :refer [spy]])
  (:require [cognitect.transit :as transit]
            [re-frame.core :as rf]
            [ajax.core :as http]
            cljsjs.codemirror
            cljsjs.codemirror.mode.clojure
            cljsjs.codemirror.mode.markdown
            cljsjs.codemirror.addon.edit.closebrackets
            cljsjs.codemirror.addon.edit.matchbrackets
            [grapple.evaluate :refer [write-handlers]]))

(rf/reg-fx
  :clojure/init
  (fn [{:keys [init/on-success]}]
    (http/POST "/api/init"
               {:headers {"X-CSRF-Token" js/antiForgeryToken}
                :response-format :transit
                :handler (fn [session-id]
                           (on-success session-id))})))

(rf/reg-fx
  :clojure/eval
  (fn [{:keys [eval/code eval/session-id eval/eval-id eval/on-success]}]
    (http/POST "/api/eval"
               {:params {:code code :session-id session-id :eval-id eval-id}
                :headers {"X-CSRF-Token" js/antiForgeryToken}
                :response-format :transit
                :handler on-success})))

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
    (http/POST "/api/save"
               {:params {:filename filename :blocks blocks}
                :headers {"X-CSRF-Token" js/antiForgeryToken}
                :writer (transit/writer :json {:handlers write-handlers})
                :handler on-success})))

(rf/reg-fx
  :page/load
  (fn [{:keys [load/filename load/on-success]}]
    (http/POST "/api/load"
               {:params {:filename filename}
                :headers {"X-CSRF-Token" js/antiForgeryToken}
                :response-format :transit
                :handler on-success})))

(rf/reg-fx
  :action/defer
  (fn [{:keys [defer/message defer/seconds]}]
    (js/setTimeout
      #(rf/dispatch message)
      (* seconds 1000))))
