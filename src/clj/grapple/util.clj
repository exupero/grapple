(ns grapple.util)

(defmacro spy [x]
  `(let [x# ~x]
     (println '~x "=>" (pr-str x#))
     x#))
