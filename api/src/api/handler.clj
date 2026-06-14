(ns api.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [cheshire.core :as json]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-body]]
            [compojure.core :refer :all]
            [clojure.string :as str]
            [clj-http.client :as http]
            [api.db :as db]
            [api.externa :as externa]))

(defn como-json [conteudo & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/json; charset=utf-8"}
   :body (json/generate-string conteudo)})

(defn- no-periodo? [inicio fim transacao]
  (let [data (:data transacao)]
    (and (or (nil? inicio) (>= (compare data inicio) 0))
         (or (nil? fim)    (<= (compare data fim) 0)))))

(defn- nil-se-vazio [s]
  (when-not (str/blank? s) s))

(defn- filtrar-periodo [inicio fim]
  (filter (partial no-periodo? (nil-se-vazio inicio) (nil-se-vazio fim))
          (db/transacoes)))

(defn- calcular-saldo [transacoes]
  (reduce (fn [acumulador transacao] 
            (cond (= (:tipo transacao) "ganho") (+ acumulador (:calorias transacao))
                  :else (- acumulador (:calorias transacao))))
          0 
          transacoes))

(defn- peso-usuario []
  (-> (db/obter-usuario) :peso))

(defroutes app-routes
  (POST "/usuario" req 
    (como-json (db/salvar-usuario (:body req)) 201))
  (GET "/usuario" []
    (como-json (db/obter-usuario)))
  (POST "/alimentos" req
    (let [{:keys [descricao quantidade data]} (:body req) ;; destructure, pega a descricao, quantidade e data da requisicao que ta chegando e manda pra essas constantes
          _ (println "BODY RECEBIDO:" (:body req))
          _ (println "QUANTIDADE:" quantidade "TIPO:" (type quantidade))
          calorias (externa/calorias-alimento descricao quantidade)
          _ (println "CALORIAS CALCULADAS:" calorias)
          transacao {:tipo "ganho"
                     :descricao descricao
                     :quantidade quantidade
                     :data data
                     :calorias calorias}
          _ (println "TRANSACAO:" transacao)
          resultado (db/salvar-transacao transacao)
          _ (println "RESULTADO REGISTRO:" resultado)]
      (como-json resultado 201)))
  
  (POST "/atividades" req 
    (let [{:keys [descricao duracao data]} (:body req)
          peso (peso-usuario)
          calorias-gastas (externa/calorias-atividade descricao duracao peso)
          transacao {:tipo "perda"
                     :descricao descricao
                     :duracao duracao
                     :data data
                     :calorias calorias-gastas}]
      (como-json (db/salvar-transacao transacao) 201)))
  
  (GET "/extrato" req
    (let [{:keys [inicio fim]}  (:params req)] 
      (como-json {:transacoes (filtrar-periodo inicio fim)})))
  
  (GET "/saldo" req 
    (let [{:keys [inicio fim]} (:params req)]
      (como-json {:saldo (calcular-saldo (filtrar-periodo inicio fim))})))
  (route/not-found "Recurso não encontrado."))

(def app
  (-> (wrap-defaults app-routes api-defaults)
      (wrap-json-body {:keywords? true})))
