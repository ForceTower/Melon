# UNES - (UNES Não É Sagres)
![Preview-Screens](https://github.com/ForceTower/Melon/blob/development/screens.png)

[![Android Publisher CI](https://github.com/ForceTower/Melon/workflows/Android%20Publisher%20CI/badge.svg)](https://github.com/ForceTower/Melon/actions)

O aplicativo oficial pode ser encontrado na [PlayStore](https://play.google.com/store/apps/details?id=com.forcetower.uefs)

### Por que?
Acessar o portal para ver se algum professor mudou algo era muito chato, eu criei este aplicativo para automatizar esta tarefa.

O UNES é um aplicativo feito para notificar o estudante quando um professor posta uma nota ou um recado no Portal Sagres. 
Com o tempo, ele se tornou um pouco mais que isso, mas notificações e acesso offline às informações do portal são as funções principais do aplicativo.

Contribuições para o projeto são muito bem vindas e qualquer dúvida, erro, sugestão de feature ou melhoria de código basta colocar nas issues e tento resolver :v

### Está querendo ver o código antigo e ter nostalgias?
Você pode encontrar o UNES v0.0.1-alpha0 [neste repositório](https://github.com/ForceTower/Pineapple). Até onde sei, ele precisa de uma atualização nas constantes, mas ainda deve executar bem.

### Compilando o Melon
Para um guia detalhado sobre como compilar o aplicativo e testar você mesmo, visite o [guia de contribuição](https://github.com/ForceTower/Melon/blob/development/CONTRIBUTING.md#preparação-do-projeto-unes-melon)

### Sobre o aplicativo
O aplicativo é nativo Android, utiliza as bibliotecas do AndroidX e a maior parte do código está em Kotlin. Toda a interface é pensada para utilizar as guias do Material Design 2.0.

Como o projeto cresceu, toda a parte de comunicação com o Portal Sagres foi separada para o [Juice](https://github.com/ForceTower/Juice), que está publicado no Maven e é uma dependencia deste projeto.
O Juice é uma biblioteca sem dependência com a framework Android, logo, você pode executa-lo em seu computador e integra-lo facilmente em qualquer projeto Java/Kotlin.

O UNES tenta tirar vantagem das coisas novas que o Google está trazendo para os desenvolvedores, então você irá encontrar uso dos componentes Jetpack, Funções Dinâmicas e PlayCore, Dagger/Hilt para injeção de depencias e outras coisas.

### Quer entrar em contato?
Me mande um email joaopaulo761@gmail.com, ou me encontre no [LinkedIn](https://www.linkedin.com/in/forcetower/), [Facebook](https://www.facebook.com/ForceTower) ou [Instagram](https://www.instagram.com/joaopauloforce/).

### Suporte a outras universidades
Apesar do foco do aplicativo ser a Universidade Estadual de Feira de Santana (UEFS), se você quiser fazer um port para a sua universidade ou faculdade, sinta-se em casa.
Para adicionar suporte a outras universidades/faculdades basta adicionar os endereços base nas [constantes](https://github.com/ForceTower/Juice/blob/unsuspended/src/main/kotlin/com/forcetower/sagres/Constants.kt) do aplicativo.

### Adicionando uma mensagem nova ao login
Você tambem pode contribuir com o projeto colocando uma mensagem aleatória que será exibida durante o carregamento dos dados iniciais.
Você pode editar o arquivo [login_messages.json](https://github.com/ForceTower/Melon/blob/development/app/src/main/assets/login_messages.json) e mandar o seu PR :)

As suas mensagens serão avaliadas e se aprovadas elas poderão aparecer no aplicativo!

### Aviso
Este aplicativo não é licenciado nem tem qualquer ligação com a Tecnotrends, a empresa que mantem o Website e o serviço Sagres da UEFS. O aplicativo filtra as informações disponibilizadas pelo portal do estudante e então exibe no aplicativo.
