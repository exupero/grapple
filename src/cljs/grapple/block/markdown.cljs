(ns grapple.block.markdown
  (:require-macros [grapple.util :refer [spy]])
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [markdown.core :refer [md->html]]
            markdown.transformers
            [grapple.block :as b]
            [grapple.codemirror :as cm]))

(def markdown-transformers
  (remove #{markdown.transformers/superscript} markdown.transformers/transformer-vector))

(def block
  {:block/type :block-type/markdown
   :block/abbr "md"
   :block/codemirror-mode "markdown"
   :block/eval-event :markdown/render
   :block/content ""
   :block/active? false})

(defmethod b/convert :block-type/markdown [b _]
  (-> b
    (merge (dissoc block :block/content :block/active?))
    (dissoc :block/results)))

;; Components

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

(defmethod b/render :block-type/markdown [{:keys [block/id block/content block/active?] :as b}]
  (if active?
    [cm/codemirror b]
    [markdown id content]))

;; Events

(rf/reg-event-fx :markdown/render
  (fn [{:keys [db]} [_ id content]]
    {:db (assoc-in db [:page/blocks id :block/content] content)}))
