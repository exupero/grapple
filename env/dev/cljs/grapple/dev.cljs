(ns ^:figwheel-no-load grapple.dev
  (:require [devtools.core :as devtools]
            [grapple.core :as core]))

(devtools/install!)

(enable-console-print!)

(core/init! core/default-tags)
