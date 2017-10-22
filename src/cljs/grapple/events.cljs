(ns grapple.events
  (:require-macros [grapple.util :refer [spy]])
  (:require [re-frame.core :as rf]
            [cljs-uuid-utils.core :as uuid]))

(rf/reg-event-fx
  :page/init
  [(rf/inject-cofx :generate/ns-name)]
  (fn [{generated-ns-name :generated/ns-name} _]
    {:clojure/init {:init/on-success #(rf/dispatch [:page/session-id %])}
     :db {:page/session-id nil
          :page/blocks {"1" {:block/order 0
                             :block/code (str "(ns " generated-ns-name "\n  (:require [grapple.plot :as plot]))")}
                        "2" {:block/order 1
                             :block/code "(plot/scatter [[10 1] [4 3] [8 2]])"}}}}))

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
  (fn [{:keys [db generated/uuid]} [_ id code]]
    {:clojure/eval {:eval/code code
                    :eval/session-id (db :page/session-id)
                    :eval/eval-id (uuid/uuid-string uuid)
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
