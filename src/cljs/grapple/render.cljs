(ns grapple.render
  (:require [clojure.string :as string]
            [goog.string :refer [unescapeEntities]]))

(def nbsp (unescapeEntities "&nbsp;"))

(defprotocol Renderable
  (render [_]))

(defrecord Print [s]
  Renderable
  (render [_]
    [:div.block-results__printed s]))

(defrecord VarName [s]
  Renderable
  (render [_]
    [:div.block-results__var s]))

(defrecord Stacktrace [class message stacktrace]
  Renderable
  (render [_]
    [:div.block-results__stacktrace
     [:div.stacktrace__exception class ": " message]
     [:div.stacktrace__frames
      (for [frame stacktrace]
        (if (= "clj" (:type frame))
          [:div.stacktrace__clojure
           (:fn frame)
           " - " (:ns frame)
           " - (" (:file frame) ":" (:line frame) ")"]
          [:div.stacktrace__java
           (:method frame)
           " - (" (:file frame) ":" (:line frame) ")"]))]]))

(defn render-collection [[open-delim close-delim] xf coll]
  [:span.block-results__collection
   open-delim
   (sequence
     (comp
       xf
       (interpose (str "," (unescapeEntities "&nbsp;"))))
     coll)
   close-delim])

(extend-protocol Renderable
  nil
  (render [_]
    [:span.block-results__nil "nil"])
  number
  (render [this]
    [:span.block-results__number this])
  string
  (render [this]
    [:span.block-results__string (pr-str this)])
  cljs.core/Keyword
  (render [this]
    [:span.block-results__keyword (pr-str this)])
  cljs.core/Symbol
  (render [this]
    [:span.block-results__symbol (pr-str this)])
  cljs.core/EmptyList
  (render [this]
    [:span.block-results__collection "()"])
  cljs.core/List
  (render [this]
    (render-collection
      [\( \)]
      (map-indexed (fn [i x]
                     (with-meta (render x) {:key i})))
      this))
  cljs.core/PersistentVector
  (render [this]
    (render-collection
      [\[ \]]
      (map-indexed (fn [i x]
                     (with-meta (render x) {:key i})))
      this))
  cljs.core/PersistentArrayMap
  (render [this]
    (render-collection
      [\{ \}]
      (map-indexed (fn [i [k v]]
                     (with-meta
                       (list
                         (with-meta (render k) {:key "key"})
                         (unescapeEntities "&nbsp;")
                         (with-meta (render v) {:key "value"}))
                       {:key i})))
      this))
  cljs.core/PersistentHashSet
  (render [this]
    (render-collection
      ["#{" \}]
      (map-indexed (fn [i v]
                     (with-meta (render v) {:key i})))
      this)))
