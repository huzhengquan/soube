(ns soube.page
  (:use [ring.util.response :only [redirect response]]
				[ring.middleware session params keyword-params])
  (:require [clostache.parser :as clostache]
            [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql]
						[soube.jopbox :as dropbox]
						;[cheshire.core :as cheshire]
            [clj-time [format :as timef] [coerce :as timec]]
						[soube.config :as config]))

(defn render-page
    [template data]
    (clostache/render-resource
      (str "templates/" template ".mustache")
      data
      (reduce
        (fn [accum pt]
          (assoc accum pt (slurp
                            (clojure.java.io/resource
                                    (str "templates/" (name pt) ".mustache")))))
        {}
        [:header :footer])))

(defn view-index [req]
  (render-page "index" {:name "is正全"}))

(defn view-article [req]
  ; (render-page "post" (first l))
  (let [table-name (str ((config/account-dict (:server-name req)) :table-prefix) "posts")
        id (:id (:params req))
        l (jdbc/query
            config/mysql-db
            (sql/select [:html] table-name (sql/where {:id id})))
        html (:html (first l))]
    (if html
      (render-page "post" {:markdown html})
      (render-page "post" {:markdown "404"}))))

