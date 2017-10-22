(ns grapple.views
  (:require-macros [grapple.util :refer [spy]])
  (:require [clojure.string :as string]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [markdown.core :refer [md->html]]
            grapple.render))

(defn codemirror [id block-type _]
  (let [textarea (r/atom nil)]
    (r/create-class
      {:reagent-render
       (fn [id _ code]
         [:div
          [:textarea {:ref #(reset! textarea %)
                      :default-value code
                      :style {:display "none"}}]])
       :component-did-mount
       (fn [this]
         (rf/dispatch [:codemirror/init id block-type @textarea]))})))

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

(defn markdown []
  (let [clickable-links
        (fn [node]
          (doseq [node (array-seq (.querySelectorAll node "a"))]
            (.addEventListener node "click" #(.stopPropagation %))))]
    (r/create-class
      {:reagent-render
       (fn [id content]
         [:div {:dangerouslySetInnerHTML {:__html (md->html content)}
                :on-click #(rf/dispatch [:block/edit id])}])
       :component-did-mount
       (fn [this]
         (clickable-links (r/dom-node this)))
       :component-did-update
       (fn [this]
         (clickable-links (r/dom-node this)))})))

(defmulti block (fn [type _] type))

(defmethod block :block-type/clojure [_ {block-type :block/type :keys [block/id block/content block/results]}]
  [:div.block.block--clojure {:key id}
   ^{:key "code"} [codemirror id block-type content]
   (when results
     ^{:key "results"} [code-results results])])

(defmethod block :block-type/markdown [_ {block-type :block/type :keys [block/id block/content block/mode]}]
  [:div.block.block--markdown {:key id}
   (if (= mode :block-mode/render)
     [markdown id content]
     [codemirror id block-type content])])

(defn notebook []
  [:div
   [:div.blocks
    (for [[id {block-type :block/type :as block-data}] @(rf/subscribe [:page/blocks])
          :let [block-data (assoc block-data :block/id id)]]
      (with-meta
        (block block-type block-data)
        {:key id}))]])
