(ns grapple.core
  (:require-macros [grapple.util :refer [spy]])
  (:require [clojure.string :as string]
            [cljs.reader :as edn]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [ajax.core :as http]
            [cognitect.transit :as transit]
            [cljs-uuid-utils.core :as uuid]
            cljsjs.codemirror
            cljsjs.codemirror.mode.clojure
            cljsjs.codemirror.addon.edit.closebrackets
            cljsjs.codemirror.addon.edit.matchbrackets
            cljsjs.highlight
            cljsjs.highlight.langs.clojure
            grapple.render
            grapple.vega))

(def custom-readers
  {'grapple.plot.Vega (fn [{:keys [spec]}]
                        (grapple.vega/->Vega spec))})

;; -------------------------
;; Events

(rf/reg-event-fx
  :page/init
  (fn [_ _]
    {:clojure/init {:init/on-success #(rf/dispatch [:page/session-id %])}
     :db {:page/session-id nil
          :page/blocks {"1" {:block/order 0
                             :block/code "(ns scenic-overlook\n  (:require [grapple.plot :as plot]))"}
                        "2" {:block/order 1
                             :block/code "(plot/scatter [])"}}}}))

(rf/reg-event-db
  :page/session-id
  (fn [db [_ session-id]]
    (assoc db :page/session-id session-id)))

(rf/reg-event-fx
  :codemirror/init
  (fn [_ [_ id node]]
    {:codemirror/init {:codemirror/node node
                       :codemirror/config {:lineNumbers true
                                           :viewportMargin js/Infinity
                                           :matchBrackets true
                                           :autoCloseBrackets true
                                           :mode "clojure"
                                           :theme "neat"
                                           :cursorHeight 0.9
                                           :extraKeys {"Shift-Enter" #(rf/dispatch [:block/eval id (.getValue %)])}}
                       :codemirror/on-success #(rf/dispatch [:block/codemirror id %])}}))

(rf/reg-event-db
  :block/codemirror
  (fn [db [_ id cm]]
    (assoc-in db [:page/blocks id :block/codemirror] cm)))

(rf/reg-event-fx
  :block/eval
  [(rf/inject-cofx :generate/uuid)]
  (fn [{:keys [db generate-uuid]} [_ id code]]
    {:clojure/eval {:eval/code code
                    :eval/session-id (db :page/session-id)
                    :eval/eval-id (uuid/uuid-string (generate-uuid))
                    :eval/on-success
                    (fn [forms]
                      (rf/dispatch [:page/session-id (-> forms first :session)])
                      (rf/dispatch [:block/results id forms]))}
     :db (assoc-in db [:page/blocks id :block/code] code)}))

(rf/reg-event-db
  :block/results
  (fn [db [_ id results]]
    (-> db
      (assoc-in [:page/blocks id :block/results] results)
      (assoc-in [:page/blocks id :block/eval-id] (-> results first :id)))))

;; -------------------------
;; Co-effects

(rf/reg-cofx
  :generate/uuid
  (fn [cofx _]
    (assoc cofx :generate-uuid uuid/make-random-uuid)))

;; -------------------------
;; Effects

(rf/reg-fx
  :clojure/init
  (fn [{:keys [init/on-success]}]
    (http/POST "/api/init"
               {:headers {"X-CSRF-Token" js/antiForgeryToken}
                :handler (fn [resp]
                           (let [results (transit/read (transit/reader :json) resp)]
                             (on-success (-> results first :new-session))))})))

(rf/reg-fx
  :clojure/eval
  (fn [{:keys [eval/code eval/session-id eval/eval-id eval/on-success]}]
    (http/POST "/api/eval"
               {:params {:code code :session-id session-id :eval-id eval-id}
                :headers {"X-CSRF-Token" js/antiForgeryToken}
                :handler (fn [resp]
                           (let [results (map (fn [{:keys [value] :as result}]
                                                (if value
                                                  (assoc result :evaled (edn/read-string {:readers custom-readers} value))
                                                  result))
                                              (transit/read (transit/reader :json) resp))]
                             (on-success results)))})))

(rf/reg-fx
  :codemirror/init
  (fn [{:keys [codemirror/node codemirror/config codemirror/on-success]}]
    (let [cm (js/CodeMirror.fromTextArea node (clj->js config))]
      (on-success cm))))

;; -------------------------
;; Subscriptions

(rf/reg-sub
  :page/blocks
  (fn [db _]
    (sort-by (comp :order val) (:page/blocks db))))

;; -------------------------
;; Views

(defn codemirror [id _]
  (r/create-class
    {:reagent-render
     (fn [id code _]
       [:textarea {:default-value code
                   :style {:display "none"}}])
     :component-did-mount
     (fn [this]
       (rf/dispatch [:codemirror/init id (r/dom-node this)]))}))

(defn highlight-block [node]
  (let [blocks (array-seq (.querySelectorAll node "pre code"))]
    (doseq [block blocks]
      (js/hljs.highlightBlock block))))

(defn code-results [results]
  (r/create-class
    {:reagent-render
     (fn [results]
       [:div.code-result
        (for [[i {:keys [status value evaled out err]}] (map-indexed vector results)
              :while (not (= status ["done"]))]
          [:div {:key i}
           (cond
             err
             [:pre [:code.lang-clojure.code-result__error err]]
             (and evaled (grapple.render/renderable? evaled))
             (grapple.render/render evaled)
             (and (string? value) (string/starts-with? value "#'"))
             [:pre [:code.lang-clojure.code-result__var value]]
             out
             [:pre [:code.lang-clojure.code-result__out out]]
             :else
             [:pre [:code.lang-clojure.code-result__value value]])])])
     :component-did-mount
     (fn [this]
       (highlight-block (r/dom-node this)))
     :component-did-update
     (fn [this]
       (highlight-block (r/dom-node this)))}))

(defn notebook []
  [:div
   [:div.blocks
    (for [[id {:keys [block/code block/results]}] @(rf/subscribe [:page/blocks])]
      [:div.block {:key id}
       [codemirror id code]
       [code-results results]])]])

;; -------------------------
;; Initialize app

(defn mount-root []
  (r/render [notebook] (.getElementById js/document "app")))

(defn init! []
  (rf/dispatch-sync [:page/init])
  (mount-root))
