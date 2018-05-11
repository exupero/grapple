(ns grapple.effects
  (:require-macros [grapple.util :refer [spy]])
  (:require [re-frame.core :as rf]
            [ajax.core :as ajax]
            [grapple.serialization :refer [with-evaled]]))

(rf/reg-fx :mathjax/init
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

(rf/reg-fx :scripts/load
  (fn [{:keys [load/scripts load/on-success load/on-error]}]
    (add-scripts! scripts on-success on-error)))

(rf/reg-fx :page/save
  (fn [{:keys [save/filename save/blocks save/on-success]}]
    (ajax/POST "/saved"
               {:params {:filename filename :blocks blocks}
                :handler on-success})))

(rf/reg-fx :page/load
  (fn [{:keys [load/filename load/on-success]}]
    (ajax/GET "/saved"
              {:params {:filename filename}
               :handler (fn [blocks]
                          (on-success
                            (map (fn [{:keys [block/id block/results] :as block}]
                                   (update block :block/results #(map with-evaled %)))
                                 blocks)))})))

(rf/reg-fx :action/execute
  (fn [f]
    (f)))

(rf/reg-fx :action/defer
  (fn [{:keys [defer/message defer/seconds]}]
    (js/setTimeout
      #(rf/dispatch message)
      (* seconds 1000))))
