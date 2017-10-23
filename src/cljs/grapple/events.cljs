(ns grapple.events
  (:require-macros [grapple.util :refer [spy]])
  (:require [clojure.string :as string]
            [re-frame.core :as rf]
            [cljs-uuid-utils.core :as uuid]
            cljsjs.codemirror))

(def template-markdown-block
  {:block/type :block-type/markdown
   :block/content ""
   :block/mode :block-mode/render
   :block/active? false})

(def template-clojure-block
  {:block/type :block-type/clojure
   :block/content ""
   :block/active? false})

(rf/reg-event-fx
  :page/init
  [(rf/inject-cofx :generate/ns-name)
   (rf/inject-cofx :generator/uuid)]
  (fn [{generated-ns-name :generated/ns-name generate-uuid :generator/uuid} _]
    {:clojure/init {:init/on-success #(rf/dispatch [:page/session-id %])}
     :db {:page/session-id nil
          :page/blocks
          (into {} (map (fn [x]
                          (let [id (generate-uuid)]
                            [id (assoc x :block/id id)])))
                [(merge template-markdown-block
                        {:block/order 0
                         :block/content "# My Grapple Notebook"})
                 (merge template-clojure-block
                        {:block/order 1
                         :block/content (str "(ns " generated-ns-name "\n  (:require [grapple.plot :as plot]))")
                         :block/active? true})])}}))

(rf/reg-event-db
  :page/session-id
  (fn [db [_ session-id]]
    (assoc db :page/session-id session-id)))

(defn cursor-up [id cm]
  (let [cursor (.getCursor cm)]
    (if (zero? (.-line cursor))
      (rf/dispatch [:blocks/focus-previous id :line/default])
      js/CodeMirror.Pass)))

(defn cursor-down [id cm]
  (let [cursor (.getCursor cm)
        last-line (dec (.lineCount cm))]
    (if (= last-line (.-line cursor))
      (rf/dispatch [:blocks/focus-next id :line/default])
      js/CodeMirror.Pass)))

(defn cursor-left [id cm]
  (let [cursor (.getCursor cm)]
    (if (and (zero? (.-line cursor)) (zero? (.-ch cursor)))
      (rf/dispatch [:blocks/focus-previous id :line/end])
      js/CodeMirror.Pass)))

(defn cursor-right [id cm]
  (let [cursor (.getCursor cm)
        last-line (dec (.lineCount cm))
        last-ch (.-length (.getLine cm last-line))]
    (if (and (= last-line (.-line cursor))
             (= last-ch (.-ch cursor)))
      (rf/dispatch [:blocks/focus-next id :line/start])
      js/CodeMirror.Pass)))

(rf/reg-event-fx
  :codemirror/init
  (fn [{:keys [db]} [_ block node]]
    (let [{block-type :block/type :keys [block/id block/active?]} block
          block-specific (condp = block-type
                           :block-type/clojure {:mode "clojure"
                                                :evaluate #(rf/dispatch [:block/eval id (.getValue %)])}
                           :block-type/markdown {:mode "markdown"
                                                 :evaluate #(rf/dispatch [:block/render id (.getValue %)])})]
      {:codemirror/init {:codemirror/id id
                         :codemirror/node node
                         :codemirror/config {:lineNumbers true
                                             :viewportMargin js/Infinity
                                             :matchBrackets true
                                             :autoCloseBrackets true
                                             :mode (block-specific :mode)
                                             :theme "neat"
                                             :cursorHeight 0.9
                                             :extraKeys {:Shift-Enter (block-specific :evaluate)
                                                         :Ctrl-G #(rf/dispatch [:block/commands id])
                                                         :Up #(cursor-up id %)
                                                         :Down #(cursor-down id %)
                                                         :Left #(cursor-left id %)
                                                         :Right #(cursor-right id %)}}
                         :codemirror/focus? active?
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
            new-block (assoc template-clojure-block :block/order last-pos :block/id new-uuid)]
        (assoc blocks new-uuid new-block)))))

(defn block-before [id blocks]
  (->> (vals blocks)
    (sort-by :block/order)
    (partition 2 1)
    (filter #(-> % second :block/id (= id)))
    first first :block/id (get blocks)))

(defn block-after [id blocks]
  (->> (vals blocks)
    (sort-by :block/order)
    (partition 2 1)
    (filter #(-> % first :block/id (= id)))
    first second :block/id (get blocks)))

(rf/reg-event-fx
  :block/eval
  [(rf/inject-cofx :generator/uuid)]
  (fn [{:keys [db] generate-uuid :generator/uuid} [_ id content]]
    (when-not (string/blank? content)
      (let [eval-id (uuid/uuid-string uuid)
            db (-> db
                 (update-in [:page/blocks id] merge {:block/eval-id eval-id
                                                     :block/content content})
                 (update :page/blocks guarantee-empty-block (generate-uuid)))
            focus-block (block-after id (db :page/blocks))]
        {:db (assoc-in db [:page/blocks (:block/id focus-block) :block/active?] true)
         :codemirror/focus (:block/codemirror focus-block)
         :clojure/eval {:eval/code content
                        :eval/session-id (db :page/session-id)
                        :eval/eval-id (uuid/uuid-string uuid)
                        :eval/on-success
                        (fn [forms]
                          (rf/dispatch [:page/session-id (-> forms first :session)])
                          (rf/dispatch [:block/results id forms]))}}))))

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
    (assoc-in db [:page/blocks id :block/results] results)))

(rf/reg-event-fx
  :block/edit
  (fn [{:keys [db]} [_ id]]
    {:codemirror/focus (get-in db [:page/blocks id :block/codemirror])
     :db (assoc-in db [:page/blocks id :block/mode] :block-mode/edit)}))

(rf/reg-event-fx
  :block/command
  (fn [{:keys [db]} [_ id]]
    {:db (assoc db :page/show-modal? true :block/selected id)}))

(rf/reg-event-db
  :blocks/activate
  (fn [db [_ id]]
    (-> db
      (update :page/blocks
              (fn [blocks]
                (into {}
                      (map (fn [[k v]]
                             [k (assoc v :block/active? false)]))
                      blocks)))
      (assoc-in [:page/blocks id :block/active?] true))))

(rf/reg-event-fx
  :blocks/focus-previous
  (fn [{:keys [db]} [_ id pos]]
    {:codemirror/focus {:focus/codemirror (->> db :page/blocks (block-before id) :block/codemirror)
                        :focus/position pos}}))

(rf/reg-event-fx
  :blocks/focus-next
  (fn [{:keys [db]} [_ id pos]]
    {:codemirror/focus {:focus/codemirror (->> db :page/blocks (block-after id) :block/codemirror)
                        :focus/position pos}}))
