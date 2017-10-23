(ns grapple.render
  (:require [goog.string :refer [unescapeEntities]]))

(def nbsp (unescapeEntities "&nbsp;"))

(defprotocol Renderable
  (render [_]))

(defrecord Error [err]
  Renderable
  (render [_]
    [:div.code-result__error err]))

(defrecord Print [s]
  Renderable
  (render [_]
    [:div.code-result__print s]))

(defrecord VarName [s]
  Renderable
  (render [_]
    [:div.code-result__var s]))

(defn render-collection [[open-delim close-delim] xf coll]
  [:span.code-result__collection
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
    [:span.code-result__nil "nil"])
  number
  (render [this]
    [:span.code-result__number this])
  string
  (render [this]
    [:span.code-result__string (pr-str this)])
  cljs.core/Keyword
  (render [this]
    [:span.code-result__keyword (pr-str this)])
  cljs.core/Symbol
  (render [this]
    [:span.code-result__symbol (pr-str this)])
  cljs.core/EmptyList
  (render [this]
    [:span.code-result__collection "()"])
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
