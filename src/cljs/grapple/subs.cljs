(ns grapple.subs
  (:require-macros [grapple.util :refer [spy]])
  (:require [re-frame.core :as rf]))

(rf/reg-sub
  :page/blocks
  (fn [db _]
    (sort-by (comp :order val) (:page/blocks db))))
