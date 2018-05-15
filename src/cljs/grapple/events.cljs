(ns grapple.events
  (:require-macros [grapple.util :refer [spy]])
  (:require [clojure.set :refer [difference union]]
            [clojure.string :as string]
            [clojure.zip :as zip]
            [re-frame.core :as rf]
            cljsjs.codemirror
            [grapple.block :as block]
            [grapple.block.markdown :as md]
            [grapple.block.clojure :as clj]
            [grapple.block.clojurescript :as cljs]))

(defn savable-blocks [{:keys [page/block-order page/blocks]}]
  (sequence
    (comp
      (map blocks)
      (map #(dissoc % :block/codemirror))
      (map (fn [x]
             (update x :block/results
                     (fn [results]
                       (map #(dissoc % :result/evaled) results))))))
    block-order))

(defn update-block-contents [blocks]
  (into {}
        (map (fn [[k {:keys [block/codemirror] :as v}]]
               (if codemirror
                 [k (assoc v :block/content (.getValue codemirror))]
                 [k v])))
        blocks))

(rf/reg-event-fx :scripts/load
  (fn [{:keys [db]} [_ scripts]]
    (let [to-load (difference (set scripts) (db :scripts/loaded))]
      (when (seq to-load)
        {:scripts/load {:load/scripts to-load
                        :load/on-success #(rf/dispatch [:scripts/loaded to-load])
                        :load/on-error #(js/console.error "Failed to load script" %)}}))))

(rf/reg-event-fx :scripts/loaded
  (fn [{:keys [db]} [_ scripts]]
    {:db (update db :scripts/loaded union (set scripts))}))

(rf/reg-event-fx :page/init
  [(rf/inject-cofx :generate/ns-name)
   (rf/inject-cofx :generator/uuid)]
  (fn [{generated-ns-name :generated/ns-name generate-uuid :generator/uuid}
       [_ {:keys [init/tag-readers init/tag-writers] :as arg}]]
    (let [blocks [(merge md/block
                         {:block/id (generate-uuid)
                          :block/content "# My Grapple Notebook"})
                  (merge clj/block
                         {:block/id (generate-uuid)
                          :block/content (str "(ns " generated-ns-name "\n  (:require [grapple.plot :as plot]))")
                          :block/active? true
                          :block/permanent? true
                          :block/load-scripts? true})]]
      {:tags/init {:tags/read-handlers tag-readers}
       :clojure/init {:ws/write-handlers tag-writers}
       :mathjax/init true
       :db {:page/session-id nil
            :page/flash {:flash/text "" :flash/on? false}
            :page/block-order (mapv :block/id blocks)
            :page/blocks (into {} (map (juxt :block/id identity)) blocks)
            :scripts/loaded #{}}})))

(rf/reg-event-db :page/session-id
  (fn [db [_ session-id]]
    (assoc db :page/session-id session-id)))

(rf/reg-event-fx :page/save
  (fn [{:keys [db]} _]
    (let [db (update db :page/blocks update-block-contents)]
      (if-let [filename (db :page/filename)]
        {:page/save {:save/filename filename
                     :save/blocks (savable-blocks db)
                     :save/on-success (fn []
                                        (rf/dispatch [:page/flash (str "Saved " filename)])
                                        (rf/dispatch [:page/hide-save-modal]))}}
        {:db (assoc db :page/show-save-modal? true)}))))

(rf/reg-event-db :page/save-as
  (fn [db _]
    (-> db
      (update :page/blocks update-block-contents)
      (assoc :page/show-save-modal? true))))

(rf/reg-event-fx :page/save-to-filename
  (fn [{:keys [db]} [_ filename]]
    (let [db (update db :page/blocks update-block-contents)]
      {:page/save {:save/filename filename
                   :save/blocks (savable-blocks db)
                   :save/on-success (fn []
                                      (rf/dispatch [:page/flash (str "Saved " filename)])
                                      (rf/dispatch [:page/hide-save-modal]))}
       :db (assoc db :page/filename filename)})))

(rf/reg-event-db :page/load
  (fn [db _]
    (assoc db :page/show-load-modal? true)))

(rf/reg-event-fx :page/load-from-filename
  (fn [{:keys [db]} [_ filename]]
    {:page/load {:load/filename filename
                 :load/on-success (fn [blocks]
                                    (rf/dispatch [:page/flash (str "Loaded " filename)])
                                    (rf/dispatch [:page/load-blocks blocks])
                                    (rf/dispatch [:page/hide-load-modal]))}}))

(rf/reg-event-db :page/load-blocks
  (fn [db [_ blocks]]
    (assoc db
           :page/block-order (map :block/id blocks)
           :page/blocks (into {}
                              (map (juxt :block/id identity))
                              blocks))))

(rf/reg-event-db :page/hide-save-modal
  (fn [db _]
    (assoc db :page/show-save-modal? false)))

(rf/reg-event-db :page/hide-load-modal
  (fn [db _]
    (assoc db :page/show-load-modal? false)))

(rf/reg-event-fx :page/flash
  (fn [{:keys [db]} [_ text]]
    {:action/defer {:defer/message [:page/unflash] :defer/seconds 1}
     :db (assoc db :page/flash {:flash/text text :flash/on? true})}))

(rf/reg-event-db :page/unflash
  (fn [db _]
    (assoc-in db [:page/flash :flash/on?] false)))
