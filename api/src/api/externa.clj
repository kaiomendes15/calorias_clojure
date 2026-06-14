(ns api.externa
  (:require [clj-http.client :as http]
            [cheshire.core :as json]))

(def api-key-ninja "OhqhxCvQUCwTUIAGayVMUiH0r00vvuhPS87T6vp5")
(def api-key-usda "yI0DOPxyU2td5z2x56R2YMJBce3MBfRPS8eq3JdA")



(defn- chamar [url params headers]
  (-> (http/get url {:query-params params
                     :headers headers}) ;; retorna a resposta da api
      (:body) ;; pega só o body
      (json/parse-string true)) ;; recebe uma string json e retorna um mapa clojure. o true significa pra converter em keyword em vez de string.
  )

(defn- quilo-para-libras [peso-em-quilo]
  (* peso-em-quilo 2.205))
(defn- libras-para-quilo [peso-em-libras]
  (/ peso-em-libras 2.205))

(defn calorias-alimento [descricao quantidade]
  (let [resposta (chamar "https://api.nal.usda.gov/fdc/v1/foods/search"
                         {"api_key" api-key-usda
                          "query" descricao
                          "dataType" ["Foundation" "SR Legacy"]
                          "pageSize" "1"} ;; params
                         {} ;; headers (vazio)
                         )
        nutrientes (-> resposta :foods first :foodNutrients)
        kcal-por-100g (->> nutrientes
                           (filter #(and (= "Energy" (:nutrientName %))
                                         (= "KCAL"   (:unitName %))))
                           (first)
                           :value)]
    (println "=== kcal por 100g: ===")
    (println kcal-por-100g)
    (println (* kcal-por-100g (/ quantidade 100.0)))
    (println "=== kcal por 100g: ===\n")
    (println "=== RESPOSTA USDA ===")
    (println resposta)
    (println "=== FIM ===")
    (* kcal-por-100g (/ quantidade 100.0))))

(defn calorias-atividade [atividade duracao peso]
  (let [resposta (chamar "https://api.api-ninjas.com/v1/caloriesburned"
                         {"activity" atividade
                          "duration" (str duracao)
                          "weight" (quilo-para-libras peso)} ;;params
                         {"X-Api-Key" api-key-ninja} ;;headers
                         )]
    (->> resposta (map :total_calories) (reduce + 0))))