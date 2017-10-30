(ns grapple.views
  (:require-macros [grapple.util :refer [spy]])
  (:require [clojure.string :as string]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [markdown.core :refer [md->html]]
            markdown.transformers
            grapple.render))

(defn codemirror [block]
  (let [textarea (r/atom nil)]
    (r/create-class
      {:reagent-render
       (fn [{:keys [block/content block/processing?]}]
         [:div
          [:textarea {:ref #(reset! textarea %)
                      :default-value content
                      :style {:display "none"}}]])
       :component-did-mount
       (fn [this]
         (rf/dispatch [:codemirror/init block @textarea]))})))

(defn code-result [{:keys [value result/evaled out class message stacktrace] :as result}]
  (cond
    stacktrace (grapple.render/->Stacktrace class message stacktrace)
    evaled evaled
    out (grapple.render/->Print out)
    (and (string? value) (string/starts-with? value "#'")) (grapple.render/->VarName value)))

(defn code-results [results]
  [:div.block-results
   (when-let [output (seq (filter #(contains? % :out) results))]
     ^{:key "output"}
     [:div.block-results__output
      (for [[i result] (map-indexed vector output)]
        (with-meta
          (grapple.render/render (code-result result))
          {:key i}))])
   (when-let [values (seq (filter #(contains? % :value) results))]
     ^{:key "values"}
     [:div.block-results__values
      (for [[i result] (map-indexed vector values)]
        ^{:key i}
        [:div (grapple.render/render (code-result result))])])
   (when-let [error (seq (filter #(contains? % :stacktrace) results))]
     ^{:key "stacktrace"}
     [:div.block-results__error
      (for [[i result] (map-indexed vector error)]
        ^{:key i}
        [:div (grapple.render/render (code-result result))])])])

(def markdown-transformers
  (remove #(= markdown.transformers/superscript %)
          markdown.transformers/transformer-vector))

(defn markdown [id content]
  (let [rerender
        (fn [node]
          (js/MathJax.Hub.Queue #js ["Typeset" js/MathJax.Hub node])
          (doseq [node (array-seq (.querySelectorAll node "a"))]
            (.addEventListener node "click" #(.stopPropagation %))))]
    (r/create-class
      {:reagent-render
       (fn [id content]
         (let [html (md->html content :replacement-transformers markdown-transformers)]
           [:div {:dangerouslySetInnerHTML {:__html html}
                  :on-click #(rf/dispatch [:block/edit id])}]))
       :component-did-mount
       (fn [this]
         (rerender (r/dom-node this)))
       :component-did-update
       (fn [this]
         (rerender (r/dom-node this)))})))

(defmulti block :block/type)

(defmethod block :block-type/markdown [{:keys [block/id block/content block/active?] :as block}]
  [:div.block.block--markdown
   {:key id
    :className (when active? "block--active")}
   (if active?
     [codemirror block]
     [markdown id content])])

(defmethod block :block-type/clojure [{:keys [block/id block/content block/results block/active? block/processing?] :as block}]
  [:div.block.block--clojure
   {:key id
    :class (string/join " " [(when active? "block--active") (when processing? "block--processing")])}
   ^{:key "code"} [codemirror block]
   (when results
     ^{:key "results"} [code-results results])])

(defn modal [& body]
  [:div.modal
   (for [[i child] (map-indexed vector body)]
     (with-meta child {:key i}))])

(defn save-modal []
  (let [input (r/atom nil)
        submit (fn [value]
                 (rf/dispatch [:page/save-to-filename value]))]
    (r/create-class
      {:reagent-render
       (fn []
         [modal
          [:input
           {:ref #(reset! input %)
            :type "text"
            :on-key-up #(when (= "Enter" (.-key %))
                          (submit (.. % -target -value)))}]
          [:button.modal__button
           {:on-click #(submit (.-value @input))}
           "Save Notebook"]])
       :component-did-mount
       (fn [this]
         (.focus @input))})))

(defn load-modal []
  (let [input (r/atom nil)
        submit (fn [value]
                 (rf/dispatch [:page/load-from-filename value]))]
    (r/create-class
      {:reagent-render
       (fn []
         [modal
          [:input
           {:ref #(reset! input %)
            :type "text"
            :on-key-up #(when (= "Enter" (.-key %))
                          (submit (.. % -target -value)))}]
          [:button.modal__button
           {:on-click #(submit (.-value @input))}
           "Load Notebook"]])
       :component-did-mount
       (fn [this]
         (.focus @input))})))

(defn page []
  [:div
   [:div {:key "modals-and-flash"}
    (let [{:keys [flash/text flash/on]} @(rf/subscribe [:page/flash])]
      [:div.flash
       {:key "flash"
        :class [(when on "flash--on")]}
       text])
    (when @(rf/subscribe [:page/show-save-modal?])
      ^{:key "save-modal"} [save-modal])
    (when @(rf/subscribe [:page/show-load-modal?])
      ^{:key "load-modal"} [load-modal])]
   [:div.blocks {:key "blocks"}
    (for [{:keys [block/id] :as block-data} @(rf/subscribe [:page/blocks])]
      (with-meta
        (block block-data)
        {:key id}))]])
