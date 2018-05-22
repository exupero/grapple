(ns grapple.util)

(defn load-script!
  ([script] (load-script! script (fn []) (fn [])))
  ([script on-success] (load-script! script on-success (fn [])))
  ([script on-success on-error]
   (let [node (doto (js/document.createElement "script")
                (.setAttribute "type" "text/javascript")
                (.setAttribute "charset" "utf8")
                (.setAttribute "async" true)
                (.setAttribute "src" script))]
     (set! (.-onload node)
           (fn []
             (this-as this
                      (set! (.-onload this) nil)
                      (set! (.-onerror this) nil)
                      (on-success this))))
     (set! (.-onerror node)
           (fn []
             (this-as this
                      (set! (.-onload this) nil)
                      (set! (.-onerror this) nil)
                      (on-error this))))
     (.appendChild js/document.head node))))
