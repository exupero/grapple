(defproject org.exupero/grapple "0.1.0-SNAPSHOT"
  :description "Grapple Notebook REPL"
  :url "https://github.com/exupero/grapple"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.946" :scope "provided"]
                 [org.clojure/tools.nrepl "0.2.13"]
                 [org.clojure/tools.reader "1.2.1"]
                 [ring "1.6.2"]
                 [ring-server "0.5.0"]
                 [ring/ring-defaults "0.3.1"]
                 [http-kit "2.2.0"]
                 [reagent "0.7.0"]
                 [reagent-utils "0.2.1"]
                 [re-frame "0.10.2"]
                 [compojure "1.6.0"]
                 [hiccup "1.0.5"]
                 [yogthos/config "0.9"]
                 [cljs-ajax "0.7.2"]
                 [com.cognitect/transit-clj "0.8.300"]
                 [com.cognitect/transit-cljs "0.8.243"]
                 [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]
                 [cheshire "5.8.0"]
                 [cider/cider-nrepl "0.10.2"]
                 [com.taoensso/sente "1.11.0"]
                 [markdown-clj "1.0.1"]
                 [cljsjs/codemirror "5.24.0-1"]
                 [garden "1.3.5"]]

  :plugins [[lein-environ "1.0.2"]
            [lein-cljsbuild "1.1.7"]
            [lein-garden "0.3.0"]]

  :min-lein-version "2.5.0"

  :clean-targets ^{:protect false}
  [:target-path
   [:cljsbuild :builds :dev :compiler :output-dir]
   [:cljsbuild :builds :dev :compiler :output-to]
   [:cljsbuild :builds :app :compiler :output-dir]
   [:cljsbuild :builds :app :compiler :output-to]]

  :source-paths ["src/clj" "src/cljc"]
  :resource-paths ["resources" "target/cljsbuild"]

  :cljsbuild
  {:builds {:dev
            {:source-paths ["src/cljs" "src/cljc" "env/dev/cljs"]
             :figwheel {:on-jsload "grapple.core/mount-root"}
             :compiler
             {:main "grapple.dev"
              :asset-path "/js/dev"
              :output-to "target/cljsbuild/public/js/app.js"
              :output-dir "target/cljsbuild/public/js/dev"
              :source-map true
              :optimizations :none
              :pretty-print  true}}
            :prod
            {:source-paths ["src/cljs" "src/cljc" "env/prod/cljs"]
             :jar true
             :compiler
             {:asset-path "/js/out"
              :output-to "target/cljsbuild/public/js/app.js"
              :output-dir "target/cljsbuild/public/js/out"
              :optimizations :simple
              :pretty-print false}}}}

  :figwheel
  {:http-server-root "public"
   :server-port 3450
   :nrepl-port 7003
   :nrepl-middleware ["cemerick.piggieback/wrap-cljs-repl"]
   :css-dirs ["resources/public/css"]
   :ring-handler grapple.figwheel/app}

  :garden
  {:builds [{:id "screen"
             :source-paths ["src/clj"]
             :stylesheet grapple.css/screen
             :compiler {:output-to "resources/public/css/screen.css"
                        :pretty-print? true}}]}

  :profiles {:dev {:dependencies [[binaryage/devtools "0.9.7"]
                                  [ring/ring-mock "0.3.1"]
                                  [ring/ring-devel "1.6.2"]
                                  [prone "1.1.4"]
                                  [figwheel-sidecar "0.5.14"]
                                  [com.cemerick/piggieback "0.2.2"]
                                  [pjstadig/humane-test-output "0.8.3"]]
                   :plugins [[lein-figwheel "0.5.14"]]
                   :injections [(require 'pjstadig.humane-test-output)
                                (pjstadig.humane-test-output/activate!)]
                   :source-paths ["env/dev/clj"]
                   :env {:dev true}}
             :provided {:source-paths ["env/prod/clj"]
                        :prep-tasks ["compile" ["cljsbuild" "once" "prod"]]
                        :env {:production true}}
             :uberjar {:source-paths ["env/prod/clj"]
                       :prep-tasks ["compile" ["cljsbuild" "once" "prod"]]
                       :env {:production true}
                       :omit-source true}}

  :prep-tasks [["cljsbuild" "once" "min"] ["garden" "once"] "compile"]
  )
