
## Introdução
Obrigado pelo seu interesse em contribuir com o projeto!. Todos os tipos de contribuição são bem vindos e valorizados. Veja a [tabela de conteudos](#toc) para maneiras diferentes de ajudar e detalhes sobre como o projeto lida com cada uma delas!📝

Leia a seção relativa à sua contribuição antes de envia-la! Isso faz o trabalho da verificação da contribuição mais facil e deixa a experiencia mais legal para todos

## Pedir ajuda

Se você tem alguma pergunta sobre este projeto, como usa-lo, ou só quer tirar alguma dúvida sobre algo:

* Abra uma issue mp https://github.com/ForceTower/Melon/issues
* Coloque o máximo de contexto na sua pergunta.

Quando você terminar:

* A issue será marcada
* Alguem vai tentar responde-la

## Reportar um erro

Se você encontrar um erro no projeto:

* Abra uma issue no https://github.com/ForceTower/Melon/issues
* Siga o modelo de reportar erro :)

## Pedir uma função

Se o UNES não faz algo que você queria que ele fizesse:

* Abra uma issue no https://github.com/ForceTower/Melon/issues
* Siga o modelo de pedidos de funções

Quando você enviar:

* O pedido da função será avaliado, possivelmente perguntando mais perguntas sobre a criação e os requisitos. Se a issue for fechada, será sugerido um caminho alternativo para a atingir o resultado.
* Se o pedido for aceito, ele será marcado para implementação com `feature-accepted`, que poderá ser feito por qualquer um do time ou por qualquer um da comunidade que quiser [contribuir](#contribute-code).

## Preparação do Projeto UNES [Melon]

Então você quer contribuir com o UNES? Que demais! O UNES GitHub Pull Requests para gerenciar as contribuições, então [leia sobre como fazer um fork de um projeto do GitHub e como criar um PR](https://guides.github.com/activities/forking) se você nunca fez isso antes.

A partir de agora vamos demorar um tempo (ou não):
 1. [Instale o Android Studio](https://developer.android.com/studio/) e configure os passos iniciais (se você ainda não possui ele instalado)
 2. [Faça um Fork do UNES](https://guides.github.com/activities/forking/#fork), opcional, você pode fazer um clone do projeto diretamente e configurar o repositorio git depois
 3. [Faça um clone do seu fork](https://help.github.com/articles/cloning-a-repository/)
 4. [Abra o projeto no Android Studio](https://github.com/dogriffiths/HeadFirstAndroid/wiki/How-to-open-a-project-in-Android-Studio)
 5. [Espere muito tempo](https://www.youtube.com/watch?v=BdhGQMDjBSQ), se esta for a primeira vez abrindo ou criando um projeto, instale tudo que o Android Studio pedir
 6. [Fique calmo, espere mais um pouco](https://www.youtube.com/watch?v=dQw4w9WgXcQ)
 
O UNES utiliza o Firebase como backend, então temos uns passos adicionais...

* [Crie um projeto no Firebase](https://console.firebase.google.com/), você pode dar o nome que você quiser.
* Após criar o projeto você vai ser apresentado com uma tela onde ele fala para adicionar o firebase ao seu aplicativo. Como estamos num aplicativo nativo Android, selecione `Adicionar o Firebase ao seu aplicativo Android`.
* O primeiro passo é dizer o nome do pacote do aplicativo, no caso do UNES é: `com.forcetower.uefs`
* As informações do Certificado e Assinatura de debug podem ser deixadas em branco, ou siga as instruções mostradas no firebase para encontrar as suas.
* Após clicar em registrar app o próximo passo é baixar o `google-services.json`, baixe-o e mova-o para a pasta `app` do projeto.
* Pronto, você não precisa mais fazer qualquer alteração, se quiser, ative os recursos de autenticação com email e senha, o firestore, o storage e o functions. Estes são os 4 serviços principais para o aplicativo funcionar com todos os recursos, mas este passo não é obrigatório e a maioria dos recursos irão funcionar corretamente sem fazer este passo. Ah, se você quiser o código das functions do UNES elas estão [aqui](https://github.com/ForceTower/FireMelon).

Isso deve ser tudo, nem parece que demoramos 7 horas para fazer o projeto rodar.

Uma coisa importante, o projeto utiliza o [ktlint](https://ktlint.github.io/) para manter o estilo de código Kotlin, essa ferramenta irá manter tudo organizadinho =D

## Contribuindo com documentação
Documentação é importante para que entendamos o que estamos fazendo, o que fizemos e porque fizemos certas escolhas.
Se quiser comentar o código e/ou criar readme's pode começar!
Sinta-se livre para criar um PR mesmo que seu commit seja somente reordenar as palavras para que elas façam sentido.

Para contribuir:
* [Prepare o projeto](#project-setup)
* Faça as mudanças
* Abra um Pull Request

Se seu PR for aceito, seu nome irá aparecer na lista de contribuidores dentro do aplicativo o/

## Contribuindo com código
Contribuições com código tambem são bem vindas.
O projeto atualmente está misto entre Kotlin e Java, o uso de Kotlin é o mais desejado, mas Java tambem é aceito.

Você pode:
* Implementar uma feature marcada nas issues.
* Atualizar alguma tela, design
* Corrigir erros nas strings do aplicativo (strings.xml)
* Traduzir strings
* Corrigir bugs e erros
* Transferir uma classe Java para uma classe em Kotlin

Para contribuir com código:
* [Prepare o projeto](#project-setup)
* Faça as mudanças realizadas, tentando manter a identação utilizada ao redor do código
* Inclua comentários sobre o que foi feito na mudança
* Escreva mensagens de commits claras e concisas.
* O UNES utiliza o [ktlint](https://ktlint.github.io/) para manter o estilo de código do projeto, se você mandar o PR com o código fora do estilo, o CircleCI irá acusar um erro. Você pode verificar se o seu código está de acordo com o estilo executando o comando: `gradlew lintKotlin` e também pode corrigir automáticamente os erros de estilo com o comando: `gradlew formatKotlin`.

Quando você enviar:
* O PR somente será analisado se passar por todas as verificações (CircleCI)
* Caso sejam necessárias mudanças você será notificado(a).
* Caso seu PR seja recusado, será dada uma explicação do motivo que o levou a isso. Mas não fique triste, sua consideração pelo projeto é bem vinda e será lembrada
* Se seu PR for aceito, ele será incluido na branch `development` e seu nome irá aparecer na lista de contribuidores dentro do aplicativo
* O novo código será distribuído para todos quando uma release for feita. (Geralmente a cada 2 dias na versão beta)

