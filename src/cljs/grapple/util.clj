(ns grapple.util)

(defmacro spy [form]
  `(let [result# ~form]
     (println (pr-str '~form) "=>")
     (println (pr-str result#))
     ; (js/console.log result#)
     result#))
