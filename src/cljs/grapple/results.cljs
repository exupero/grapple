(ns grapple.results
  (:require-macros [grapple.util :refer [spy]])
  (:require [clojure.string :as string]
            [grapple.render :as r]))

(defn result [{:keys [value result/evaled out class message stacktrace] :as result}]
  (cond
    stacktrace (r/->Stacktrace class message stacktrace)
    evaled evaled
    out (r/->Print out)
    (and (string? value) (string/starts-with? value "#'")) (r/->VarName value)))

(defn results [rs]
  [:div.block-results
   (for [[field node] [[:out :div.block-results__output]
                       [:value :div.block-results__values]
                       [:stacktrace :div.block-results__error]]]
     (when-let [values (seq (filter #(contains? % field) rs))]
       [node {:key field}
        (for [[i r] (map-indexed vector values)]
          [:div {:key i}
           [(r/render (result r))]])]))])