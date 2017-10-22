(ns grapple.views
  (:require-macros [grapple.util :refer [spy]])
  (:require [clojure.string :as string]
            [reagent.core :as r]
            [re-frame.core :as rf]
            grapple.render))

(defn codemirror [id _]
  (r/create-class
    {:reagent-render
     (fn [id code _]
       [:textarea {:default-value code
                   :style {:display "none"}}])
     :component-did-mount
     (fn [this]
       (rf/dispatch [:codemirror/init id (r/dom-node this)]))}))

(defn highlight-block [node]
  (let [blocks (array-seq (.querySelectorAll node "pre code"))]
    (doseq [block blocks]
      (js/hljs.highlightBlock block))))

(defn code-result [{:keys [value evaled out err]}]
  (cond
    evaled evaled
    err (grapple.render/->Error err)
    out (grapple.render/->Print out)
    (and (string? value) (string/starts-with? value "#'")) (grapple.render/->VarName value)))

(defn code-results [results]
  (r/create-class
    {:reagent-render
     (fn [results]
       [:div.code-result
        (for [[i {:keys [status] :as result}] (map-indexed vector results)
              :while (not (= status ["done"]))]
          (with-meta
            (grapple.render/render (code-result result))
            {:key i}))])
     :component-did-mount
     (fn [this]
       (highlight-block (r/dom-node this)))
     :component-did-update
     (fn [this]
       (highlight-block (r/dom-node this)))}))

(defn notebook []
  [:div
   [:div.blocks
    (for [[id {:keys [block/code block/results]}] @(rf/subscribe [:page/blocks])]
      [:div.block {:key id}
       [codemirror id code]
       [code-results results]])]])
