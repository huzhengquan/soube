(defproject soube "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [compojure "1.1.5"]
								 [de.ubercode.clostache/clostache "1.3.1"]
;								 [jopbox "0.1.0-SNAPSHOT"]
                 [org.clojure/java.jdbc "0.3.0-alpha4"]
                 [mysql/mysql-connector-java "5.1.25"]
                 [clj-oauth "1.4.0"]
                 [cheshire "5.0.1"]
;								 [clj-dropbox "1.2.1"]
;                 [hiccup "1.0.3"]
;                 [enlive "1.1.1"]
;                 [sandbar/sandbar "0.4.0-SNAPSHOT"]
								 ;[clj-oauth "1.4.0"]
;								 [clj-oauth "1.3.1-SNAPSHOT"]
;								 [clj-http "0.7.2"]
;								 [org.clojars.tavisrudd/clj-apache-http "2.3.2-SNAPSHOT"]

								 ]
  :plugins [[lein-ring "0.8.3"]]
  :ring {:handler soube.handler/app}
  :profiles
  {:dev {:dependencies [[ring-mock "0.1.3"]]}})
