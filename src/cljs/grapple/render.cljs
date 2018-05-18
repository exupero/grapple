(ns grapple.render
  (:require-macros [grapple.util :refer [spy]])
  (:require [clojure.string :as string]
            [clojure.walk :as walk]
            [cljs.reader :as edn]
            [goog.string :refer [unescapeEntities]]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [markdown.core :refer [md->html]]))

(declare ->renderable)

(defonce tag-readers (atom nil))

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
  number
  (->component [this]
    (constant [:code.result__number this]))
  string
  (->component [this]
    (constant [:code.result__string (pr-str this)]))
  function
  (->component [this]
    (constant [:code.result__function (pr-str this)]))
  object
  (->component [this]
    (constant [:code.result__object (pr-str this)]))
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

(def pending ::pending)

(defrecord Cell [id value]
  Renderable
  (->component [_]
    (r/create-class
      {:reagent-render
       (fn []
         (if (= pending value)
           [:div.result__loading "Loading..."]
           [(-> value ->renderable ->component)]))})))

(defn cell
  ([id] (->Cell id pending))
  ([id value] (->Cell id value)))

(defn update-cell [db block-id eval-id value]
  (update-in db [:page/blocks block-id :block/results]
             (partial walk/prewalk (fn [node]
                                     (if (and (instance? Cell node) (= eval-id (:id node)))
                                       (assoc node :value value) node)))))

(defrecord Generic [spec]
  Renderable
  (->component [_]
    (let [{code :update :keys [data dom]} spec
          data (clj->js data)
          update-fn (if code
                      (js/eval (str "(function(node,data){" code "})"))
                      (constantly nil))
          updater #(update-fn (r/dom-node %) data)]
      (r/create-class
        {:reagent-render
         (fn [_]
           (walk/postwalk
             #(if (instance? Generic %)
                [(->component %)] %)
             dom))
         :component-did-mount
         updater
         :component-did-update
         updater}))))

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

(defn ->renderable [value]
  (cond
    (nil? value) nil
    (and (string? value) (string/starts-with? value "#'")) (->VarName value)
    (string? value) (edn/read-string {:readers @tag-readers} value)
    :else value))

;; Effects

(rf/reg-fx :tags/init
  (fn [{:keys [tags/read-handlers]}]
    (reset! tag-readers read-handlers)))
