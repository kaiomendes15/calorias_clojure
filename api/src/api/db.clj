(ns api.db)

(def estado
  (atom {:usuario nil :transacoes []}))

(defn salvar-usuario [dados]
  (swap! estado assoc :usuario dados)
  dados)

(defn obter-usuario []
  (:usuario @estado))

(defn salvar-transacao [transacao]
  (let [colecao-atualizada (swap! estado update :transacoes conj transacao)]))

(defn transacoes []
  (:transacoes @estado))