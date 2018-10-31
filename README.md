# \[ 🚧 🔧 Trabalho em progresso ⛏ 👷 \] UNES 2.0 
Reescrevendo o UNES utilizando o AndroidX, transformando partes do código para Kotlin e utilizando o Material Design 2.0
O aplicativo oficial pode ser encontrado na [PlayStore](https://play.google.com/store/apps/details?id=com.forcetower.uefs)

🛑 Versão altamente instável e com funções faltando 🛑

[![CircleCI](https://circleci.com/gh/ForceTower/Melon/tree/development.svg?style=shield)](https://circleci.com/gh/ForceTower/Melon/tree/development)

### Objetivos
* Diminuir o tempo de carregamento do aplicativo quando está abrindo
* Utilizar os novos componentes do Android
* Usar Kotlin, já que parece que o Google vai adotar isso como a linguagem principal
* ~~Demanda Web direto no App~~ [Completo]
* ~~Corrigir o deadlock raro durante a sincronização~~ [Completo]
* Corrigir o erro que afeta alguns estudantes que tem 2 cursos em 1 (????)
* Utilizar o Material Design 2.0 para deixar as coisas bonitas OuO
* Integrar o UNES com algum backend para fazer backup de algumas informações para não sermos traídos pelo Portal

### Versão do Android
Esta nova versão não oferecerá suporte para dispositivos com Android inferior ao 5.0
Os dispositivos com versão antiga receberão updates para falhas críticas e talvez algumas funções interessantes que envolvam apenas os parsers.

### Compilando o Melon
Para compilar o Melon você precisa seguir uns passos iniciais já que alguns arquivos não podem ser commitados no Git :)

* Se quiser que tudo funcione, crie um projeto no Firebase com qualquer nome e coloque o arquivo .json na pasta do módulo principal do aplicativo.
* Se você não quiser utilizar o lançamento automático no Google Play (provavelmente não quer), você pode apagar a seção "play" assim como o apply plugin: 'com.github.triplet.play' do build.gradle em nível de aplicativo. E apagar o classpath 'com.github.triplet.gradle:play-publisher:*' do build.gradle a nível de projeto.

* O Projeto Melon utiliza o ktlint para ser de acordo com as convenções do Kotlin, a utilização dele é opcional, mas se quiser fazer um pull request seria legal se o código tambem seguisse este padrão :)

### Sobre o Aplicativo
Este aplicativo mostra notificações quando algo novo é detectado no Sagres.
Ele tambem tenta aproximar os conteúdos do Sagres em um aplicativo cujas ações podem ser feitas offline e então quando houver internet elas serão sincronizadas com o portal online. Também espera-se que possua algumas funcionalidades aleatórias que forem julgadas interessantes :)

### Aviso
Este aplicativo não é licenciado nem tem qualquer ligação com a Tecnotrends, a empresa que mantem o Website e o serviço Sagres da UEFS. O aplicativo filtra as informações disponibilizadas pelo portal do estudante e então exibe no aplicativo.
