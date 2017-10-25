(ns grapple.events
  (:require-macros [grapple.util :refer [spy]])
  (:require [clojure.string :as string]
            [clojure.zip :as zip]
            [re-frame.core :as rf]
            [cljs-uuid-utils.core :as uuid]
            cljsjs.codemirror
            [grapple.nav :as nav]))

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
    (let [blocks [(merge template-markdown-block
                         {:block/id (generate-uuid)
                          :block/content "# My Grapple Notebook"})
                  (merge template-clojure-block
                         {:block/id (generate-uuid)
                          :block/content (str "(ns " generated-ns-name "\n  (:require [grapple.plot :as plot]))")
                          :block/active? true})]]
      {:clojure/init {:init/on-success #(rf/dispatch [:page/session-id %])}
       :db {:page/session-id nil
            :page/flash {:flash/text "" :flash/on false}
            :page/block-order (mapv :block/id blocks)
            :page/blocks (into {} (map (juxt :block/id identity)) blocks)}})))

(rf/reg-event-db
  :page/session-id
  (fn [db [_ session-id]]
    (assoc db :page/session-id session-id)))

(defn key-bindings [id evaluate]
  {:Up #(nav/cursor-up id %)
   :Down #(nav/cursor-down id %)
   :Left #(nav/cursor-left id %)
   :Right #(nav/cursor-right id %)
   :Shift-Enter evaluate
   :Ctrl-Shift-Enter #(rf/dispatch [:blocks/evaluate])
   :Ctrl-C #(rf/dispatch [:block/interrupt id])
   "Ctrl-G Ctrl-A" #(rf/dispatch [:word/completions (nav/current-word %)])
   "Ctrl-G Ctrl-B" #(rf/dispatch [:block/insert-new-before id])
   "Ctrl-G Ctrl-C" #(rf/dispatch [:word/documentation (nav/current-word %)])
   "Ctrl-G Ctrl-D" #(rf/dispatch [:block/move-down id])
   "Ctrl-G Ctrl-E" #(rf/dispatch [:page/save-as])
   "Ctrl-G Ctrl-G" #(rf/dispatch [:block/commands id])
   "Ctrl-G Ctrl-J" #(rf/dispatch [:block/to-clojure id])
   "Ctrl-G Ctrl-L" #(rf/dispatch [:page/load])
   "Ctrl-G Ctrl-M" #(rf/dispatch [:block/to-markdown id])
   "Ctrl-G Ctrl-N" #(rf/dispatch [:block/insert-new-after id])
   "Ctrl-G Ctrl-O" #(rf/dispatch [:block/clear-results id])
   "Ctrl-G Ctrl-S" #(rf/dispatch [:page/save])
   "Ctrl-G Ctrl-U" #(rf/dispatch [:block/move-up id])
   "Ctrl-G Ctrl-W" #(rf/dispatch [:page/save-without-markup])
   "Ctrl-G Ctrl-X" #(rf/dispatch [:block/delete id])
   "Ctrl-G Ctrl-Z" #(rf/dispatch [:blocks/clear-results])
   "Ctrl-G Ctrl-\\" #(rf/dispatch [:block/undo-delete])})

(rf/reg-event-fx
  :codemirror/init
  (fn [{:keys [db]} [_ block node]]
    (let [{block-type :block/type :keys [block/id block/active?]} block
          {:keys [mode evaluate]}
          (condp = block-type
            :block-type/clojure {:mode "clojure"
                                 :evaluate #(rf/dispatch [:block/eval id (.getValue %)])}
            :block-type/markdown {:mode "markdown"
                                  :evaluate #(rf/dispatch [:block/render id (.getValue %)])})
          extra-keys (clj->js (key-bindings id evaluate))]
      (js/CodeMirror.normalizeKeyMap extra-keys)
      {:codemirror/init {:codemirror/id id
                         :codemirror/node node
                         :codemirror/config {:lineNumbers true
                                             :viewportMargin js/Infinity
                                             :matchBrackets true
                                             :autoCloseBrackets true
                                             :mode mode
                                             :theme "neat"
                                             :cursorHeight 0.9
                                             :extraKeys extra-keys}
                         :codemirror/focus? active?
                         :codemirror/on-success #(rf/dispatch [:block/codemirror id %])}})))

(rf/reg-event-db
  :block/codemirror
  (fn [db [_ id cm]]
    (assoc-in db [:page/blocks id :block/codemirror] cm)))

(rf/reg-event-fx
  :block/eval
  [(rf/inject-cofx :generator/uuid)]
  (fn [{:keys [db] generate-uuid :generator/uuid} [_ id content]]
    (when-not (string/blank? content)
      (let [eval-id (uuid/uuid-string uuid)
            db (-> db
                 (update-in [:page/blocks id] merge {:block/eval-id eval-id
                                                     :block/content content})
                 (nav/guarantee-empty-block template-clojure-block (generate-uuid)))
            focus-block (nav/block-after db id)]
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
           (nav/guarantee-empty-block template-clojure-block (generate-uuid)))}))

(rf/reg-event-db
  :block/results
  (fn [db [_ id results]]
    (assoc-in db [:page/blocks id :block/results] results)))

(rf/reg-event-fx
  :block/edit
  (fn [{:keys [db]} [_ id]]
    {:db (update-in db [:page/blocks id] merge {:block/mode :block-mode/edit :block/active? true})}))

(rf/reg-event-fx
  :block/commands
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
    {:codemirror/focus {:focus/codemirror (-> db (nav/block-before id) :block/codemirror)
                        :focus/position pos}}))

(rf/reg-event-fx
  :blocks/focus-next
  (fn [{:keys [db]} [_ id pos]]
    {:codemirror/focus {:focus/codemirror (-> db (nav/block-after id) :block/codemirror)
                        :focus/position pos}}))

(rf/reg-event-fx
  :block/insert-new-before
  [(rf/inject-cofx :generator/uuid)]
  (nav/insert-new-block
    template-clojure-block
    (fn [ids id new-id]
      (-> (zip/vector-zip ids)
        (nav/goto #(= id %))
        (zip/insert-left new-id)
        zip/root
        vec))))

(rf/reg-event-fx
  :block/insert-new-after
  [(rf/inject-cofx :generator/uuid)]
  (nav/insert-new-block
    template-clojure-block
    (fn [ids id new-id]
      (-> (zip/vector-zip ids)
        (nav/goto #(= id %))
        (zip/insert-right new-id)
        zip/root
        vec))))

(rf/reg-event-fx
  :block/delete
  (fn [{:keys [db]} [_ id]]
    (let [move-to-block (or (nav/block-after db id) (nav/block-before db id))]
      {:codemirror/focus {:focus/codemirror (:block/codemirror move-to-block)}
       :db (-> db
             (update :page/block-order #(vec (filter (partial not= id) %)))
             (update :page/blocks dissoc id))})))

(rf/reg-event-db
  :block/move-up
  (fn [db [_ id]]
    (update db :page/block-order
            (fn [ids]
              (-> (zip/vector-zip ids)
                (nav/goto #(= id %))
                nav/move-left
                zip/root
                vec)))))

(rf/reg-event-db
  :block/move-down
  (fn [db [_ id]]
    (update db :page/block-order
            (fn [ids]
              (-> (zip/vector-zip ids)
                (nav/goto #(= id %))
                nav/move-right
                zip/root
                vec)))))

(defn savable-blocks [{:keys [page/block-order page/blocks]}]
  (sequence
    (comp
      (map blocks)
      (map #(dissoc % :block/codemirror)))
    block-order))

(defn update-block-contents [blocks]
  (into {}
        (map (fn [[k {:keys [block/codemirror] :as v}]]
               (if codemirror
                 [k (assoc v :block/content (.getValue codemirror))]
                 [k v])))
        blocks))

(rf/reg-event-fx
  :page/save
  (fn [{:keys [db]} _]
    (let [db (update db :page/blocks update-block-contents)]
      (if-let [filename (db :page/filename)]
        {:page/save {:save/filename filename
                     :save/blocks (savable-blocks db)
                     :save/on-success (fn []
                                        (rf/dispatch [:page/flash "Saved"])
                                        (rf/dispatch [:page/hide-save-modal]))}}
        {:db (assoc db :page/show-save-modal? true)}))))

(rf/reg-event-db
  :page/save-as
  (fn [db _]
    (-> db
      (update :page/blocks update-block-contents)
      (assoc :page/show-save-modal? true))))

(rf/reg-event-fx
  :page/save-to-filename
  (fn [{:keys [db]} [_ filename]]
    (let [db (update db :page/blocks update-block-contents)]
      {:page/save {:save/filename filename
                   :save/blocks (savable-blocks db)
                   :save/on-success (fn []
                                      (rf/dispatch [:page/flash "Saved"])
                                      (rf/dispatch [:page/hide-save-modal]))}
       :db (assoc db :page/filename filename)})))

(rf/reg-event-db
  :page/hide-save-modal
  (fn [db _]
    (assoc db :page/show-save-modal? false)))

(rf/reg-event-fx
  :page/flash
  (fn [{:keys [db]} [_ text]]
    {:action/defer {:defer/message [:page/unflash] :defer/seconds 1}
     :db (assoc db :page/flash {:flash/text text :flash/on true})}))

(rf/reg-event-db
  :page/unflash
  (fn [db _]
    (assoc-in db [:page/flash :flash/on] false)))
