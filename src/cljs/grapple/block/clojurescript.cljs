(ns grapple.block.clojurescript
  (:require [re-frame.core :as rf]
            [cljs.tools.reader :refer [read-string]]
            [cljs.js :refer [empty-state eval js-eval]]
            [grapple.block :as b]
            [grapple.codemirror :as cm]))

(def block
  {:block/type :block-type/clojurescript
   :block/abbr "cljs"
   :block/codemirror-mode "clojure"
   :block/eval-event :clojurescript/eval
   :block/content ""
   :block/active? false})

(defmethod b/convert :block-type/clojurescript [b _]
  (merge b (dissoc block :block/content :block/active?)))

(defn eval-str [id code]
  (eval (empty-state)
        (read-string code)
        {:eval js-eval
         :source-map true
         :context :expr}
        (fn [result]
          (rf/dispatch [:clojurescript/result id result]))))

;; Components

(defmethod b/render :block-type/clojurescript [{:keys [block/results] :as b}]
  [:div
   [cm/codemirror b]
   (when results [:div results])])

;; Events

(rf/reg-event-fx :clojurescript/eval
  (fn [{:keys [db]} [_ id content]]
    {:db (update-in db [:page/blocks id] merge
                    {:block/content content})
     :clojurescript/eval {:eval/id id
                          :eval/code content}}))

(rf/reg-event-db :clojurescript/result
  (fn [db [_ id result]]
    (assoc-in db [:page/blocks id :block/results] result)))

;; Effects

(rf/reg-fx :clojurescript/eval
  (fn [{:keys [eval/id eval/code]}]
    (eval-str id code)))
