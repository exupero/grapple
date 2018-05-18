(ns grapple.css
  (:require [garden.def :refer [defstyles]]
            [garden.color :as color :refer [hsl]]
            [garden.units :as u]))

; Fonts
(def monospace ["Menlo" "Consolas" "monospace"])

; Colors
(def gray-dark (hsl 0 0 60))
(def gray-medium (hsl 0 0 85))
(def gray-light (hsl 0 0 96))

(defstyles flash
  [:.flash {:pointer-events :none
            :position :fixed
            :width (u/px 600)
            :margin [[0 (u/px 100)]]
            :font-weight :bold
            :color :steelblue
            :font-size (u/pt 16)
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
              :font-size (u/pt 10)}]
  [:.result__loading {:font-family "'PT Sans'"
                      :color gray-dark}]
  [:.result__markdown {:font-family "'PT Sans'"
                       :font-size (u/pt 14)}]
  [:.result__output {:border-bottom [[(u/px 1) :solid gray-light]]
                     :padding [[(u/rem 0.3) (u/rem 0.5)]]
                     :position :relative
                     :white-space :pre}]
  [:.result__output:before {:content "''"
                            :display :block
                            :position :absolute
                            :left 0
                            :top 0
                            :height (u/px 8)
                            :width (u/px 8)
                            :border-bottom-right-radius "100%"
                            :background-color "#e8e8e8"}]
  [:.result__output:after {:content "''"
                           :display :block
                           :position :absolute
                           :left 0
                           :top 0
                           :height (u/px 10)
                           :width (u/px 10)
                           :border-bottom-right-radius "100%"
                           :border [[(u/px 1) :solid "#e8e8e8"]]
                           :border-width [[0 (u/px 1) (u/px 1) 0]]}]
  [:.result__error {:font-size (u/pt 10)
                    :line-height 1.5}]
  [:.result__values {:padding [[(u/rem 0.3) (u/rem 0.5)]]}]
  [:.result__error {:padding [[(u/rem 0.3) (u/rem 0.5)]]}]
  [:.result__object {:color :deeppink}]
  [:.result__atom {:color :deeppink}]
  [:.result__collection {:display :flex
                         :align-items :center
                         :flex-wrap :wrap}]
  [:.result__function {:color :deeppink}]
  [:.result__keyword {:color "#3a3"}]
  [:.result__namespace {:color :deeppink}]
  [:.result__nil {:color "#3a3"}]
  [:.result__number {:color "#3a3"}]
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
  [:.body-container {:font-family ["'PT Sans'" "Verdana" "sans-serif"]
                     :margin [[(u/rem 1) (u/rem 10)]]
                     :-webkit-font-smoothing :antialiased
                     :font-size (u/pt 10)
                     :line-height (u/rem 1.5)
                     :color "#333"}]
  [:pre {:margin 0
         :white-space :pre-wrap
         :line-height 1.5}]
  [:code {:font-family monospace}]
  flash
  modal
  block
  results
  codemirror)
