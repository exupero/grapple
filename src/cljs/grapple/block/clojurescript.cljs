(ns grapple.block.clojurescript
  (:require-macros [grapple.util :refer [spy]])
  (:require [re-frame.core :as rf]
            [cljs.tools.reader :refer [read-string]]
            [cljs.js :refer [empty-state eval js-eval]]
            [grapple.block :as b]
            [grapple.codemirror :as cm]
            [grapple.results :as r]
            [grapple.serialization :refer [with-evaled]]))

(def block
  {:block/type :block-type/clojurescript
   :block/abbr "cljs"
   :block/codemirror-mode "clojure"
   :block/eval-event :clojurescript/eval
   :block/content ""
   :block/active? false})

(defmethod b/create :block-type/clojurescript []
  block)

(defmethod b/convert :block-type/clojurescript [b _]
  (merge b (dissoc block :block/content :block/active?)))

(defonce state (empty-state))

(defn eval-str [id code]
  (eval state
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
   (when results [r/results results])])

;; Events

(rf/reg-event-fx :clojurescript/eval
  (fn [{:keys [db]} [_ id content]]
    {:db (update-in db [:page/blocks id] merge
                    {:block/content content
                     :block/results []})
     :clojurescript/eval {:eval/id id
                          :eval/code content}}))

(rf/reg-event-db :clojurescript/result
  (fn [db [_ id result]]
    (update-in db [:page/blocks id :block/results] conj (with-evaled result))))

;; Effects

(rf/reg-fx :clojurescript/eval
  (fn [{:keys [eval/id eval/code]}]
    (eval-str id code)))
