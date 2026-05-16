# Contribuindo com o UNES

## Introdução

Obrigado pelo seu interesse em contribuir com o projeto! Todos os tipos de contribuição são bem vindos e valorizados — código, documentação, traduções, design, ideias e relatos de bug. Leia a seção relativa à sua contribuição antes de enviá-la; isso facilita a revisão e deixa a experiência mais legal para todos.

## Pedir ajuda

Se você tem alguma pergunta sobre o projeto, como usá-lo, ou só quer tirar uma dúvida:

- Abra uma issue em https://github.com/ForceTower/Melon/issues
- Coloque o máximo de contexto possível na pergunta.

A issue será marcada e alguém vai tentar responder.

## Reportar um erro

- Abra uma issue em https://github.com/ForceTower/Melon/issues
- Siga o modelo de bug report.
- Inclua plataforma (iOS / Android / landing), versão do app, e passos para reproduzir.

## Pedir uma função

Se o UNES não faz algo que você queria que ele fizesse:

- Abra uma issue em https://github.com/ForceTower/Melon/issues
- Siga o modelo de pedido de função.

O pedido será avaliado e talvez surjam mais perguntas sobre requisitos. Se for aceito, a issue recebe a label `feature-accepted` e pode ser implementada por qualquer pessoa do time ou da comunidade.

## Preparação do Projeto UNES [Melon]

O UNES virou um monorepo poliglota: iOS nativo, Android nativo, uma landing page e uma camada de lógica compartilhada em Kotlin Multiplatform. O backend mora num repositório privado separado e não é necessário para rodar os apps localmente — eles funcionam contra a API de produção/staging.

O fluxo de PR continua o mesmo: faça [fork do projeto](https://guides.github.com/activities/forking), clone o seu fork e abra um Pull Request contra a branch `development` quando estiver pronto.

### 1. Instale o `mise`

[`mise`](https://mise.jdx.dev/) gerencia as versões de `bun`, `gradle`, `java` e `license-plist` que o projeto precisa. Depois de instalar o `mise`, na raiz do repositório rode:

```sh
mise install
```

Isso baixa as versões corretas de todas as ferramentas. Não use `npm`, `yarn` ou `pnpm` — o projeto usa `bun` (e `bunx` no lugar de `npx`).

### 2. Instale as dependências Node

Na raiz do repo:

```sh
bun install
```

### 3. Escolha o que você quer mexer

Dependendo do app que você quer alterar, os próximos passos mudam:

#### Android (`apps/android`)

- Abra `apps/android` no Android Studio mais recente (estável).
- O Gradle composite build já integra `build-logic/` e `packages/shared-kmp/` automaticamente.
- Rode o app diretamente pelo Android Studio ou via `./gradlew :apps:android:app:installDebug` (na raiz do repo).

#### iOS (`apps/ios`)

- Você precisa de um Mac com Xcode atualizado.
- Abra `apps/ios` no Xcode e rode o app.
- O projeto compila com **Swift 6** e `SWIFT_STRICT_CONCURRENCY = complete`. Código que compila ainda pode quebrar em runtime se você violar isolamento de actor — escreva código seguro, não só código que satisfaz o compilador.

#### Landing (`apps/landing`)

- Site em Astro, publicado no Cloudflare Pages.
- Para rodar localmente, entre no workspace e use os scripts do `bun`:

  ```sh
  bun run --filter @melon/landing dev
  ```

#### Shared KMP (`packages/shared-kmp`)

- Lógica de negócio compartilhada entre iOS e Android (banco de dados, rede, features de domínio).
- Mudou algo aqui? Lembre-se que o codigo precisa ser compativel com ambas as plataformas.

## Estilo de código

- **TypeScript / Astro / JS**: `oxlint` (lint) e `oxfmt` (formatação). Rode `bun run fix` para aplicar lint + format automaticamente, ou `bun run check` para só verificar.
- **Kotlin (Android + KMP)**: siga o estilo do Kotlin oficial. Declare classes, funções e propriedades top-level como `internal` por padrão; só use `public` quando o símbolo for realmente consumido por outro módulo Gradle. `private` quando ficar dentro de um arquivo.
- **Swift (iOS)**: convenções padrão do Swift. `PascalCase.swift` para tipos.
- **Android — Design System**: nada de cores, tipografia ou animações hardcoded em código de feature. Tudo vem de `MaterialTheme.colorScheme.*`, `MaterialTheme.melon.*`, `MaterialTheme.typography.*` e `MelonMotion.*`. Se um token não existe, adicione ao design system primeiro. Strings de UI sempre via `stringResource(R.string.…)`.

Antes de abrir o PR, execute o linter correto para a linguagem: rode `bun run fix` para typescript para garantir que o lint/format está limpo.

## Mensagens de commit

- Mensagens claras e concisas.
- Não adicione co-autores automáticos (Claude ou qualquer outro bot) nos commits.
- Prefira commits pequenos e focados.

## Contribuindo com documentação

Documentação é importante. Se quiser melhorar o README, esse próprio guia, comentar código não óbvio, ou traduzir algo — manda bala.

Para contribuir com docs:

- [Prepare o projeto](#preparação-do-projeto-unes-melon)
- Faça as mudanças
- Abra um Pull Request

Se seu PR for aceito, seu nome aparece na lista de contribuidores dentro do aplicativo.

## Contribuindo com código

Algumas ideias para começar:

- Implementar uma feature marcada como `feature-accepted` nas issues
- Atualizar uma tela ou ajustar design para alinhar iOS e Android
- Corrigir bugs
- Adicionar/atualizar strings em `strings.xml` ou traduzir o app
- Migrar lógica duplicada entre iOS e Android para `shared-kmp`
- Melhorar a landing page

Para contribuir com código:

- [Prepare o projeto](#preparação-do-projeto-unes-melon)
- Faça as mudanças tentando manter o estilo do código ao redor
- Rode `bun run fix` (TS/Astro) e teste em ambos os apps quando a mudança for cross-platform
- Escreva mensagens de commit claras
- Abra um Pull Request contra `development`

Quando você enviar:

- O PR só é analisado se passar pelas verificações de CI.
- Se forem necessárias mudanças, você será notificado(a).
- Caso o PR seja recusado, é dada uma explicação. Sua consideração pelo projeto é bem vinda e será lembrada.
- Se aceito, o PR entra na `development` e seu nome aparece na lista de contribuidores do app.
- O novo código é distribuído quando sai uma release.
