(ns grapple.events
  (:require-macros [grapple.util :refer [spy]])
  (:require [re-frame.core :as rf]
            [cljs-uuid-utils.core :as uuid]))

(def template-markdown-block
  {:block/type :block-type/markdown
   :block/content ""
   :block/mode :block-mode/render})

(def template-clojure-block
  {:block/type :block-type/clojure
   :block/content ""})

(rf/reg-event-fx
  :page/init
  [(rf/inject-cofx :generate/ns-name)
   (rf/inject-cofx :generator/uuid)]
  (fn [{generated-ns-name :generated/ns-name generate-uuid :generator/uuid} _]
    {:clojure/init {:init/on-success #(rf/dispatch [:page/session-id %])}
     :db {:page/session-id nil
          :page/blocks
          (into {} (map (fn [x] [(generate-uuid) x]))
                [(merge template-markdown-block
                        {:block/order 0
                         :block/content "# My Grapple Notebook"})
                 (merge template-clojure-block
                        {:block/order 1
                         :block/content (str "(ns " generated-ns-name "\n  (:require [grapple.plot :as plot]))")})])}}))

(rf/reg-event-db
  :page/session-id
  (fn [db [_ session-id]]
    (assoc db :page/session-id session-id)))

(rf/reg-event-fx
  :codemirror/init
  (fn [{:keys [db]} [_ id block-type node]]
    (let [block-specific (condp = block-type
                           :block-type/clojure {:mode "clojure"
                                                :evaluate #(rf/dispatch [:block/eval id (.getValue %)])}
                           :block-type/markdown {:mode "markdown"
                                                 :evaluate #(rf/dispatch [:block/render id (.getValue %)])})]
      {:db (assoc db :page/focus nil)
       :codemirror/init {:codemirror/node node
                         :codemirror/config {:lineNumbers true
                                             :viewportMargin js/Infinity
                                             :matchBrackets true
                                             :autoCloseBrackets true
                                             :mode (block-specific :mode)
                                             :theme "neat"
                                             :cursorHeight 0.9
                                             :extraKeys {"Shift-Enter" (block-specific :evaluate)}}
                         :codemirror/focus? (= id (:page/focus db))
                         :codemirror/on-success #(rf/dispatch [:block/codemirror id %])}})))

(rf/reg-event-db
  :block/codemirror
  (fn [db [_ id cm]]
    (assoc-in db [:page/blocks id :block/codemirror] cm)))

(defn guarantee-empty-block [blocks new-uuid]
  (let [end (apply max-key :block/order (vals blocks))]
    (if (= "" (:block/content end))
      blocks
      (let [last-pos (inc (end :block/order))
            new-block (assoc template-clojure-block :block/order last-pos)]
        (assoc blocks new-uuid new-block)))))

(rf/reg-event-fx
  :block/eval
  [(rf/inject-cofx :generator/uuid)]
  (fn [{:keys [db] generate-uuid :generator/uuid} [_ id content]]
    {:clojure/eval {:eval/code content
                    :eval/session-id (db :page/session-id)
                    :eval/eval-id (uuid/uuid-string uuid)
                    :eval/on-success
                    (fn [forms]
                      (rf/dispatch [:page/session-id (-> forms first :session)])
                      (rf/dispatch [:block/results id forms]))}
     :db (-> db
           (assoc-in [:page/blocks id :block/content] content)
           (update :page/blocks guarantee-empty-block (generate-uuid)))}))

(rf/reg-event-fx
  :block/render
  [(rf/inject-cofx :generator/uuid)]
  (fn [{:keys [db] generate-uuid :generator/uuid} [_ id content]]
    {:db (-> db
           (update-in [:page/blocks id] merge {:block/content content
                                               :block/mode :block-mode/render})
           (update :page/blocks guarantee-empty-block (generate-uuid)))}))

(rf/reg-event-db
  :block/results
  (fn [db [_ id results]]
    (-> db
      (assoc-in [:page/blocks id :block/results] results)
      (assoc-in [:page/blocks id :block/eval-id] (-> results first :id)))))

(rf/reg-event-db
  :block/edit
  (fn [db [_ id]]
    (-> db
      (assoc-in [:page/blocks id :block/mode] :block-mode/edit)
      (assoc :page/focus id))))
