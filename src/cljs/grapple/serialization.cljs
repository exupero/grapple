(ns grapple.serialization
  (:require [cljs.reader :as edn]
            [clojure.string :as string]
            [re-frame.core :as rf]))

(defonce tag-readers (atom nil))

(defn with-evaled [{:keys [value] :as result}]
  (cond
    (nil? value) result
    (number? value) (assoc result :result/evaled value)
    (string/starts-with? value "#'") result
    :else (assoc result :result/evaled (edn/read-string {:readers @tag-readers} value))))

;; Effects

(rf/reg-fx :tags/init
  (fn [{:keys [tags/read-handlers]}]
    (reset! tag-readers read-handlers)))
