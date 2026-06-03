(ns api.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [cheshire.core :as json]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [compojure.core :refer :all]
            [clj-http.client :as http]))

(def api-key (System/getenv "API_KEY"))
(def base-url (System/getenv "BASE_URL"))

(defn get-foods-from-api []
  (let [response (http/get (str base-url "/foods/list") 
                           {:query-params {"api_key" api-key}})]
    (get response :body)))


(defroutes app-routes
  (GET "/" [] "Hello World")
  (GET "/saldo" [] {:headers {"Content-Type"
                              "application/json; charset=utf-8"}
                    :body (json/generate-string {:saldo 0})})
  (GET "/food" [] {:headers {"Content-Type"
                             "application/json; charset=utf-8"}
                   :body (json/generate-string (get-foods-from-api))})
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes api-defaults))
