(ns api.db)

(def estado
  (atom {:usuario nil :transacoes []}))

(defn salvar-usuario [dados]
  (swap! estado assoc :usuario dados)
  dados)

(defn obter-usuario []
  (:usuario @estado))

(defn salvar-transacao [transacao]
  (let [novo-id (inc (count (:transacoes @estado)))
        com-id (assoc transacao :id novo-id)]
    (swap! estado update :transacoes conj com-id)
    com-id))

(defn transacoes []
  (:transacoes @estado))