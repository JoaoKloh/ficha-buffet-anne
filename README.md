# Ficha Buffet — Buffet Anne

Sistema de gestão do Buffet Anne. Reúne as fichas técnicas dos pratos, a produção de cada evento, o cadastro de ingredientes, o controle financeiro e a precificação de orçamentos.

O projeto tem duas partes:

- **Frontend:** Next.js (App Router + TypeScript), em [`anne-fichas-tecnicas-react/anne-fichas-tecnicas`](anne-fichas-tecnicas-react/anne-fichas-tecnicas)
- **Backend:** API REST em Spring Boot com PostgreSQL, em [`ficha-buffet`](ficha-buffet)

## Funcionalidades

| Módulo | Página | O que faz |
| --- | --- | --- |
| Produção | `/` | Catálogo de pratos, filtrado por categoria, com foto e ficha técnica |
| Produção | `/producao` | Produção por evento: lista de cozinha e lista de compras consolidada |
| Produção | `/ingredientes` | Cadastro de ingredientes, associados aos pratos |
| Financeiro | `/financeiro` | Lançamentos de receitas e despesas, com resumo mensal e DRE |
| Orçamento | `/orcamento` | Precificação de eventos: cardápio, bebidas, equipe, louças, descartáveis e gelo |

Todas as páginas, exceto `/login`, pedem login.

## Arquitetura

```
Navegador ──► Next.js (porta 3000) ──► Spring Boot (porta 8080) ──► PostgreSQL
                │
                └──► Vercel Blob (upload das fotos dos pratos)
```

- O navegador fala só com o Next.js. As rotas em `app/api/**` repassam as chamadas ao backend Java (`lib/server/*Backend.ts`), junto com o cookie de sessão.
- O backend autentica com **JWT assinado por RSA**. O token fica num cookie `httpOnly` chamado `accessToken` e nunca passa pelo `localStorage`.
- O `middleware.ts` do Next.js manda para `/login` quem chega sem o cookie de sessão.
- As fotos dos pratos vão direto do navegador para o Vercel Blob. O servidor só emite o token temporário de upload.

## Stack

**Frontend:** Next.js 16, React 18, TypeScript, Zod, `@vercel/blob`

**Backend:** Java 21, Spring Boot 4, Spring Security (OAuth2 Resource Server/JWT), Spring Data JPA, PostgreSQL, Bucket4j (rate limiting), Lombok

**Infraestrutura:** Docker, Docker Compose, GitHub Actions, Docker Hub, VPS

## Estrutura do repositório

```
.
├── anne-fichas-tecnicas-react/anne-fichas-tecnicas/   # Frontend Next.js
│   ├── app/            # Páginas e rotas de API (proxy para o backend)
│   ├── components/     # Componentes de UI e de domínio
│   ├── lib/
│   │   ├── models/     # Tipos e schemas Zod
│   │   ├── api/        # Cliente HTTP usado pelos componentes
│   │   ├── server/     # Integração com o backend Java e autenticação
│   │   └── utils/      # Cálculos de produção, orçamento e financeiro
│   └── Dockerfile
├── ficha-buffet/                                       # Backend Spring Boot
│   ├── src/main/java/com/jbkloh/ficha_buffet/
│   │   ├── config/         # Segurança, CORS, rate limit, chaves RSA
│   │   ├── controller/     # Endpoints REST
│   │   ├── service/        # Regras de negócio
│   │   ├── model/          # Entidades JPA
│   │   ├── repositories/
│   │   ├── dtos/
│   │   └── exceptions/
│   ├── src/main/resources/keys/README.md   # Como gerar as chaves RSA
│   └── Dockerfile
├── docker-compose.prod.yaml                            # Stack de produção
└── .github/workflows/deploy.yml                        # CI/CD
```

## Rodando localmente

### Pré-requisitos

- Node.js 18.18 ou mais novo (o Docker usa o 22)
- Java 21 (o Maven Wrapper `mvnw` já vem no repositório)
- PostgreSQL 17, instalado na máquina ou via Docker
- OpenSSL, para gerar as chaves RSA

### 1. Banco de dados

O jeito mais rápido é subir o Postgres com Docker:

```bash
docker run -d --name ficha-db \
  -e POSTGRES_DB=ficha -e POSTGRES_USER=ficha -e POSTGRES_PASSWORD=ficha \
  -p 5432:5432 postgres:17-alpine
```

As tabelas são criadas pelo Hibernate (`ddl-auto=update`) quando o backend sobe.

### 2. Backend

Gere o par de chaves RSA que assina os JWTs. O passo a passo está em [`ficha-buffet/src/main/resources/keys/README.md`](ficha-buffet/src/main/resources/keys/README.md). A chave privada precisa estar no formato PKCS#8.

Crie o arquivo `ficha-buffet/.env`. Ele é lido automaticamente e fica fora do git:

```properties
URL_DB=jdbc:postgresql://localhost:5432/ficha
POSTGRES_USER=ficha
POSTGRES_PASSWORD=ficha
URL_FRONTEND=http://localhost:3000
COOKIE_SECURE=false
RSA_PUBLIC_KEY_PATH=src/main/resources/keys/public.pem
RSA_PRIVATE_KEY_PATH=src/main/resources/keys/private.pem
```

Suba a API:

```bash
cd ficha-buffet
./mvnw spring-boot:run      # http://localhost:8080
```

### 3. Primeiro usuário administrador

Com o backend no ar, cadastre a permissão de admin e depois o usuário:

```bash
curl -X POST http://localhost:8080/auth/inserirRoleAdmin

curl -X POST http://localhost:8080/auth/criarUsuarioAdmin \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "uma-senha-forte"}'
```

### 4. Frontend

Crie o arquivo `anne-fichas-tecnicas-react/anne-fichas-tecnicas/.env.local`:

```properties
API_BASE_URL=http://localhost:8080
BLOB_READ_WRITE_TOKEN=   # token do Vercel Blob, necessário só para o upload de fotos
```

Suba o app:

```bash
cd anne-fichas-tecnicas-react/anne-fichas-tecnicas
npm install
npm run dev                 # http://localhost:3000
```

Entre em `http://localhost:3000/login` com o usuário criado no passo 3.

## Comandos úteis

**Frontend** (em `anne-fichas-tecnicas-react/anne-fichas-tecnicas`):

```bash
npm run dev         # servidor de desenvolvimento
npm run build       # build de produção (saída standalone)
npm start           # roda o build de produção
npm run lint        # ESLint
npm run typecheck   # checagem de tipos (tsc --noEmit)
```

**Backend** (em `ficha-buffet`):

```bash
./mvnw spring-boot:run   # roda a API
./mvnw test              # testes (usam H2 em memória, sem precisar de Postgres)
./mvnw clean package     # gera o .jar em target/
```

## API do backend

Todos os endpoints, exceto os de `/auth/**`, exigem o cookie `accessToken`.

| Recurso | Endpoints |
| --- | --- |
| Autenticação | `POST /auth/login`, `POST /auth/logout`, `POST /auth/inserirRoleAdmin`, `POST /auth/criarUsuarioAdmin` |
| Pratos | `POST /prato/criar`, `POST /prato/criar-lote`, `PUT /prato/atualizar`, `GET /prato/retornarTodos`, `POST /prato/associarEventos`, `DELETE /prato/apagar/{id}` |
| Ingredientes | `POST /ingredientes/criar`, `PUT /ingredientes/atualizar`, `GET /ingredientes/retornarTodos`, `POST /ingredientes/associarPratos`, `DELETE /ingredientes/apagar/{id}` |
| Produção | `POST /producao/criar`, `PUT /producao/atualizar`, `GET /producao/retornarTodos`, `DELETE /producao/apagar/{id}` |
| Lançamentos | `POST /lancamentos/criar`, `PUT /lancamentos/atualizar`, `GET /lancamentos/retornarTodos`, `DELETE /lancamentos/apagar/{id}` |
| Orçamentos | `POST /orcamento/criar`, `GET /orcamento/retornarTodos`, `DELETE /orcamento/apagar/{id}` |

## Deploy

A cada push na branch `main`, o workflow [`.github/workflows/deploy.yml`](.github/workflows/deploy.yml) faz o seguinte:

1. Gera as imagens Docker do backend e do frontend.
2. Publica as duas no Docker Hub (`klohjoao/ficha-backend` e `klohjoao/ficha-frontend`).
3. Entra na VPS por SSH e roda `docker compose pull` e `docker compose up -d` com o arquivo de produção.

Secrets que o repositório precisa ter no GitHub: `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`, `VPS_HOST`, `VPS_USER` e `VPS_SSH_KEY`.

O [`docker-compose.prod.yaml`](docker-compose.prod.yaml) sobe o Postgres, o backend e o frontend. As portas ficam presas em `127.0.0.1`, então um proxy reverso com HTTPS precisa ficar na frente. Variáveis esperadas no `.env` da VPS:

| Variável | Uso |
| --- | --- |
| `DB_NAME`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | Credenciais do Postgres |
| `URL_DB` | JDBC URL, por exemplo `jdbc:postgresql://db:5432/<DB_NAME>` |
| `URL_FRONTEND` | Origem liberada no CORS do backend |
| `COOKIE_SECURE` | `true` em produção (HTTPS) |
| `BLOB_READ_WRITE_TOKEN` | Token do Vercel Blob para o upload de fotos |

As chaves RSA de produção são montadas como volume no container do backend. Os caminhos são indicados por `RSA_PUBLIC_KEY_PATH` e `RSA_PRIVATE_KEY_PATH`. Use um par de chaves só para produção.

## Segurança

- A sessão usa JWT assinado com RSA, guardado em cookie `httpOnly`, com `Secure` ligado em produção.
- O login tem rate limiting por IP (Bucket4j).
- As entradas são validadas com Zod no frontend e com Bean Validation no backend.
- O Next.js envia headers de segurança: CSP, `X-Frame-Options: DENY`, `nosniff`, `Referrer-Policy` e `Permissions-Policy`.
- Nenhum segredo é versionado: `.env` e `*.pem` estão no `.gitignore`.
