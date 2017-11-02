(ns grapple.table)

(defrecord Table [headings rows])

(defn matrix [rows]
  (->Table nil rows))

(defn table [maps]
  (let [headings (keys (first maps))]
    (->Table
      headings
      (map (fn [m]
             (map #(get m %) headings))
           maps))))
