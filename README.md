# \[ 🚧 🔧 Trabalho em progresso ⛏ 👷 \] UNES 2.0 
Reescrevendo o UNES utilizando o AndroidX, transformando partes do código para Kotlin e utilizando o Material Design 2.0
O aplicativo oficial pode ser encontrado na [PlayStore](https://play.google.com/store/apps/details?id=com.forcetower.uefs)

🛑 Versão altamente instável e com funções faltando 🛑

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/b7f7893bd3a64fb7bad733fa9d4b576f)](https://app.codacy.com/app/ForceTower/Melon?utm_source=github.com&utm_medium=referral&utm_content=ForceTower/Melon&utm_campaign=Badge_Grade_Dashboard)
[![CircleCI](https://circleci.com/gh/ForceTower/Melon.svg?style=svg)](https://circleci.com/gh/ForceTower/Melon/tree/development)

### Objetivos
* ~~Diminuir o tempo de carregamento do aplicativo quando está abrindo~~
* ~~Utilizar os novos componentes do Android~~
* ~~Usar Kotlin, já que parece que o Google vai adotar isso como a linguagem principal~~
* ~~Demanda Web direto no App~~ [Completo]
* ~~Corrigir o deadlock raro durante a sincronização~~ [Completo]
* Matrícula no App
* Corrigir o erro que afeta alguns estudantes que tem 2 cursos em 1 (????)
* ~~Utilizar o Material Design 2.0 para deixar as coisas bonitas OuO~~
* Integrar o UNES com algum backend para fazer backup de algumas informações para não sermos traídos pelo Portal

### Versão do Android
Esta nova versão não oferecerá suporte para dispositivos com Android inferior ao 5.0
Os dispositivos com versão antiga receberão updates para falhas críticas e talvez algumas funções interessantes que envolvam apenas os parsers.

Durante a fase de testes, o aplicativo será configurado para executar apenas no Android 6.0 ou acima por causa da compatibilidade com o JobScheduler e o bug conhecido na versão 5.x.x

Quando a fase de testes terminar, uma migração que envolva o 5.x.x será avaliada :)

### Compilando o Melon
Para um guia sobre como compilar o aplicativo, visite o [guia de contribuição](https://github.com/ForceTower/Melon/blob/development/CONTRIBUTING.md#preparação-do-projeto-unes-melon)

### Sobre o Aplicativo
Este aplicativo mostra notificações quando algo novo é detectado no Sagres.
Ele tambem tenta aproximar os conteúdos do Sagres em um aplicativo cujas ações podem ser feitas offline e então quando houver internet elas serão sincronizadas com o portal online. Também espera-se que possua algumas funcionalidades aleatórias que forem julgadas interessantes :)

### Aviso
Este aplicativo não é licenciado nem tem qualquer ligação com a Tecnotrends, a empresa que mantem o Website e o serviço Sagres da UEFS. O aplicativo filtra as informações disponibilizadas pelo portal do estudante e então exibe no aplicativo.
