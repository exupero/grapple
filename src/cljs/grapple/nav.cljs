(ns grapple.nav
  (:require [clojure.zip :as zip]
            [cljs-uuid-utils.core :as uuid]
            [re-frame.core :as rf]))

(defn new-block []
  {:block/id (uuid/make-random-uuid)
   :block/content ""
   :block/active? false
   :block/results []})

(defn goto [loc pred]
  (loop [loc loc]
    (if (zip/end? loc)
      loc
      (if (pred (zip/node loc))
        loc
        (recur (zip/next loc))))))

(defn move-left [loc]
  (-> loc
    zip/remove
    (zip/insert-left (zip/node loc))))

(defn move-right [loc]
  (-> loc
    zip/remove
    zip/right
    (zip/insert-right (zip/node loc))))

(defn cursor-up [id cm]
  (let [cursor (.getCursor cm)]
    (if (zero? (.-line cursor))
      (rf/dispatch [:nav/focus-previous id :line/default])
      js/CodeMirror.Pass)))

(defn cursor-down [id cm]
  (let [cursor (.getCursor cm)
        last-line (dec (.lineCount cm))]
    (if (= last-line (.-line cursor))
      (rf/dispatch [:nav/focus-next id :line/default])
      js/CodeMirror.Pass)))

(defn cursor-left [id cm]
  (let [cursor (.getCursor cm)]
    (if (and (zero? (.-line cursor)) (zero? (.-ch cursor)))
      (rf/dispatch [:nav/focus-previous id :line/end])
      js/CodeMirror.Pass)))

(defn cursor-right [id cm]
  (let [cursor (.getCursor cm)
        last-line (dec (.lineCount cm))
        last-ch (.-length (.getLine cm last-line))]
    (if (and (= last-line (.-line cursor))
             (= last-ch (.-ch cursor)))
      (rf/dispatch [:nav/focus-next id :line/start])
      js/CodeMirror.Pass)))

(defn current-word [cm]
  (let [sel (.findWordAt cm (.getCursor cm))]
    (.getRange cm (.-anchor sel) (.-head sel))))

(defn node-or-nil [loc]
  (when loc
    (zip/node loc)))

(defn block-before [db id]
  (get (db :page/blocks)
       (-> db :page/block-order
         zip/vector-zip
         (goto #(= id %))
         zip/left
         node-or-nil)))

(defn block-after [db id]
  (get (db :page/blocks)
       (-> db :page/block-order
         zip/vector-zip
         (goto #(= id %))
         zip/right
         node-or-nil)))

(defn ensure-next-block [db id {new-block-id :block/id :as new-block}]
  (if-let [next-block (block-after db id)]
    db
    (-> db
      (update :page/blocks assoc new-block-id new-block)
      (update :page/block-order conj new-block-id))))

(defn insert-new-block [f]
  (fn [{:keys [db] generate-empty-block :generator/empty-block} [_ id]]
    (let [{new-id :block/id :as new-block} (generate-empty-block)]
      {:db (-> db
             (update :page/blocks assoc new-id new-block)
             (update :page/block-order f id new-id))
       :nav/focus {:focus/id new-id}})))

(defn activate [db id]
  (-> db
    (update :page/blocks
            (fn [blocks]
              (into {}
                    (map (fn [[k v]]
                           [k (assoc v :block/active? false)]))
                    blocks)))
    (assoc-in [:page/blocks id :block/active?] true)))

;; Events

(rf/reg-event-db :nav/activate
  (fn [db [_ id pos]]
    (let [{:keys [block/id block/codemirror]} (get-in db [:page/blocks id])]
      (activate db id))))

(rf/reg-event-fx :nav/focus
  (fn [{:keys [db]} [_ id pos]]
    (let [{:keys [block/id block/codemirror]} (get-in db [:page/blocks id])]
      {:codemirror/focus {:focus/codemirror codemirror
                          :focus/position pos}
       :db (activate db id)})))

(rf/reg-event-fx :nav/focus-previous
  (fn [{:keys [db]} [_ id pos]]
    (let [{:keys [block/id]} (block-before db id)]
      {:nav/focus {:focus/id id
                   :focus/position pos}})))

(rf/reg-event-fx :nav/focus-next
  (fn [{:keys [db]} [_ id pos]]
    (let [{:keys [block/id]} (block-after db id)]
      {:nav/focus {:focus/id id
                   :focus/position pos}})))

(rf/reg-event-fx :nav/ensure-focus-next
  [(rf/inject-cofx :generator/empty-block)]
  (fn [{:keys [db] generate-empty-block :generator/empty-block} [_ id pos]]
    (let [db (ensure-next-block db id (generate-empty-block))
          {:keys [block/id]} (block-after db id)]
      {:nav/focus {:focus/id id
                   :focus/position pos}
       :db db})))

(rf/reg-event-fx :nav/insert-new-before
  [(rf/inject-cofx :generator/empty-block)]
  (insert-new-block
    (fn [ids id new-id]
      (-> (zip/vector-zip ids)
        (goto #{id})
        (zip/insert-left new-id)
        zip/root
        vec))))

(rf/reg-event-fx :nav/insert-new-after
  [(rf/inject-cofx :generator/empty-block)]
  (insert-new-block
    (fn [ids id new-id]
      (-> (zip/vector-zip ids)
        (goto #{id})
        (zip/insert-right new-id)
        zip/root
        vec))))

(rf/reg-event-fx :nav/delete
  (fn [{:keys [db]} [_ id]]
    (when-not (get-in db [:page/blocks id :block/permanent?])
      (let [move-to-block (or (block-after db id) (block-before db id))]
        {:codemirror/focus {:focus/codemirror (:block/codemirror move-to-block)}
         :db (-> db
               (update :page/block-order #(vec (filter (partial not= id) %)))
               (update :page/blocks dissoc id))}))))

(rf/reg-event-db :nav/move-up
  (fn [db [_ id]]
    (update db :page/block-order
            (fn [ids]
              (-> (zip/vector-zip ids)
                (goto #(= id %))
                move-left
                zip/root
                vec)))))

(rf/reg-event-db :nav/move-down
  (fn [db [_ id]]
    (update db :page/block-order
            (fn [ids]
              (-> (zip/vector-zip ids)
                (goto #(= id %))
                move-right
                zip/root
                vec)))))

;; Effects

(rf/reg-fx :nav/focus
  (fn [{:keys [focus/id focus/position]}]
    (rf/dispatch [:nav/focus id position])))

;; Co-effects

(rf/reg-cofx
  :generator/empty-block
  (fn [cofx _]
    (assoc cofx :generator/empty-block new-block)))
