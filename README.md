# UNES - (UNES Não É Sagres)
![Preview-Screens](https://github.com/ForceTower/Melon/blob/development/repoimages/screens.png)

O aplicativo oficial pode ser encontrado na [PlayStore](https://play.google.com/store/apps/details?id=com.forcetower.uefs).

### Por que?
Acessar o portal para ver se algum professor mudou algo era muito chato, eu criei este aplicativo para automatizar esta tarefa.

O UNES é um aplicativo feito para notificar o estudante quando um professor posta uma nota ou um recado no Portal Sagres. Com o tempo, ele se tornou um pouco mais que isso, mas notificações e acesso offline às informações do portal continuam sendo as funções principais do aplicativo.

Contribuições para o projeto são muito bem vindas e qualquer dúvida, erro, sugestão de feature ou melhoria de código basta colocar nas issues e tento resolver :v

### Está querendo ver o código antigo e ter nostalgias?
Você pode encontrar o UNES v0.0.1-alpha0 [neste repositório](https://github.com/ForceTower/Pineapple). Até onde sei, ele precisa de uma atualização nas constantes, mas ainda deve executar bem.

## Estrutura do repositório

Este repositório é a parte cliente do UNES — um monorepo poliglota que reúne os apps nativos, a landing page e a lógica compartilhada. O backend mora num repositório privado separado.

- **`apps/ios`** — App nativo iOS (Swift + SwiftUI).
- **`apps/android`** — App nativo Android (Kotlin + Jetpack Compose).
- **`apps/landing`** — Landing page (Astro, publicada no Cloudflare Pages).
- **`packages/shared-kmp`** — Lógica de negócio compartilhada via Kotlin Multiplatform, empacotada como XCFramework para iOS e como biblioteca para Android.
- **`build-logic/`** — Convention plugins do Gradle usados pelos projetos JVM.

## Ferramentas

- [`mise`](https://mise.jdx.dev/) gerencia versões de ferramentas (`bun`, `gradle`, `java`, `license-plist`). Rode `mise install` uma vez.
- `bun install` para dependências Node (usamos `bun`, não `npm`/`yarn`/`pnpm`).
- `./gradlew` para o lado JVM/Android; o composite build do Gradle integra `build-logic/` e `packages/shared-kmp/`.
- iOS é um projeto Xcode padrão (`apps/ios`). O build script do Xcode já reconstrói o XCFramework do umbrella KMP automaticamente como parte do build do app — não precisa rodar nenhum passo manual antes.
- `oxlint` para lint e `oxfmt` para formatação. Use `bun run fix` para aplicar ambos.

### Compilando o Melon
Para um guia detalhado sobre como compilar o aplicativo e testar você mesmo, visite o [guia de contribuição](https://github.com/ForceTower/Melon/blob/development/CONTRIBUTING.md#preparação-do-projeto-unes-melon).

## Sobre o aplicativo

O UNES começou como um app Android nativo em Kotlin com Material Design e foi crescendo até virar este monorepo cross-platform. A interface Android usa Jetpack Compose, a iOS usa SwiftUI, e a lógica que faz sentido compartilhar (banco de dados, rede, features de domínio) vive em `packages/shared-kmp` via Kotlin Multiplatform.

A comunicação original com o Portal Sagres foi separada para o [Juice](https://github.com/ForceTower/Juice), publicado no Maven e usado como dependência. O Juice não depende do framework Android, então você pode executá-lo em qualquer projeto Java/Kotlin.

## Suporte a outras universidades

Apesar do foco do aplicativo ser a Universidade Estadual de Feira de Santana (UEFS), se você quiser fazer um port para a sua universidade ou faculdade, sinta-se em casa. Para adicionar suporte a outras universidades/faculdades basta adicionar os endereços base nas [constantes](https://github.com/ForceTower/Juice/blob/unsuspended/src/main/kotlin/com/forcetower/sagres/Constants.kt) do aplicativo.

## Aviso

Este aplicativo não é licenciado nem tem qualquer ligação com a Tecnotrends, a empresa que mantém o website e o serviço Sagres da UEFS. O aplicativo filtra as informações disponibilizadas pelo portal do estudante e então exibe no aplicativo.

## Quer entrar em contato?

Me mande um email joaopaulo761@gmail.com, ou me encontre no [LinkedIn](https://www.linkedin.com/in/forcetower/) ou [Instagram](https://www.instagram.com/joaopauloforce/).

## Licença

Veja [`LICENSE`](./LICENSE).
