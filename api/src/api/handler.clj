(ns api.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [cheshire.core :as json]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-body]]
            [compojure.core :refer :all]
            [clj-http.client :as http]
            [api.db :as db]
            [api.externa :as externa]))

(def api-key "OhqhxCvQUCwTUIAGayVMUiH0r00vvuhPS87T6vp5")
(def base-url "https://api.nal.usda.gov/fdc/v1")

(defn como-json [conteudo & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/json; charset=utf-8"}
   :body (json/generate-string conteudo)})

(defn- no-periodo? [inicio fim transacao]
  (let [data (:data transacao)]
    (and (>= (compare data inicio) 0) 
         (<= (compare data fim) 0))))

(defn- filtrar-periodo [inicio fim]
  (filter (partial no-periodo? inicio fim) (db/transacoes)))

(defn- calcular-saldo [transacoes]
  (reduce (fn [acumulador transacao] 
            (cond (= (:tipo transacao) "ganho") (+ acumulador (:calorias transacao))
                  :else (- acumulador (:calorias transacao))))
          0 
          transacoes))

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
  
  (POST "/atividade" req 
    (let [{:keys [descricao duracao data]} (:body req)
          calorias-gastas (externa/calorias-atividade descricao duracao)
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
