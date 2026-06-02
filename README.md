# calorias_clojure

Monorepo Clojure com duas aplicações independentes:

- **`/api`** — backend criado com `lein new compojure api` (Ring + Compojure).
- **`/frontend`** — aplicação criada com `lein new app frontend`.

Cada projeto tem seu próprio `project.clj` e pode ser executado de forma isolada.

## API (`/api`)

Backend HTTP com Ring/Compojure e o plugin `lein-ring`.

```bash
cd api
lein ring server          # sobe o servidor e abre o navegador
lein ring server-headless # sobe o servidor sem abrir o navegador
```

Por padrão escuta em `http://localhost:3000`. Para usar outra porta:

```bash
lein ring server-headless 8080
```

Testes:

```bash
cd api
lein test
```

## Frontend (`/frontend`)

```bash
cd frontend
lein run        # imprime "Hello, World!"
```

Empacotar em uber-jar:

```bash
cd frontend
lein uberjar
java -jar target/uberjar/frontend-0.1.0-SNAPSHOT-standalone.jar
```

Testes:

```bash
cd frontend
lein test
```
