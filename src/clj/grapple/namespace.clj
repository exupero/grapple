(ns grapple.namespace)

(defn required-namespace [req]
  (if (vector? req)
    (first req)
    req))

(defn dequote [form]
  (if (and (list? form) (= 'quote (first form)))
    (second form)
    form))

(defn required [form]
  (when (list? form)
    (condp = (first form)
      'ns (sequence
            (comp
              (filter list?)
              (filter (comp #{:require} first))
              (mapcat rest)
              (map required-namespace))
            (tree-seq sequential? seq form))
      'require (sequence
                 (comp
                   (map dequote)
                   (map required-namespace))
                 (rest form))
      nil)))
