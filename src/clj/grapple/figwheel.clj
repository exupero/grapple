(ns grapple.figwheel
  (:require grapple.handler))

(grapple.handler/start-notebook 7888)

(def app @grapple.handler/app)
