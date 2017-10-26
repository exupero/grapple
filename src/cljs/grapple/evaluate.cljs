(ns grapple.evaluate
  (:require-macros [grapple.util :refer [spy]])
  (:require [cognitect.transit :as transit]
            [cljs.reader :as edn]
            grapple.vega))

(def custom-readers
  {'grapple.plot.Vega (fn [{:keys [spec]}]
                        (grapple.vega/->Vega spec))})

(def write-handlers
  {grapple.vega/Vega (transit/write-handler
                       (constantly "grapple.plot.Vega")
                       (fn [v] v))})

(defn results-with-evaled [results]
  (map (fn [{:keys [value] :as result}]
         (if value
           (assoc result :result/evaled (edn/read-string {:readers custom-readers} value))
           result))
       results))
