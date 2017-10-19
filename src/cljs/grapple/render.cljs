(ns grapple.render)

(defprotocol Renderable
  (render [_]))

(defn renderable? [x]
  (satisfies? Renderable x))
