(ns soube.handler
  (:use [compojure.core]
				[ring.util.response :only [redirect]]
				[ring.middleware session params keyword-params])
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
;            [clj-http.client :as client]
						[soube.admin :as admin]
						[soube.page :as page]
						[soube.rss :as rss]
						[soube.config :as config])
  (:import [java.net URLEncoder]))

;(System/setProperty "org.mortbay.util.URI.charset" "utf-8")
;(System/setProperty "org.eclipse.jetty.util.URI.charset" "utf-8")

(defn wrap-auth
  "验证的逻辑"
	[handler]
	(fn [req]
		(if (and (.startsWith (:uri req) "/admin")
             (not (admin/authenticated req)))
          (admin/authenticate req)
          (handler req))))

(defn wrap-hostname
  "判断域名是否是别名"
	[handler]
	(fn
    [req]
    (if-let [goto-hostname (get config/hostname-map (:server-name req))]
      (merge
        (redirect (str (name (:scheme req)) "://" goto-hostname ":" (:server-port req) (:uri req)))
        {:status 301})
      (handler req))))

(defroutes app-routes
  (GET "/" [] page/view-index)
  (GET "/feed" [] rss/view-rss)
  (GET ["/tag/:tag", :tag #"[^/?&]+"] [] page/view-tag)
  (GET "/admin" [] admin/view-index)
  (GET "/admin/dashboard" [] admin/view-index)
  (GET "/admin/sync.json" [] admin/markdown-sync)
  (GET "/admin/info" [] admin/account-info)
  (GET "/admin/install" [] admin/init-table)
  (GET "/admin/tools" [] admin/view-tools)
  (GET "/admin/doc" [] admin/view-doc)
  #_(GET "/test" [] page/view-test)
;  (GET "/test_https" [] (test-https))
  (GET ["/post/:id.:type", :id  #"[0-9]+", :type #"(html|md)"] [] page/view-article)
  (GET ["/archives/:id", :id  #"[0-9]+"] [id] (#(merge (redirect (str "/post/" % ".html")) {:status 301}) id))
  (GET ["/archives/tag/:tag", :tag  #"[^/?&]+"] [tag] (#(merge (redirect (str "/tag/" %)) {:status 301}) tag))
  (route/resources "/")
  (route/not-found "Not Found"))

;(def app
;  (handler/site app-routes))

(def app
	(->
    app-routes
    wrap-hostname
		wrap-auth
		wrap-session
		wrap-keyword-params
		wrap-params))
