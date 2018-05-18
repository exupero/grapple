(ns grapple.views
  (:require-macros [grapple.util :refer [spy]])
  (:require [clojure.string :as string]
            [reagent.core :as r]
            [re-frame.core :as rf]
            grapple.render
            [grapple.codemirror :refer [codemirror]]
            [grapple.block :as block]))

(defn classnames [m]
  (transduce
    (comp
      (filter val)
      (map key)
      (map name)
      (interpose " "))
    str m))

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
    (let [{:keys [flash/text flash/on?]} @(rf/subscribe [:page/flash])]
      [:div.flash
       {:key "flash"
        :className (classnames {:flash--on on?})}
       [:div.flash__text text]])
    (when @(rf/subscribe [:page/show-save-modal?])
      ^{:key "save-modal"} [save-modal])
    (when @(rf/subscribe [:page/show-load-modal?])
      ^{:key "load-modal"} [load-modal])]
   [:div.blocks {:key "blocks"}
    (for [{:keys [block/id block/abbr block/active?]
           :as b} @(rf/subscribe [:page/blocks])]
      [:div.block
       {:key id
        :className (classnames {:block--active active?})
        :data-abbr abbr}
       (block/block b)])]])
