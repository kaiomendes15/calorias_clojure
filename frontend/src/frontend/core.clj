(ns frontend.core
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clj-http.client :as http-client]
            [cheshire.core :as json])
  (:gen-class))

(def api-url "http://localhost:3000")

(def opcoes-do-programa
  [["-c" "--comando COMANDO" "cadastrar | usuario | alimento | atividade | extrato | saldo"]
   ["-a" "--altura ALTURA" "altura em cm" :default "0"]
   ["-p" "--peso PESO" "peso em kg" :default "0"]
   ["-i" "--idade IDADE" "idade" :default "0"]
   ["-s" "--sexo SEXO" "M ou F" :default ""]
   ["-d" "--descricao DESCRICAO" "alimento ou atividade" :default ""]
   ["-q" "--quantidade QUANTIDADE" "em gramas" :default "0"]
   ["-t" "--duracao DURACAO" "em minutos" :default "0"]
   ["-x" "--data DATA" "YYYY-MM-DD" :default ""]
   ["-y" "--inicio INICIO" "data inicial" :default nil]
   ["-z" "--fim FIM" "data final" :default nil]])

(defn- enviar-post [rota corpo]
  (-> (http-client/post (str api-url rota)
                        {:content-type :json
                         :body (json/generate-string corpo)})
      (:body)
      (json/parse-string true)))

(defn- enviar-get [rota params]
  (-> (http-client/get (str api-url rota) {:query-params params})
      (:body)
      (json/parse-string true)))

(defn- cadastrar [opcoes]
  (enviar-post "/usuario"
               {:altura (Integer/parseInt (:altura opcoes))
                :peso   (Integer/parseInt (:peso opcoes))
                :idade  (Integer/parseInt (:idade opcoes))
                :sexo   (:sexo opcoes)}))

(defn- consultar-usuario [_]
  (enviar-get "/usuario" {}))

(defn- registrar-alimento [opcoes]
  (enviar-post "/alimentos"
               {:descricao (:descricao opcoes)
                :quantidade (Integer/parseInt (:quantidade opcoes))
                :data (:data opcoes)}))

(defn- registrar-atividade [opcoes]
  (enviar-post "/atividades"
               {:descricao (:descricao opcoes)
                :duracao (Integer/parseInt (:duracao opcoes))
                :data (:data opcoes)}))

(defn- extrato [opcoes]
  (enviar-get "/extrato" {"inicio" (:inicio opcoes)
                          "fim"    (:fim opcoes)}))

(defn- saldo [opcoes]
  (enviar-get "/saldo" {"inicio" (:inicio opcoes)
                        "fim"    (:fim opcoes)}))

(def comandos
  {"cadastrar" cadastrar
   "usuario"   consultar-usuario
   "alimento"  registrar-alimento
   "atividade" registrar-atividade
   "extrato"   extrato
   "saldo"     saldo})

(defn -main [& args]
  (let [opcoes (:options (parse-opts args opcoes-do-programa))
        acao   (get comandos (:comando opcoes))]
    (prn (acao opcoes))))