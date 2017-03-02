(ns blog-datom.service
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [ring.util.response :as ring-resp]
            [io.pedestal.test :as test]
            [io.pedestal.interceptor :refer [interceptor]]
            [hiccup.core :as hc]
            #_[clojure.core.async :refer (<!!)]
            [datomic.api :as d]))

;;========================================================================

#_(def conn
  (<!! (client/connect
        {:db-name "firstdb"
         :account-id client/PRO_ACCOUNT
         :secret "mysecret"
         :region "none"
         :endpoint "localhost:8998"
         :service "peer-server"
         :access-key "myaccesskey"})))

(def uri
  "datomic:mem://firstdb")

(defn create-database []
  (d/create-database uri))

(defn conn []
  (d/connect uri))

(def schema
  [{:db/ident :post/id
    :db/valueType :db.type/uuid
    :db/unique :db.unique/identity
    :db/cardinality :db.cardinality/one}
   {:db/ident :post/title
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :post/content
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :post/time
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one}])

(defn insert-schema []
  (d/transact (conn) schema))

(defn create-post-database [title content]
  (d/transact
   (conn)
   [{:post/id (d/squuid)
     :post/title (str title)
     :post/content (str content)
     :post/time (new java.util.Date)}]))

(defn take-database []
  (d/q '[:find ?id ?title ?content ?time
              :where
              [?e :post/title ?title]
              [?e :post/id ?id]
              [?e :post/content ?content]
              [?e :post/time ?time]]
            (d/db (conn))))

(defn take-database-id []
  (d/q '[:find ?id
              :where
              [?e :post/id ?id]]
            (d/db (conn))))

(defn take-post-by-id [id]
  (d/q '[:find ?title ?content ?time
              :in $ ?id
              :where
              [?e :post/id ?id]
              [?e :post/title ?title]
              [?e :post/content ?content]
              [?e :post/time ?time]]
            (d/db (conn)) id))

(defn delete-post-database [id title content tm]
  (d/transact
   (conn)
   [[:db/retract [:post/id id]
     :post/id id
     :post/title title
     :post/content content
     :post/time tm]]))

(defn edit-post-database [id title content]
  (d/transact
   (conn)
   [[:db/add [:post/id id]
     :post/title title]
    [:db/add [:post/id id]
     :post/content content]]))

(defn take-database-time-id []
  (d/q '[:find ?time ?id
         :where
         [?e :post/time ?time]
         [?e :post/id ?id]]
       (d/db (conn))))

;;==========================================================================

(defn post-list [id]
  (for [time-id (sort (take-database-time-id))]
    [:div {:class "col-sm-4"}
     [:h5 [:small (str "Post ID: " (str (last time-id)))]]
     [:a {:href (str "/post/" (last time-id))} [:h3 (first (first (take-post-by-id (last time-id))))]]
     [:h5 [:small (str "Posted at " (first time-id))]]
     (second (first (take-post-by-id (last time-id))))[:br][:br]]))

(defn bootstrap []
  (for [cnt (range 4)]
    ([[:meta {:charset "utf-8"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
      [:link {:rel "stylesheet" :href "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css"}]
      [:script {:src "https://ajax.googleapis.com/ajax/libs/jquery/3.1.1/jquery.min.js"}]
      [:script {:src "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js"}]] cnt)))

(defn navpanel [active]
  [:nav {:class "navbar navbar-inverse"}
   [:div {:class "container-fluid"}
    [:div {:class "navbar-header"}
     [:a {:class "navbar-brand" :href "/"} "BlogWeb"]]
    [:ul {:class "nav navbar-nav"}
     (if (= active 1)
       [:li {:class "active"} [:a {:href "/"} "Home"]]
       [:li [:a {:href "/"} "Home"]])
     [:li [:a {:href "/"} "About"]]]
    [:ul {:class "nav navbar-nav navbar-right"}
     [:li [:a {:href "#"} [:span {:class "glyphicon glyphicon-user"}] " Sign Up"]]
     [:li [:a {:href "#"} [:span {:class "glyphicon glyphicon-log-in"}] " Login"]]]]])

(defn make-html [nav-val title [& content]]
  (hc/html
   [:html
    [:head
     [:title (str title)]
     (bootstrap)]
    [:body
     (navpanel nav-val)
     content]]))

;;===============================================================================

(defn home-content [id]
   [[:div {:class "jumbotron text-center"}
    [:h1 "Home Page"]
    [:p "Welcome to blog test page"]]
   [:div {:class "container"}
    (if (empty? id)
      [:div {:class "container-fluid"}
       [:div {:class "alert alert-info"}
        [:div [:strong "You have no post yet!"] " Click the new post button to create a new post"]]]
      [:div {:class "row"}
       (post-list id)])
    [:div {:class "text-center"}
     [:br][:br]
     [:a {:href "/new"}
      [:button {:class "btn btn-primary" :type "button"} "New Post"]]
     [:br][:br]]]])

(def new-post-content
  [[:div {:align "center"}
    [:h1 "Create New Post"]]
   [:div {:class "container"}
    [:form {:action "/ok" :method "post" :id "input-form"}
     [:div {:class "form-group"}
      [:label {:for "title"} "Title"]
      [:input {:type "text" :class "form-control" :id "title" :name "title" :required ""}]]
     [:div {:class "form-group"}
      [:label {:for "content"} "Content"]
      [:textarea {:class "form-control" :rows "20" :id "content" :name "content" :required ""}]]
     [:div {:class "text-center"}
      [:div {:class "btn-group"}
       [:a {:href "/" :class "btn btn-primary"} "Cancel"]
       [:button {:type "reset" :class "btn btn-primary"} "Reset"]
       [:button {:type "submit" :class "btn btn-primary"} "Submit"]]]]]])

(def post-ok-content
  [[:div {:class "container-fluid"}
    [:div {:class "alert alert-success"}
     [:strong "Congratulations!"] " Your post successfully created!"]
    [:div {:class "text-center"}
     [:div {:class "btn-group"}
      [:a {:href "/new" :class "btn btn-primary"} "New Post"]
      [:a {:href "/" :class "btn btn-primary"} "Home"]]]]])

(defn view-post-content [id title content tm]
  [[:div {:class "container"}
    [:h1 [:strong (str title)]]
    [:h5 [:small (str "Posted at " tm)]]
    [:h5 [:small (str "Post ID: " id)]]
    [:br]
    (str content)
    [:br][:br]
    [:div {:class "text-center"}
     [:div {:class "btn-group"}
      [:a {:href "/" :class "btn btn-primary"} "Home"]
      [:a {:href (str "/delete/" id) :class "btn btn-primary"} "Delete"]
      [:a {:href (str "/edit/" id) :class "btn btn-primary"} "Edit"]]]]])

(defn edit-post-content [id title content]
  [[:div {:align "center"}
    [:h1 "Edit Post"]]
   [:div {:class "container"}
    [:form {:action (str "/edit-ok/" id) :method "post" :id "input-form"}
     [:div {:class "form-group"}
      [:label {:for "title"} "Title"]
      [:input {:type "text" :class "form-control" :id "title" :name "title" :value (str title)}]]
     [:div {:class "form-group"}
      [:label {:for "content"} "Content"]
      [:textarea {:class "form-control" :rows "20" :id "content" :name "content"} (str content)]]
     [:div {:class "text-center"}
      [:div {:class "btn-group"}
       [:a {:href (str "/post/" id) :class "btn btn-primary"} "Cancel"]
       [:button {:type "reset" :class "btn btn-primary"} "Reset"]
       [:button {:type "submit" :class "btn btn-primary"} "Edit"]]]]]])

(def edit-ok-content
  [[:div {:class "container-fluid"}
    [:div {:class "alert alert-success"}
     [:strong "Congratulations!"] " Your post successfully edited!"]
    [:div {:class "text-center"}
     [:a {:href "/" :class "btn btn-primary"} "Home"]]]])

(defn delete-post-confirm-content [postid]
  [[:div {:class "container-fluid"}
    [:div {:class "alert alert-warning"} [:strong (str "You are about to delete this post!")]
     " Deleted post cannot be recovered, are you sure you delete this post?"]
    [:br][:br]
    [:div {:class "text-center"}
     [:form {:action (str "/delete-ok/" postid) :method "post"}
      [:a {:href (str "/post/" postid) :class "btn btn-default"} "Cancel"]
      "     "
      [:button {:type "submit" :class "btn btn-primary"} "Yes"]]]]])

(def delete-ok-content
  [[:div {:class "container-fluid"}
    [:div {:class "alert alert-success"} [:strong "Congratulations!"] " Your post successfully deleted!"]
    [:br][:br]
    [:div {:class "text-center"}
     [:a {:href "/" :class "btn btn-primary"} "Go to Home"]]]])

(defn home-html []
  (make-html
   1 "Home" (home-content (take-database-id))))

(def new-post-html
  (make-html
   0 "Create New Post" new-post-content))

(def post-ok-html
  (make-html
   0 "Post Successfully Created!" post-ok-content))

(defn view-post-html [id title content tm]
  (make-html
   0 title (view-post-content id title content tm)))

(defn edit-post-html [id title content]
  (make-html
   0 (str "Edit Post :: " title) (edit-post-content id title content)))

(def edit-ok-html
  (make-html
   0 "Post Successfully Edited!" edit-ok-content))

(defn delete-post-confirm-html [postid]
  (make-html
   0 "Delete Post COnfirmation" (delete-post-confirm-content postid)))

(def delete-ok-html
  (make-html
   0 "Post Successfully Deleted!" delete-ok-content))

;;====================================================================================================


(def home-page
  (interceptor
   {:name :home-page
    :enter
    (fn [context]
      (let [request (:request context)
            response {:status 200 :body (home-html)}]
        (assoc context :response response)))}))

(def new-post
  (interceptor
   {:name :new-post
    :enter
    (fn [context]
      (let [request (:request context)
            response {:status 200 :body new-post-html}]
        (assoc context :response response)))}))

(def new-post-ok
  (interceptor
   {:name :new-post-ok
    :enter
    (fn [context]
      (let [title (:title (:form-params (:request context)))
            content (:content (:form-params (:request context)))]
        (create-post-database title content)
        (assoc context :response {:status 200 :body post-ok-html})))}))

(def view-post
  (interceptor
   {:name :view-post
    :enter
    (fn [context]
      (let [postid (get-in context [:request :path-params :postid])
            id (if (= (count postid) 36)
                 (java.util.UUID/fromString (str postid))
                 false)
            title (first (first (take-post-by-id id)))
            content (second (first (take-post-by-id id)))
            tm (last (first (take-post-by-id id)))
            response {:status 200 :body (view-post-html id title content tm)}]
        (if (= id false)
          (assoc context :response {:status 404 :body "No Page Found"})
          (assoc context :response response))))}))

(def edit-post
  (interceptor
   {:name :edit-post
    :enter
    (fn [context]
      (let [postid (get-in context [:request :path-params :postid])
            id (if (= (count postid) 36)
                 (java.util.UUID/fromString (str postid))
                 false)
            title (first (first (take-post-by-id id)))
            content (second (first (take-post-by-id id)))]
        (if (= id false)
          (assoc context :response {:status 404 :body "No Page Found"})
          (assoc context :response {:status 200 :body (edit-post-html id title content)}))))}))

(def edit-post-ok
  (interceptor
   {:name :edit-post-ok
    :enter
    (fn [context]
      (let [postid (get-in context [:request :path-params :postid])
            id (java.util.UUID/fromString (str postid))
            title (:title (:form-params (:request context)))
            content (:content (:form-params (:request context)))]
        (edit-post-database id title content)
        (assoc context :response {:status 200 :body edit-ok-html})))}))

(def delete-post-confirm
  (interceptor
   {:name :delete-post-confirm
    :enter
    (fn [context]
      (let [postid (get-in context [:request :path-params :postid])
            id (if (= (count postid) 36)
                 postid
                 false)
            response {:status 200 :body (delete-post-confirm-html postid)}]
        (if (= id false)
          (assoc context :response {:status 404 :body "No Page Found"})
          (assoc context :response response))))}))

(def delete-post-ok
  (interceptor
   {:name :delete-post-ok
    :enter
    (fn [context]
      (let [postid (get-in context [:request :path-params :postid])
            id (java.util.UUID/fromString (str postid))
            title (first (first (take-post-by-id id)))
            content (second (first (take-post-by-id id)))
            tm (last (first (take-post-by-id id)))
            response {:status 200 :body delete-ok-html}]
        (delete-post-database id title content tm)
        (assoc context :response response)))}))


;; Defines "/" and "/about" routes with their associated :get handlers.
;; The interceptors defined after the verb map (e.g., {:get home-page}
;; apply to / and its children (/about).
(def common-interceptors [(body-params/body-params) http/html-body])

;; Tabular routes
(def routes #{["/" :get (conj common-interceptors home-page)]
              ["/new" :get (conj common-interceptors new-post)]
              ["/ok" :post (conj common-interceptors new-post-ok)]
              ["/post/:postid" :get (conj common-interceptors view-post)]
              ["/edit/:postid" :get (conj common-interceptors edit-post)]
              ["/edit-ok/:postid" :post (conj common-interceptors edit-post-ok)]
              ["/delete/:postid" :get (conj common-interceptors delete-post-confirm)]
              ["/delete-ok/:postid" :post (conj common-interceptors delete-post-ok)]})

;; Map-based routes
;(def routes `{"/" {:interceptors [(body-params/body-params) http/html-body]
;                   :get home-page
;                   "/about" {:get about-page}}})

;; Terse/Vector-based routes
;(def routes
;  `[[["/" {:get home-page}
;      ^:interceptors [(body-params/body-params) http/html-body]
;      ["/about" {:get about-page}]]]])


;; Consumed by blog-datom.server/create-server
;; See http/default-interceptors for additional options you can configure
(def service {:env :prod
              ;; You can bring your own non-default interceptors. Make
              ;; sure you include routing and set it up right for
              ;; dev-mode. If you do, many other keys for configuring
              ;; default interceptors will be ignored.
              ;; ::http/interceptors []
              ::http/routes routes

              ;; Uncomment next line to enable CORS support, add
              ;; string(s) specifying scheme, host and port for
              ;; allowed source(s):
              ;;
              ;; "http://localhost:8080"
              ;;
              ;;::http/allowed-origins ["scheme://host:port"]

              ;; Root for resource interceptor that is available by default.
              ::http/resource-path "/public"

              ;; Either :jetty, :immutant or :tomcat (see comments in project.clj)
              ::http/type :jetty
              ;;::http/host "localhost"
              ::http/port 8080
              ;; Options to pass to the container (Jetty)
              ::http/container-options {:h2c? true
                                        :h2? false
                                        ;:keystore "test/hp/keystore.jks"
                                        ;:key-password "password"
                                        ;:ssl-port 8443
                                        :ssl? false}})
