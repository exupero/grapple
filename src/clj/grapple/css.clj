(ns grapple.css
  (:require [garden.def :refer [defstyles]]
            [garden.color :as color :refer [hsl]]
            [garden.units :as u]))

; Fonts

(def sans-serif ["'PT Sans'" "sans-serif"])
(def monospace ["Menlo" "Consolas" "monospace"])

; Colors

(def gray-dark (hsl 0 0 60))
(def gray-medium (hsl 0 0 85))
(def gray-light (hsl 0 0 96))

; Styles

(defstyles flash
  [:.flash {:pointer-events :none
            :position :fixed
            :width (u/px 600)
            :margin [[0 (u/px 100)]]
            :font-weight :bold
            :font-size (u/pt 16)
            :color :steelblue
            :display :flex
            :justify-content :center
            :z-index 2
            :opacity 0
            :-webkit-opacity 0
            :-moz-opacity 0
            :-ms-opacity 0
            :transition [[:opacity (u/s 0.2)]]
            :-webkit-transition [[:opacity (u/s 0.2)]]
            :-moz-transition [[:opacity (u/s 0.2)]]
            :-ms-transition [[:opacity (u/s 0.2)]]}]
  [:.flash--on {:opacity 1
                :-webkit-opacity 1
                :-moz-opacity 1
                :-ms-opacity 1}]
  [:.flash__text {:border-radius (u/rem 2)
                  :border [[(u/px 1) :solid (hsl 207 44 75)]]
                  :padding [[(u/rem 0.3) (u/rem 2)]]
                  :background-color :white}])

(defstyles modal
  [:.modal {:background gray-light
            :position :fixed
            :width (u/px 600)
            :margin [[0 (u/px 100)]]
            :z-index 1
            :padding (u/rem 1)
            :border [[(u/px 1) :solid gray-medium]]}]
  [:.modal__button {:margin-top (u/rem 0.5)
                    :float :right
                    :display :inline-block}])

(defstyles block
  [:.block {:position :relative
            :display :block
            :margin-bottom (u/rem 1)
            :z-index 0
            :border :solid
            :border-width [[0 0 0 (u/px 1)]]
            :border-color gray-medium
            :transition [[:border-color (u/s 0.2)]]}
   [:.CodeMirror {:background-color gray-light}]]
  [:.block--active {:border-color gray-dark}
   [:.CodeMirror {:background-color gray-light}]])

(defstyles results
  [:.results {:margin-top (u/px -1)
              :font-size (u/pt 10)
              :padding [[(u/rem 0.3) (u/rem 0.5)]]}]
  [:.result__atom {:color :deeppink}]
  [:.result__boolean {:color :blue}]
  [:.result__collection {:display :flex
                         :align-items :center
                         :flex-wrap :wrap}]
  [:.result__error {:color :red}]
  [:.result__function {:color :deeppink}]
  [:.result__interrupted {:font-family sans-serif
                          :color gray-dark}]
  [:.result__keyword {:color "#3a3"}]
  [:.result__loading {:font-family sans-serif
                      :color gray-dark}
   [:code {:background gray-light
           :border-radius (u/px 2)
           :padding (u/px 2)}]]
  [:.result__markdown {:font-family sans-serif
                       :font-size (u/pt 14)}]
  [:.result__namespace {:color :deeppink}]
  [:.result__nil {:color "#3a3"}]
  [:.result__number {:color "#3a3"}]
  [:.result__object {:color :deeppink}]
  [:.result__string {:color "#a22"}]
  [:.result__var {:color :deeppink}])

(defstyles stacktrace
  [:.stacktrace__exception {:color :red}]
  [:.stacktrace__frames {:margin-left (u/rem 2)}]
  [:.stacktrace__clojure {:color :steelblue}]
  [:.stacktrace__java {:color "#aaa"}])

(defstyles codemirror
  [:.CodeMirror {:height :auto
                 :line-height 1.5
                 :font-family monospace}]
  [:.cm-s-neat [:.CodeMirror-matchingbracket {:background-color :powderblue
                                              :outline :none}]])

(defstyles screen
  [:h1 {:font-size (u/pt 20)}]
  [:h3 {:margin 0}]
  [:.body-container {:font-family sans-serif
                     :margin [[(u/rem 1) (u/rem 10)]]
                     :-webkit-font-smoothing :antialiased
                     :font-size (u/pt 10)
                     :line-height (u/rem 1.5)
                     :color "#333"}]
  [:pre {:margin 0
         :white-space :pre-wrap
         :line-height 1.5}]
  [:code {:font-family monospace}]
  [:.cancel {:display :inline-block
             :width (u/px 9)
             :height (u/px 9)
             :border-radius (u/percent 50)
             :background-color :red
             :position :relative
             :margin-right (u/px 5)
             :cursor :pointer}
   [:&:after {:content "'\u00d7'"
              :color :white
              :position :absolute
              :left (u/px 0)
              :right 0
              :top (u/px -7)
              :text-align :center}]]
  flash
  modal
  block
  results
  codemirror)
