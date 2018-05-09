(ns grapple.render
  (:require-macros [grapple.util :refer [spy]])
  (:require [clojure.string :as string]
            [clojure.walk :as walk]
            [goog.string :refer [unescapeEntities]]
            [reagent.core :as r]
            [re-frame.core :as rf]))

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

(defn render-collection [[open-delim inter-delim close-delim] xf coll]
  (constant
    [:span.block-results__collection
     open-delim
     (sequence
       (comp
         xf
         (interpose inter-delim))
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
      [\( nbsp \)]
      (map-indexed (fn [i x]
                     ^{:key i} [(render x)]))
      this))
  cljs.core/PersistentVector
  (render [this]
    (render-collection
      [\[ nbsp \]]
      (map-indexed (fn [i x]
                     ^{:key i} [(render x)]))
      this))
  cljs.core/PersistentArrayMap
  (render [this]
    (render-collection
      [\{ (str "," nbsp) \}]
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
      ["#{" nbsp \}]
      (map-indexed (fn [i v]
                     ^{:key i} [(render v)]))
      this)))

(defrecord Ns [nm]
  Renderable
  (render [_]
    (constant [:span.block-results__namespace (str "#namespace" nm)])))

(defrecord Generic [spec]
  Renderable
  (render [_]
    (let [{code :update :keys [data dom]} spec
          update-fn (if code
                      (js/eval (str "(function(node,data){" code "})"))
                      (constantly nil))
          updater (fn [this]
                    (update-fn (r/dom-node this) (clj->js data)))]
      (r/create-class
        {:reagent-render
         (fn [_]
           (walk/postwalk
             #(if (instance? Generic %)
                [(render %)] %)
             dom))
         :component-did-mount
         updater
         :component-did-update
         updater}))))
