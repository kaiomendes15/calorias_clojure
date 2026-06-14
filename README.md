# calorias_clojure

Trabalho da disciplina de **Programação Funcional**

Aplicação de controle de balanço calórico desenvolvida em **Clojure**, organizada como um *monorepo* com duas aplicações independentes que se comunicam via HTTP:

- **`/api`** — backend HTTP (Ring + Compojure) que expõe os endpoints e concentra a lógica de negócio.
- **`/frontend`** — aplicação de linha de comando (CLI) que consome a API a partir do terminal.

Cada projeto possui seu próprio `project.clj` e pode ser executado de forma isolada.

---

## 1. Contexto da aplicação

A aplicação funciona como um **"extrato bancário de calorias"**. A ideia central é tratar a alimentação e a atividade física como transações financeiras:

- **Alimentos consumidos** são **ganhos** de calorias (entradas, *créditos*).
- **Atividades físicas** são **perdas** de calorias (gastos, *débitos*).
- O **saldo** é a soma dos ganhos menos as perdas em um determinado período.

O usuário cadastra seus dados (altura, peso, idade e sexo), registra o que comeu e quais atividades praticou, e a aplicação calcula automaticamente as calorias de cada registro consultando **APIs externas** de nutrição e de gasto calórico. Por fim, é possível consultar o **extrato** (lista de transações) e o **saldo** calórico, opcionalmente filtrando por um intervalo de datas.

### Conceitos de Programação Funcional aplicados

O projeto explora os pilares do paradigma funcional:

- **Imutabilidade e estado controlado:** o estado da aplicação fica isolado em um único `atom` (`api/src/api/db.clj`), e toda modificação acontece via `swap!`, de forma controlada.
- **Funções puras:** a lógica de negócio (cálculo de saldo, filtragem por período) é composta por funções puras que não dependem de efeitos colaterais — recebem dados e retornam dados.
- **Funções de ordem superior:** uso extensivo de `map`, `filter`, `reduce` e `partial` para transformar coleções de transações.
- **Composição de funções:** uso da macro *threading* (`->` e `->>`) para encadear transformações de dados de forma legível.
- **Destructuring:** extração declarativa de dados das requisições e dos mapas de resposta das APIs.

---

## 2. APIs externas consumidas

A lógica de cálculo está em `api/src/api/externa.clj`. São consumidas duas APIs públicas:

| API | Função | Uso na aplicação | Endpoint |
|-----|--------|------------------|----------|
| **USDA FoodData Central** | `calorias-alimento` | Calcula as calorias de um alimento a partir da descrição e da quantidade (em gramas). | `https://api.nal.usda.gov/fdc/v1/foods/search` |
| **API Ninjas – Calories Burned** | `calorias-atividade` | Calcula as calorias gastas em uma atividade física a partir da descrição, duração (minutos) e peso do usuário. | `https://api.api-ninjas.com/v1/caloriesburned` |

### Detalhes do cálculo

- **Alimentos (USDA):** a API retorna o valor de energia (`Energy` em `KCAL`) **por 100g**. A aplicação localiza esse nutriente na resposta e faz a regra de três para a quantidade informada:
  `calorias = kcal_por_100g * (quantidade / 100)`.
- **Atividades (API Ninjas):** a API espera o peso em **libras**, então a aplicação converte o peso do usuário de quilos para libras (`peso * 2,205`) antes de enviar. O retorno pode conter várias atividades; a aplicação soma o `total_calories` de todas elas via `reduce`.

> **Observação:** as chaves de API estão fixas no código (`externa.clj`) apenas para fins didáticos deste trabalho. Em um cenário real, deveriam ficar em variáveis de ambiente.

---

## 3. Como rodar a aplicação

A aplicação tem duas partes que rodam ao mesmo tempo, em **dois terminais diferentes**: primeiro o backend, depois o frontend.

### 3.1. Backend (`/api`)

Em um terminal, suba o servidor HTTP. Por padrão ele escuta em **`http://localhost:3000`**:

```bash
cd api
lein ring server-headless   # sobe o servidor sem abrir o navegador
```

Para usar outra porta:

```bash
lein ring server-headless 8080
```

Deixe esse terminal aberto e rodando.

### 3.2. Frontend (`/frontend`)

Em **outro terminal**, execute os comandos do CLI. Cada comando faz uma requisição para o backend (que precisa estar no ar) e imprime a resposta no terminal:

```bash
cd frontend
lein run -- -c <comando> [opções]
```

> **Importante:** o estado é mantido **em memória** no backend (ver seção 5). Ele é perdido ao reiniciar o servidor, então execute toda a sequência (cadastro → registros → extrato/saldo) sem reiniciar o backend.

### Opções (flags) disponíveis no CLI

| Flag curta | Flag longa | Descrição | Default |
|------------|------------|-----------|---------|
| `-c` | `--comando` | `cadastrar \| usuario \| alimento \| atividade \| extrato \| saldo` | — |
| `-a` | `--altura` | altura em cm | `0` |
| `-p` | `--peso` | peso em kg | `0` |
| `-i` | `--idade` | idade | `0` |
| `-s` | `--sexo` | `M` ou `F` | `""` |
| `-d` | `--descricao` | alimento ou atividade | `""` |
| `-q` | `--quantidade` | em gramas | `0` |
| `-t` | `--duracao` | em minutos | `0` |
| `-x` | `--data` | `YYYY-MM-DD` | `""` |
| `-y` | `--inicio` | data inicial (extrato/saldo) | `nil` |
| `-z` | `--fim` | data final (extrato/saldo) | `nil` |

---

## 4. Comandos do frontend (por rota)

Cada comando do CLI corresponde a uma rota da API. Abaixo, para cada um, estão o comando a executar no terminal do frontend, o corpo/parâmetros enviados ao backend e a resposta esperada.

### 4.1. `cadastrar` → `POST /usuario`

Cadastra os dados do usuário. É **pré-requisito** para registrar atividades, pois o cálculo de calorias gastas depende do peso.

```bash
lein run -- -c cadastrar -a 180 -p 80 -i 25 -s M
```

- **Corpo enviado ao backend:**

  | Campo | Origem (flag) | Tipo | Descrição |
  |-------|---------------|------|-----------|
  | `altura` | `-a` | inteiro | altura em centímetros |
  | `peso` | `-p` | inteiro | peso em quilogramas |
  | `idade` | `-i` | inteiro | idade em anos |
  | `sexo` | `-s` | string | `"M"` ou `"F"` |

- **Resposta esperada no terminal** (o usuário cadastrado):

```clojure
{:altura 180, :peso 80, :idade 25, :sexo "M"}
```

### 4.2. `usuario` → `GET /usuario`

Consulta o usuário cadastrado. Não envia nenhum dado.

```bash
lein run -- -c usuario
```

- **Resposta esperada no terminal:**

```clojure
{:altura 180, :peso 80, :idade 25, :sexo "M"}
```

Se nenhum usuário foi cadastrado ainda, retorna `nil`.

### 4.3. `alimento` → `POST /alimentos`

Registra um alimento consumido. O backend consulta a API da USDA, calcula as calorias e salva uma transação do tipo `"ganho"`.

```bash
lein run -- -c alimento -d banana -q 150 -x 2026-06-14
```

- **Corpo enviado ao backend:**

  | Campo | Origem (flag) | Tipo | Descrição |
  |-------|---------------|------|-----------|
  | `descricao` | `-d` | string | nome do alimento (em inglês, conforme a USDA) |
  | `quantidade` | `-q` | inteiro | quantidade em gramas |
  | `data` | `-x` | string | data no formato `YYYY-MM-DD` |

- **Resposta esperada no terminal** (a transação registrada, com `id` e `calorias` calculadas):

```clojure
{:tipo "ganho",
 :descricao "banana",
 :quantidade 150,
 :data "2026-06-14",
 :calorias 133.5,
 :id 1}
```

### 4.4. `atividade` → `POST /atividades`

Registra uma atividade física. O backend consulta a API Ninjas usando o **peso do usuário cadastrado**, calcula as calorias gastas e salva uma transação do tipo `"perda"`.

```bash
lein run -- -c atividade -d running -t 30 -x 2026-06-14
```

- **Corpo enviado ao backend:**

  | Campo | Origem (flag) | Tipo | Descrição |
  |-------|---------------|------|-----------|
  | `descricao` | `-d` | string | nome da atividade (em inglês, conforme a API Ninjas) |
  | `duracao` | `-t` | inteiro | duração em minutos |
  | `data` | `-x` | string | data no formato `YYYY-MM-DD` |

- **Resposta esperada no terminal:**

```clojure
{:tipo "perda",
 :descricao "running",
 :duracao 30,
 :data "2026-06-14",
 :calorias 285.0,
 :id 2}
```

### 4.5. `extrato` → `GET /extrato`

Lista as transações registradas. O filtro por período (`-y` e `-z`) é **opcional**: sem ele, todas as transações são retornadas.

```bash
# Todas as transações
lein run -- -c extrato

# Filtrando por período
lein run -- -c extrato -y 2026-06-01 -z 2026-06-30
```

- **Parâmetros enviados ao backend (opcionais):**

  | Parâmetro | Origem (flag) | Descrição |
  |-----------|---------------|-----------|
  | `inicio` | `-y` | data inicial do período (inclusive), `YYYY-MM-DD` |
  | `fim` | `-z` | data final do período (inclusive), `YYYY-MM-DD` |

- **Resposta esperada no terminal:**

```clojure
{:transacoes
 [{:tipo "ganho", :descricao "banana", :quantidade 150, :data "2026-06-14", :calorias 133.5, :id 1}
  {:tipo "perda", :descricao "running", :duracao 30, :data "2026-06-14", :calorias 285.0, :id 2}]}
```

### 4.6. `saldo` → `GET /saldo`

Calcula o saldo calórico do período: **soma dos ganhos menos a soma das perdas**. Aceita o mesmo filtro opcional (`-y` e `-z`).

```bash
# Saldo geral
lein run -- -c saldo

# Saldo do período
lein run -- -c saldo -y 2026-06-01 -z 2026-06-30
```

- **Parâmetros enviados ao backend (opcionais):** idênticos ao `extrato` (`inicio` e `fim`).
- **Resposta esperada no terminal:**

```clojure
{:saldo -151.5}
```

### Fluxo recomendado de teste ponta a ponta

1. `cadastrar` — cadastra o usuário (necessário antes de atividades).
2. `usuario` — confirma que os dados foram persistidos.
3. `alimento` — registra um ganho de calorias.
4. `atividade` — registra uma perda de calorias (depende do peso do usuário).
5. `extrato` — lista as transações registradas.
6. `saldo` — confere o cálculo `ganhos - perdas`.

---

## 5. Persistência (estado)

O estado é mantido **em memória**, em um único `atom` definido em `api/src/api/db.clj`:

```clojure
(def estado (atom {:usuario nil :transacoes []}))
```

- O usuário é guardado em `:usuario`.
- As transações ficam em `:transacoes`, e cada nova transação recebe um `id` sequencial.

> **Importante:** por ser em memória, **o estado é perdido ao reiniciar o servidor**. Execute toda a sequência de testes (cadastro → registros → extrato/saldo) sem reiniciar o backend.

---

## 6. Estrutura do projeto

```
calorias_clojure/
├── api/
│   └── src/api/
│       ├── handler.clj   # rotas, middlewares e lógica de saldo/extrato
│       ├── db.clj        # estado da aplicação (atom em memória)
│       └── externa.clj   # integração com as APIs externas (USDA e API Ninjas)
└── frontend/
    └── src/frontend/
        └── core.clj      # CLI que consome a API
```
