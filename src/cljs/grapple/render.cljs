(ns grapple.render
  (:require-macros [grapple.util :refer [spy]])
  (:require [clojure.string :as string]
            [goog.string :refer [unescapeEntities]]
            [reagent.core :as r]))

(def nbsp (unescapeEntities "&nbsp;"))

(defn constant [form]
  (r/create-class
    {:reagent-render
     (constantly form)}))

(defprotocol Renderable
  (render [_]))

(defrecord Print [s]
  Renderable
  (render [_]
    (constant [:div.block-results__printed s])))

(defrecord VarName [s]
  Renderable
  (render [_]
    (constant [:div.block-results__var s])))

(defrecord Stacktrace [class message stacktrace]
  Renderable
  (render [_]
    (constant
      [:div.block-results__stacktrace
       [:div.stacktrace__exception class ": " message]
       [:div.stacktrace__frames
        (for [[i frame] (map-indexed vector stacktrace)]
          (if (= "clj" (:type frame))
            [:div.stacktrace__clojure {:key i}
             (:fn frame)
             " - " (:ns frame)
             " - (" (:file frame) ":" (:line frame) ")"]
            [:div.stacktrace__java {:key i}
             (:method frame)
             " - (" (:file frame) ":" (:line frame) ")"]))]])))

(defn render-collection [[open-delim close-delim] xf coll]
  (constant
    [:span.block-results__collection
     open-delim
     (sequence
       (comp
         xf
         (interpose (str "," nbsp)))
       coll)
     close-delim]))

(extend-protocol Renderable
  nil
  (render [_]
    (constant [:span.block-results__nil "nil"]))
  number
  (render [this]
    (constant [:span.block-results__number this]))
  string
  (render [this]
    (constant [:span.block-results__string (pr-str this)]))
  cljs.core/Keyword
  (render [this]
    (constant [:span.block-results__keyword (pr-str this)]))
  cljs.core/Symbol
  (render [this]
    (constant [:span.block-results__symbol (pr-str this)]))
  cljs.core/EmptyList
  (render [this]
    (constant [:span.block-results__collection "()"]))
  cljs.core/List
  (render [this]
    (render-collection
      [\( \)]
      (map-indexed (fn [i x]
                     ^{:key i} [(render x)]))
      this))
  cljs.core/PersistentVector
  (render [this]
    (render-collection
      [\[ \]]
      (map-indexed (fn [i x]
                     ^{:key i} [(render x)]))
      this))
  cljs.core/PersistentArrayMap
  (render [this]
    (render-collection
      [\{ \}]
      (map-indexed (fn [i [k v]]
                     (with-meta
                       (list
                         ^{:key "key"} [(render k)]
                         nbsp
                         ^{:key "value"} [(render v)])
                       {:key i})))
      this))
  cljs.core/PersistentHashSet
  (render [this]
    (render-collection
      ["#{" \}]
      (map-indexed (fn [i v]
                     ^{:key i} [(render v)]))
      this)))
