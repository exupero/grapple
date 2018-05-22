(ns grapple.effects
  (:require-macros [grapple.util :refer [spy]])
  (:require [re-frame.core :as rf]))

(rf/reg-fx :mathjax/init
  (fn [_]
    (js/MathJax.Hub.Config
      (clj->js {:messageStyle "none"
                :showProcessingMessages false
                :skipStartupTypeset true
                :tex2jax {:inlineMath [["@@" "@@"]]}}))
    (js/MathJax.Hub.Configured)))

(rf/reg-fx :action/execute
  (fn [f]
    (f)))

(rf/reg-fx :action/defer
  (fn [{:keys [defer/message defer/seconds]}]
    (js/setTimeout
      #(rf/dispatch message)
      (* seconds 1000))))
