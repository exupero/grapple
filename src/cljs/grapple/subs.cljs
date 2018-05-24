(ns grapple.subs
  (:require-macros [grapple.util :refer [spy]])
  (:require [clojure.walk :as walk]
            [re-frame.core :as rf]
            [grapple.cell :as cell]))

(rf/reg-sub :page/blocks
  (fn [{:keys [page/blocks page/cells page/block-order]} _]
    (map
      (fn [block-id]
        (-> (blocks block-id)
          (update :block/results
                  (fn [cell-ids]
                    (walk/prewalk
                      (fn [node]
                        (if (instance? cell/Cell node)
                          (get cells (:id node))
                          node))
                      (map cells cell-ids))))))
      block-order)))

(rf/reg-sub :page/show-save-modal?
  (fn [{:keys [page/show-save-modal?]} _]
    show-save-modal?))

(rf/reg-sub :page/show-load-modal?
  (fn [{:keys [page/show-load-modal?]} _]
    show-load-modal?))

(rf/reg-sub :page/flash
  (fn [{:keys [page/flash]} _]
    flash))
