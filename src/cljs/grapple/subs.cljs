(ns grapple.subs
  (:require-macros [grapple.util :refer [spy]])
  (:require [re-frame.core :as rf]))

(rf/reg-sub
  :page/blocks
  (fn [{:keys [page/blocks page/block-order]} _]
    (map blocks block-order)))

(rf/reg-sub
  :page/show-save-modal?
  (fn [{:keys [page/show-save-modal?]} _]
    show-save-modal?))

(rf/reg-sub
  :page/show-load-modal?
  (fn [{:keys [page/show-load-modal?]} _]
    show-load-modal?))

(rf/reg-sub
  :page/flash
  (fn [{:keys [page/flash]} _]
    flash))
