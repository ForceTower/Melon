## UNES

O UNES é um aplicativo feito para notificar o estudante quando um professor posta uma nota ou um recado no Portal Sagres. Com o tempo, ele se tornou um pouco mais que isso, mas esta é a função principal e pela qual o aplicativo vive.

O UNES é nativo Android, utiliza as bibliotecas do AndroidX e a maior parte do código está em Kotlin. Toda a interface é pensada para utilizar as guias do Material Design 2.0

O aplicativo oficial pode ser encontrado na [PlayStore](https://play.google.com/store/apps/details?id=com.forcetower.uefs)

Apesar do foco do aplicativo ser a Universidade Estadual de Feira de Santana (UEFS), se você quiser fazer um port para a sua universidade ou faculdade, sinta-se em casa.

Contribuições para o projeto são muito bem vindas, qualquer dúvida, erro ou sugestão basta colocar nas issues e tento resolver :v

[![CircleCI](https://circleci.com/gh/ForceTower/Melon.svg?style=svg)](https://circleci.com/gh/ForceTower/Melon)

### Objetivos
* ~~Diminuir o tempo de carregamento do aplicativo quando está abrindo~~
* ~~Utilizar os novos componentes do Android~~
* ~~Usar Kotlin, já que parece que o Google vai adotar isso como a linguagem principal~~
* ~~Demanda Web direto no App~~ [Completo]
* ~~Corrigir o deadlock raro durante a sincronização~~ [Completo]
* Matrícula no App [||                 ] 6% (Sob análise, estrutura de dados estranha para parse)
* ~~Corrigir o erro que afeta alguns estudantes que mudaram de modalidade durante a graduação [Licenciatura/Bacharelado]~~
* ~~Utilizar o Material Design 2.0 para deixar as coisas bonitas OuO~~
* ~~Integrar o UNES com algum backend para fazer backup de algumas informações para não sermos traídos pelo Portal~~
* ~~Tema escuro/preto porque tema dark é legal em todo lugar~~

### Adicionando uma mensagem nova ao login
Você tambem pode contribuir com o projeto colocando uma mensagem aleatória que será exibida durante o carregamento de coisas.
Você pode editar o arquivo [login_messages.json](https://github.com/ForceTower/Melon/blob/development/app/src/main/assets/login_messages.json) e mandar o seu PR :)

As suas mensagens serão avaliadas e se aprovadas elas poderão aparecer no aplicativo!

### Versão do Android
Esta nova versão não oferecerá suporte para dispositivos com Android inferior ao 5.0
Os dispositivos com versão antiga receberão updates para falhas críticas e talvez algumas funções interessantes que envolvam apenas os parsers.

### Compilando o Melon
Para um guia sobre como compilar o aplicativo, visite o [guia de contribuição](https://github.com/ForceTower/Melon/blob/development/CONTRIBUTING.md#preparação-do-projeto-unes-melon)

### Sobre o Aplicativo
Este aplicativo mostra notificações quando algo novo é detectado no Sagres.
Ele tambem tenta aproximar os conteúdos do Sagres em um aplicativo cujas ações podem ser feitas offline e então quando houver internet elas serão sincronizadas com o portal online. Também espera-se que possua algumas funcionalidades aleatórias que forem julgadas interessantes :)

### Aviso
Este aplicativo não é licenciado nem tem qualquer ligação com a Tecnotrends, a empresa que mantem o Website e o serviço Sagres da UEFS. O aplicativo filtra as informações disponibilizadas pelo portal do estudante e então exibe no aplicativo.
