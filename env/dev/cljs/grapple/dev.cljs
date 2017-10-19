(ns ^:figwheel-no-load grapple.dev
  (:require
    [grapple.core :as core]
    [devtools.core :as devtools]))

(devtools/install!)

(enable-console-print!)

(core/init!)
