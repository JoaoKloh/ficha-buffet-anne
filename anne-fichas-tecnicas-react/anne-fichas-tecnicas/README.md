# Anne — Fichas Técnicas & Produção

Catálogo de pratos e produção por evento do Buffet Anne, reescrito como aplicação
React/Next.js completa (App Router + TypeScript), com API própria e modelos de
dados tipados — a partir do protótipo original em HTML/JS puro.

## Stack

- **Next.js 16** (App Router, Route Handlers como API, Server Components para a
  primeira renderização)
- **React 18** + TypeScript estrito
- **Zod** para validação de entrada (cliente e servidor, a partir do mesmo schema)
- Backend de referência: repositório em arquivo JSON local (`lib/server/db.ts`),
  desenhado para ser trocado por um banco real sem tocar em rotas ou componentes

## Como rodar

```bash
npm install
npm run dev       # http://localhost:3000
```

Build de produção:

```bash
npm run build
npm start
```

Outros comandos:

```bash
npm run lint       # ESLint (flat config, v9)
npm run typecheck  # tsc --noEmit
```

### Variáveis de ambiente

Copie `.env.example` para `.env.local` e ajuste conforme o ambiente. A única
variável sensível é `API_SECRET_KEY` — usada para autenticar chamadas que alteram
dados (`POST`/`PATCH`/`DELETE`). **Nunca prefixe segredos com `NEXT_PUBLIC_`**:
qualquer variável com esse prefixo é incluída no bundle JavaScript enviado ao
navegador.

Sem `API_SECRET_KEY` configurada, o backend de referência aceita as escritas
sem checagem — conveniente para rodar localmente, mas **configure essa variável
antes de qualquer deploy real**, e troque a comparação simples de chave em
`lib/server/auth.ts` por validação de sessão (cookies `httpOnly`) ou JWT
assinado quando este projeto ganhar autenticação de usuário de verdade.

## Estrutura

```
app/
  page.tsx                 Catálogo (Server Component)
  producao/page.tsx        Lista de produções
  producao/[id]/page.tsx   Detalhe: lista de cozinha + lista de compras
  api/dishes/               Rotas REST de pratos
  api/productions/          Rotas REST de produções
components/                Componentes de UI e de domínio (Client Components)
lib/
  models/                  Tipos + schemas Zod (fonte única da verdade)
  api/                     Cliente HTTP tipado usado pelos componentes
  server/                  Repositório de dados, autenticação, seed do catálogo
  utils/                   Gerador de receitas, cálculo de lista de compras, formatação
data/                      Armazenamento do backend de referência (gitignored)
```

### Por que Server Components chamam o repositório direto, mas o cliente usa a API HTTP

As páginas (`app/page.tsx`, `app/producao/page.tsx`, `app/producao/[id]/page.tsx`)
são Server Components e leem o repositório diretamente para a primeira
renderização — evita um round-trip HTTP desnecessário nesse momento. Todas as
**mutações do usuário** (criar, editar, excluir pratos e produções), essas sim,
sempre passam pela API HTTP (`lib/api/*.ts` → `app/api/**/route.ts`), que é onde
a validação de entrada e a autenticação realmente vivem.

## Práticas de segurança aplicadas

- **Validação de entrada em toda rota de API** com Zod — nada do que o cliente
  envia é confiado sem checagem de tipo, tamanho e formato.
- **IDs gerados no servidor** com `crypto.randomUUID()` (não sequenciais, não
  adivinháveis) — e validados por regex antes de tocar o repositório, para
  rejeitar path traversal e entradas malformadas cedo.
- **Autenticação por API key com comparação resistente a timing attack**
  (`lib/server/auth.ts`) nas rotas que alteram dados; leitura (`GET`) é pública
  por padrão, já que é um catálogo interno sem dado sensível de terceiros.
- **Segredos nunca no bundle do cliente** — só `API_SECRET_KEY` existe, e vive
  exclusivamente em variável de ambiente do servidor.
- **URLs de foto restritas a `https://`** no schema Zod — nunca aceita
  `javascript:`/`data:` vindos do usuário.
- **Imagens de usuário carregadas via `<img>` simples, não `next/image`** —
  evita que o proxy de otimização de imagem do Next.js faça requisições
  server-side para URLs arbitrárias fornecidas pelo usuário (vetor clássico de
  SSRF quando `remotePatterns` é amplo demais).
- **Headers de segurança** (`next.config.js`): `X-Content-Type-Options`,
  `X-Frame-Options: DENY`, `Referrer-Policy`, `Permissions-Policy` restritiva, e
  uma **Content-Security-Policy** que só libera o essencial.
- **Erros nunca vazam detalhes internos** — mensagens genéricas ao cliente,
  stack traces só em `console.error` no servidor.
- **`poweredByHeader: false`** — não expõe a versão do framework.
- **Escrita em arquivo serializada** (`lib/server/db.ts`) para evitar corrupção
  de dados por escrita concorrente — limitação conhecida de um backend baseado
  em arquivo; um banco real resolve isso nativamente via transações.
- **Zero vulnerabilidades conhecidas nas dependências** — `npm audit` limpo.
  A versão inicial do Next.js escolhida tinha uma falha crítica corrigida em
  versão posterior; o projeto já sobe direto na versão corrigida (16.3.5).

## Trocando o backend de referência por um banco real

Toda a persistência passa por duas interfaces em `lib/server/db.ts`:
`dishRepository` e `productionRepository`. Para migrar para Postgres (via
Prisma, por exemplo), reimplemente essas duas exportações mantendo a mesma
assinatura de métodos (`list`, `getById`, `create`, `update`, `remove`) —
nenhuma rota de API nem componente precisa mudar.

## Testes manuais já executados neste ambiente

Antes da entrega, rodei de ponta a ponta neste ambiente (não é só código
gerado às cegas):

- `npm install` com zero vulnerabilidades (`npm audit`)
- `npx tsc --noEmit` sem erros
- `npx eslint .` sem erros nem avisos
- `npx next build` — build de produção completo, `.next` gerado de verdade
- Servidor de produção real (`next start`) respondendo:
  - catálogo carregando e semeando os 193 pratos automaticamente
  - CRUD completo de pratos (criar, ler, editar, excluir) via API
  - validação rejeitando payloads inválidos com erro 400 claro
  - ID malformado → 400; ID inexistente → 404
  - criação de produção referenciando pratos reais, com página de detalhe
    renderizando a lista de cozinha e a lista de compras consolidada
  - guarda de autenticação: sem chave → 401; chave errada → 401; chave certa
    → 201; leitura pública continua liberada

## O que fica para uma próxima etapa

- Testes automatizados (o projeto foi validado manualmente ponta a ponta, mas
  não tem suíte de testes ainda — Vitest + Testing Library seria a escolha
  natural aqui).
- Autenticação de usuário de verdade (sessão/JWT) no lugar da API key simples,
  se este app for exposto além da rede interna do negócio.
- Rate limiting nas rotas de API se o backend for exposto publicamente.
- Upload real de foto (hoje é só URL) — envolveria validação de tipo de
  arquivo e armazenamento em object storage (S3 ou equivalente), não no
  filesystem local.
