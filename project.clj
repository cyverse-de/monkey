(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.cyverse/monkey "2.8.1-SNAPSHOT"
  :description "A metadata database crawler. It synchronizes the tag documents in the search data
                index with the tag information inthe metadata database.  🐒"
  :url "https://github.com/cyverse-de/monkey"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :manifest {"Git-Ref" ~(git-ref)}
  :aot [monkey.index monkey.tags monkey.core]
  :main monkey.core
  :uberjar-name "monkey-standalone.jar"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.postgresql/postgresql "42.2.4"]
                 [org.clojure/java.jdbc "0.3.5"]
                 [clojurewerkz/elastisch "2.2.1"]
                 [com.novemberain/langohr "3.5.1"]
                 [me.raynes/fs "1.4.6"]
                 [slingshot "0.10.3"]
                 [org.cyverse/clojure-commons "2.8.0"]
                 [org.cyverse/common-cli "2.8.1"]
                 [org.cyverse/event-messages "0.0.1"]
                 [org.cyverse/service-logging "2.8.0"]]
  :eastwood {:exclude-namespaces [monkey.actions :test-paths]
             :linters [:wrong-arity :wrong-ns-form :wrong-pre-post :wrong-tag :misplaced-docstrings]}
  :plugins [[jonase/eastwood "0.2.3"]
            [test2junit "1.2.2"]]
  :profiles {:dev {:resource-paths ["conf/test"]}}
  :jvm-opts ["-Dlogback.configurationFile=/etc/iplant/de/logging/monkey-logging.xml"])
