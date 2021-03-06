(ns grapple.render
  (:require-macros [grapple.util :refer [spy]])
  (:require [clojure.string :as string]
            [goog.string :refer [unescapeEntities]]
            [reagent.core :as r]
            [markdown.core :refer [md->html]]))

(def nbsp (unescapeEntities "&nbsp;"))

(defn constant [form]
  (r/create-class
    {:reagent-render
     (constantly form)}))

(defn literal [form]
  (r/create-class
    {:reagent-render
     (fn []
       [:div {:ref #(when % (.appendChild % form))}])}))

(defn collection [[open-delim inter-delim close-delim] xf coll]
  (constant
    [:code.result__collection
     open-delim
     (sequence
       (comp
         xf
         (interpose inter-delim))
       coll)
     close-delim]))

(defprotocol Renderable
  (->component [_]))

(extend-protocol Renderable
  nil
  (->component [_]
    (constant [:code.result__nil "nil"]))
  boolean
  (->component [this]
    (constant [:code.result__boolean (pr-str this)]))
  number
  (->component [this]
    (constant [:code.result__number this]))
  string
  (->component [this]
    (constant [:code.result__string (pr-str this)]))
  function
  (->component [this]
    (constant [:code.result__function (pr-str this)]))
  js/HTMLElement
  (->component [this]
    (literal this))
  cljs.core/Keyword
  (->component [this]
    (constant [:code.result__keyword (pr-str this)]))
  cljs.core/Symbol
  (->component [this]
    (constant [:code.result__symbol (pr-str this)]))
  cljs.core/Atom
  (->component [this]
    (constant [:code.result__atom (pr-str this)]))
  cljs.core/EmptyList
  (->component [this]
    (constant [:code.result__collection "()"]))
  cljs.core/List
  (->component [this]
    (collection
      [\( nbsp \)]
      (map-indexed (fn [i x]
                     ^{:key i} [(->component x)]))
      this))
  cljs.core/LazySeq
  (->component [this]
    (collection
      [\( nbsp \)]
      (map-indexed (fn [i x]
                     ^{:key i} [(->component x)]))
      this))
  cljs.core/PersistentVector
  (->component [this]
    (collection
      [\[ nbsp \]]
      (map-indexed (fn [i x]
                     ^{:key i} [(->component x)]))
      this))
  cljs.core/PersistentArrayMap
  (->component [this]
    (collection
      [\{ (str "," nbsp) \}]
      (map-indexed (fn [i [k v]]
                     (with-meta
                       (list
                         ^{:key "key"} [(->component k)]
                         nbsp
                         ^{:key "value"} [(->component v)])
                       {:key i})))
      this))
  cljs.core/PersistentHashSet
  (->component [this]
    (collection
      ["#{" nbsp \}]
      (map-indexed (fn [i v]
                     ^{:key i} [(->component v)]))
      this)))

(defrecord Error [msg]
  Renderable
  (->component [_]
    (constant [:span.result__error msg])))

(defrecord Markdown [content]
  Renderable
  (->component [_]
    (let [rerender (fn [this]
                     (let [node (r/dom-node this)]
                       (js/MathJax.Hub.Queue #js ["Typeset" js/MathJax.Hub node])
                       (doseq [node (array-seq (.querySelectorAll node "a"))]
                         (.addEventListener node "click" #(.stopPropagation %)))))]
      (r/create-class
        {:reagent-render
         (fn [_]
           (let [html (md->html content)]
             [:div.result__markdown {:dangerouslySetInnerHTML {:__html html}}]))
         :component-did-mount
         rerender
         :component-did-update
         rerender}))))

(defrecord Print [s]
  Renderable
  (->component [_]
    (constant [:div.result__printed s])))

(defrecord Reagent [component]
  Renderable
  (->component [_]
    (cond
      (map? component) (r/create-class component)
      (vector? component) (constantly component)
      (fn? component) (r/create-class {:reagent-render component}))))

(defrecord Stacktrace [class message stacktrace]
  Renderable
  (->component [_]
    (constant
      [:div.result__stacktrace
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

(defrecord VarName [s]
  Renderable
  (->component [_]
    (constant [:code.result__var s])))

(defrecord VarNamespace [nm]
  Renderable
  (->component [_]
    (constant [:code.result__namespace (str "#namespace" nm)])))
