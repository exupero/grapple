(ns grapple.util)

#?(:clj
    (defmacro spy [form]
      `(let [result# ~form]
         (js/console.log "%c%s => %c%o"
           "color:mediumseagreen"
           (pr-str '~form)
           "color:black"
           result#)
         result#)))
