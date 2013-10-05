(ns soube.page
  (:use [ring.util.response :only [redirect response]]
				[ring.middleware session params keyword-params])
  (:require [clostache.parser :as clostache]
            [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql]
						[soube.jopbox :as dropbox]
						;[cheshire.core :as cheshire]
            [clj-time [format :as timef] [coerce :as timec] [core :as timecore]]
						[soube.config :as config])
  (:import [java.net URLEncoder URLDecoder]))


(defn render-page
  "渲染页面"
  [hostname template data]
  (let [dir (config/get-siteid hostname)]
    (clostache/render-resource
      (str dir "/templates/" template ".mustache")
      data
      (reduce
        (fn [accum pt]
          (assoc accum pt (slurp
                            (clojure.java.io/resource
                                    (str dir "/templates/" (name pt) ".mustache")))))
        {}
        [:header :footer]))))


(defn view-index
  "首页，文章列表"
  [req]
  (let [table-name (config/get-tablename (:server-name req))
        p (Integer/parseInt (get (:params req) :p "1"))
        limit 5
        l (try
            (jdbc/query
              config/db-spec
              (sql/select [:date :title :id :account :html] table-name (sql/order-by {:date :desc}) (str "limit " limit " OFFSET " (* limit (dec p)))))
            (catch Exception e nil))
        tags (take 45 ((deref config/sort-tags) (config/get-siteid (:server-name req))))]
    (if (not l)
      (clostache/render-resource (str "templates/doc/install.mustache") {})
      (render-page
        (:server-name req)
        "index"
        {:site-name (:name (config/get-site-conf req))
         :site-desc (:description (config/get-site-conf req))
         :page-title "首页"
         :list (map #(merge % {:account (get config/allow-dropbox-map (:account  %) (:account  %))}) l)
         :p p
         :next (if (= limit (count l)) (inc p) false)
         :prev (if (= p 1) false (dec p))
         :tags (for [tag tags]
                 {:url (URLEncoder/encode tag "utf-8") :tag tag})
         :keywords (take 10 tags)}))))

(defn view-article
  "文章页"
  [req]
  ; (render-page "post" (first l))
  (let [table-name (config/get-tablename (:server-name req))
        id (:id (:params req))
        gettype (:type (:params req))
        l (jdbc/query
            config/db-spec
            (sql/select [:markdown :html :title :date :tags :account] table-name (sql/where {:id id})))
        thepost (first l)
        ]
    (if thepost
      (cond
        (= gettype "html")
          (render-page
            (:server-name req)
            "post"
            {:markdown (:html thepost)
             :site-name (:name (config/get-site-conf req))
             :post-date (:date thepost)
             :post-id id
             :post-account (get config/allow-dropbox-map (:account  thepost) (:account  thepost))
             :tags (if
                     (:tags thepost)
                     (map #(into {}  {:url (URLEncoder/encode % "utf-8") :tag %})
                          (filter #(not (= % ""))
                                  (map #(clojure.string/trim %)
                                       (clojure.string/split (:tags thepost) #",")))))
             :page-title (:title thepost)})
        (= gettype "md")
          (:markdown thepost)
        :else
          (response (str "404")))
      (render-page (:server-name req) "post" {:markdown "404"}))))

(defn view-tag
  "tag聚合页"
  [req]
  (let [page-tag (clojure.string/trim (URLDecoder/decode (:tag (:params req)) "utf-8"))
        hostname (:server-name req)
        tag-posts ((deref (config/tag-map (config/get-siteid hostname))) page-tag)
        postid-set (apply hash-set (map #(:id %) tag-posts))
        tags-set (disj (apply hash-set
                        (for [[tag post-list] (deref (config/tag-map (config/get-siteid hostname)))]
                          (if (> (count (clojure.set/intersection
                                          postid-set
                                          (apply hash-set (map #(:id %) post-list))))
                                 0)
                            tag)))
                       nil)
        sort-tags (filter #(contains? tags-set %) ((deref config/sort-tags) (config/get-siteid (:server-name req))))]
    (render-page (:server-name req)
                 "tag"
                 {:site-name (:name (config/get-site-conf req))
                  :page-title page-tag
                  :list (reverse tag-posts)
                  :tags (for [tag sort-tags]
                          {:url (URLEncoder/encode tag "utf-8") :tag tag})
                  :keywords (take 10 sort-tags)})))
(defn pro-robots
  "robots.txt"
  [req]
  (str "User-agent: *\n"
       "Allow:　/ \n"
       "\n"
       "Sitemap: http://" (:server-name req) "/sitemap.xml"))

(defn format-time [time]
  (timef/unparse (timef/formatter "yyyy-MM-dd") time))

(defn make-url-tag
  "get sitemap url tag"
  [loc lastmod priority changefreq]
  (str
    "<url>\n"
    "<loc>" loc "</loc>\n"
    "<lastmod>" lastmod "</lastmod>\n"
    "<priority>" priority "</priority>\n" 
    "<changefreq>" changefreq "</changefreq>\n"
    "</url>\n"))

(defn pro-sitemap
  "sitemap.xml"
  [req]
  {:status 200
   :headers {"Content-Type" "text/xml; charset=UTF-8"}
   :body
  (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
       "<urlset xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" 
                xsi:schemaLocation=\"http://www.sitemaps.org/schemas/sitemap/0.9 http://www.sitemaps.org/schemas/sitemap/0.9/sitemap.xsd\"
                xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n\n"
       (make-url-tag
         (str "http://" (:server-name req) "/")
         "2013"
         "1.0"
         "always")
       (apply
         str
         (for [[tag post-list] (deref (config/tag-map (config/get-siteid (:server-name req))))]
           (make-url-tag
             (str "http://" (:server-name req) "/tag/" (URLEncoder/encode tag))
             (format-time (timec/from-long (.getTime (:date (first post-list)))))
             (if (> (count post-list) 10)
               "1.0"
               (.format (java.text.DecimalFormat. "#.#") (/ (count post-list) 10)))
             (cond
               (< (count post-list) 2) "yearly"
               (< (count post-list) 4) "monthly"
               (< (count post-list) 8) "weekly"
               :else "daily"))))
       (apply
         str
         (for [post (apply hash-set (reduce #(concat %1 %2) [] (vals (deref (config/tag-map (config/get-siteid (:server-name req)))))))]
           (make-url-tag
             (str "http://" (:server-name req) "/post/" (:id post) ".html")
             (format-time (timec/from-long (.getTime (:date post))))
             "0.8"
             "yearly")))
       "\n</urlset>")})

(defn view-test 
  "test"
  [req]
  (str (map #(str %1 (deref %2)) config/tag-map))
  #_(let [t
        (reduce #(merge-with concat %1 %2) (for [row (jdbc/query config/db-spec (sql/select [:title :id :tags] "blogkurrunkcom_posts" ["tags is not NULL"]))]
          (reduce #(assoc %1 (first %2) [(nth %2 1)]) {} (for [tag (clojure.string/split (:tags row) #",")] [tag (select-keys row [:title :id])]))))]
    (pr-str t)))

