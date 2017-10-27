(ns grapple.util)

(defmacro spy [x]
  `(let [x# ~x]
     (println '~x "=>" x#)
     x#))
