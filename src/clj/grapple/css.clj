(ns grapple.css
  (:require [garden.def :refer [defstyles]]
            [garden.color :as color :refer [hsl]]
            [garden.units :as u]))

(defstyles flash
  [:.flash {:pointer-events "none"
            :position "fixed"
            :width (u/px 600)
            :margin [[0 (u/px 100)]]
            :font-weight "bold"
            :color "steelblue"
            :font-size "16pt"
            :display "flex"
            :justify-content "center"
            :z-index "2"
            :opacity "0"
            :-webkit-opacity "0"
            :-moz-opacity "0"
            :-ms-opacity "0"
            :transition [["opacity" (u/s 0.2)]]
            :-webkit-transition [["opacity" (u/s 0.2)]]
            :-moz-transition [["opacity" (u/s 0.2)]]
            :-ms-transition [["opacity" (u/s 0.2)]]}]
  [:.flash--on {:opacity "1"
                :-webkit-opacity "1"
                :-moz-opacity "1"
                :-ms-opacity "1"}]
  [:.flash__text {:border-radius (u/rem 2)
                  :border [[(u/px 1) "solid" (hsl 207 44 75)]]
                  :padding [[(u/rem 0.3) (u/rem 2)]]
                  :background-color "white"}]
  )

(defstyles modal
  [:.modal {:background "lightgray"
            :position "fixed"
            :width (u/px 600)
            :margin [[0 (u/px 100)]]
            :z-index "1"
            :padding (u/rem 1)
            :border [[(u/px 1) "solid" "gray"]]}]
  [:.modal__button {:margin-top (u/rem 0.5)
                    :float "right"
                    :display "inline-block"}]
  )

(defstyles block
  [:.block {:position :relative
            :display :block
            :margin-bottom (u/rem 0.5)
            :z-index "0"
            :border :solid
            :border-width [[0 0 0 (u/px 1)]]
            :border-color (hsl 0 0 80)
            :transition [[:border-color (u/s 0.2)]]}
   [:&:before {:position :absolute
               :content "attr(data-abbr)"
               :font-family "'PT Mono'"
               :font-size (u/pt 8)
               :color (hsl 0 0 75)
               :top 0
               :left (u/rem -2)
               :width (u/rem 1.7)
               :text-align :right}]
   [:.CodeMirror {:background-color (hsl 0 0 96)}]]
  [:.block--active {:border-color (hsl 0 0 50)}
   [:.CodeMirror {:background-color (hsl 0 0 96)}]]
  [:.block--processing
   [:&:after {:content "''"
              :position :absolute
              :top 0
              :bottom 0
              :background-color (hsl 0 0 50)
              :left (u/px -2)
              :width (u/px 2)}]])

(defstyles results
  [:.results {:margin-top (u/px -1)
              :font-family "monospace"
              :font-size (u/px 14.625)}]
  [:.results__markdown {:font-family "'PT Sans'"}]
  [:.results__output {:border-bottom [[(u/px 1) "solid" "lightgray"]]
                      :padding [[(u/rem 0.3) (u/rem 0.5)]]
                      :position "relative"
                      :white-space "pre"}]
  [:.results__output:before {:content "''"
                             :display "block"
                             :position "absolute"
                             :left "0"
                             :top "0"
                             :height (u/px 8)
                             :width (u/px 8)
                             :border-bottom-right-radius "100%"
                             :background-color "#e8e8e8"}]
  [:.results__output:after {:content "''"
                            :display "block"
                            :position "absolute"
                            :left "0"
                            :top "0"
                            :height (u/px 10)
                            :width (u/px 10)
                            :border-bottom-right-radius "100%"
                            :border [[(u/px 1) "solid" "#e8e8e8"]]
                            :border-width [[0 (u/px 1) (u/px 1) 0]]}]
  [:.results__error {:font-size "10pt"
                     :line-height "1.5"}]
  [:.results__values {:padding [[(u/rem 0.3) (u/rem 0.5)]]}]
  [:.results__error {:padding [[(u/rem 0.3) (u/rem 0.5)]]}]
  [:.results__var {:color "deeppink"}]
  [:.results__namespace {:color "dodgerblue"}]
  [:.results__function {:color "dodgerblue"}]
  [:.results__nil {:color "#3a3"}]
  [:.results__number {:color "#3a3"}]
  [:.results__string {:color "#a22"}]
  [:.results__keyword {:color "#3a3"}]
  [:.results__collection {:display "flex"
                          :align-items "center"
                          :flex-wrap "wrap"}]
  )

(defstyles stacktrace
  [:.stacktrace__exception {:color "red"}]
  [:.stacktrace__frames {:margin-left (u/rem 2)}]
  [:.stacktrace__clojure {:color "steelblue"}]
  [:.stacktrace__java {:color "#aaa"}]
  )

(defstyles codemirror
  [:.CodeMirror {:height "auto"
                 :line-height "1.5"}]
  [:.cm-s-neat [:.CodeMirror-matchingbracket {:background-color "powderblue"
                                              :outline "none"}]]
  )

(defstyles screen
  [:h1 {:font-size "20pt"}]
  [:h3 {:margin 0}]
  [:.body-container {:font-family ["'PT Sans'" "Verdana" "sans-serif"]
                     :max-width (u/px 800)
                     :margin [[(u/rem 1) "auto"]]
                     :-webkit-font-smoothing "antialiased"
                     :font-size (u/rem 1.125)
                     :line-height (u/rem 1.5)
                     :color "#333"}]
  [:pre {:margin "0"
         :white-space "pre-wrap"
         :line-height "1.5"}]
  [:button {:padding [[(u/rem 0.5) (u/rem 0.8)]]
            :font-size "12pt"
            :background "blue"
            :color "white"
            :border "none"
            :border-radius (u/rem 0.3)}]
  flash
  modal
  block
  results
  codemirror
  )
