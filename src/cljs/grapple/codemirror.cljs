(ns grapple.codemirror
  (:require-macros [grapple.util :refer [spy]])
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            cljsjs.codemirror
            cljsjs.codemirror.mode.clojure
            cljsjs.codemirror.mode.markdown
            cljsjs.codemirror.addon.edit.closebrackets
            cljsjs.codemirror.addon.edit.matchbrackets
            [grapple.nav :as nav]))

(defn key-bindings [id]
  {"Up" #(nav/cursor-up id %)
   "Down" #(nav/cursor-down id %)
   "Left" #(nav/cursor-left id %)
   "Right" #(nav/cursor-right id %)
   "Shift-Enter" #(rf/dispatch [:block/eval id (.getValue %)])
   "Ctrl-Shift-Enter" #(rf/dispatch [:blocks/evaluate])
   "Ctrl-G Ctrl-B" #(rf/dispatch [:nav/insert-new-before id])
   "Ctrl-G Ctrl-D" #(rf/dispatch [:nav/move-down id])
   "Ctrl-G Ctrl-E" #(rf/dispatch [:page/save-as])
   "Ctrl-G Ctrl-L" #(rf/dispatch [:page/load])
   "Ctrl-G Ctrl-N" #(rf/dispatch [:nav/insert-new-after id])
   "Ctrl-G Ctrl-S" #(rf/dispatch [:page/save])
   "Ctrl-G Ctrl-U" #(rf/dispatch [:nav/move-up id])
   "Ctrl-G Ctrl-X" #(rf/dispatch [:nav/delete id])
   })

;; Components

(defn codemirror [id active? content]
  (let [textarea (r/atom nil)]
    (r/create-class
      {:reagent-render
       (fn [_ _ content]
         [:div
          [:textarea {:ref #(reset! textarea %)
                      :default-value content
                      :style {:display "none"}}]])
       :component-did-mount
       (fn [this]
         (rf/dispatch [:codemirror/init id active? @textarea]))})))

;; Events

(rf/reg-event-fx :codemirror/init
  (fn [{:keys [db]} [_ id active? node]]
    (let [extra-keys (clj->js (key-bindings id))]
      (js/CodeMirror.normalizeKeyMap extra-keys)
      {:codemirror/init {:codemirror/id id
                         :codemirror/node node
                         :codemirror/focus? active?
                         :codemirror/on-success #(rf/dispatch [:block/codemirror id %])
                         :codemirror/config {:lineWrapping true
                                             :viewportMargin js/Infinity
                                             :matchBrackets true
                                             :autoCloseBrackets true
                                             :mode "clojure"
                                             :theme "neat"
                                             :cursorHeight 0.9
                                             :extraKeys extra-keys}}})))

;; Effects

(rf/reg-fx :codemirror/init
  (fn [{:keys [codemirror/id codemirror/node codemirror/config
               codemirror/focus? codemirror/on-success]}]
    (let [cm (js/CodeMirror.fromTextArea node (clj->js config))]
      (.on cm "focus" #(rf/dispatch [:nav/activate id]))
      (when focus?
        (.focus cm)
        (.setCursor cm (.lineCount cm) 0))
      (on-success cm))))

(rf/reg-fx :codemirror/focus
  (fn [{cm :focus/codemirror :keys [focus/position]}]
    (when cm
      (.focus cm)
      (condp = position
        :line/start (.execCommand cm "goDocStart")
        :line/end (.execCommand cm "goDocEnd")
        nil))))
