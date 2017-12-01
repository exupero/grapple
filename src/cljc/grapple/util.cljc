(ns grapple.util)

#?(:clj
    (defmacro spy [form]
      `(let [result# ~form]
         (println (pr-str '~form) "=>")
         (println (pr-str result#))
         result#)))
