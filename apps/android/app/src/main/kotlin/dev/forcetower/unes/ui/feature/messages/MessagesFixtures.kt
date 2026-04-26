package dev.forcetower.unes.ui.feature.messages

import java.time.LocalDateTime

// Mirrors `MESSAGES` from `screens-messages-data.jsx` and the iOS
// `MessageFixtures`. Same ids and order so swapping to the real KMP feed
// later is a drop-in.
internal object MessagesFixtures {
    val messages: List<Message> = listOf(
        // ── Today
        Message(
            id = "m01",
            origin = MessageOrigin.Campus,
            sender = MessageSender("Reitoria UEFS", "Comunicado oficial"),
            body = """
                Prezadxs estudantes,

                Confirmamos o retorno das atividades presenciais na próxima quarta-feira, 22 de abril, após a semana de recesso do feriado de Tiradentes.

                A secretaria está disponível para tirar dúvidas sobre trancamento e matrícula extemporânea até sexta-feira.

                Atenciosamente,
                Reitoria UEFS
            """.trimIndent(),
            receivedAt = LocalDateTime.of(2026, 4, 18, 9, 14),
            unread = true,
            attachments = listOf(
                MessageAttachment(
                    kind = MessageAttachmentKind.Link,
                    title = "Calendário acadêmico 2026.1",
                    host = "uefs.br",
                    url = "uefs.br/calendario-2026-1",
                ),
            ),
        ),
        Message(
            id = "m02",
            origin = MessageOrigin.Discipline,
            disciplineCode = "ALGI",
            sender = MessageSender("Adriana Lima", "Algoritmos e Programação II"),
            body = """
                Pessoal,

                Subi o gabarito comentado da primeira avaliação no moodle. Qualquer dúvida, usem o fórum da disciplina ou me procurem no início da aula de segunda.

                A média da turma foi 6.2 — vamos revisar ponteiros duplos com calma na terça.

                Abs,
                Adriana
            """.trimIndent(),
            receivedAt = LocalDateTime.of(2026, 4, 18, 8, 2),
            unread = true,
            starred = true,
            attachments = listOf(
                MessageAttachment(MessageAttachmentKind.Pdf, name = "gabarito-av1-algi.pdf", size = "1.4 MB"),
            ),
        ),

        // ── Yesterday
        Message(
            id = "m03",
            origin = MessageOrigin.Direct,
            sender = MessageSender("Beatriz Sampaio", "Professora · Física II"),
            body = """
                Mariana,

                Notei que você não compareceu ao laboratório 03 ontem. Se foi alguma questão de saúde ou imprevisto, me avise em particular para que eu possa ajustar a nota de participação.

                Lembre que a recuperação prática acontece no final do semestre, mas prefiro não deixar acumular.

                Att,
                Beatriz
            """.trimIndent(),
            receivedAt = LocalDateTime.of(2026, 4, 17, 18, 42),
            unread = true,
        ),
        Message(
            id = "m04",
            origin = MessageOrigin.Module,
            moduleId = "intercambio",
            sender = MessageSender("Mobilidade Internacional", "Módulo Intercâmbio"),
            subject = "Edital 01/2026 — resultado da 1ª fase",
            preview = "O resultado da primeira fase do edital de mobilidade internacional foi publicado. 47 candidatos aprovados seguem para a fase 2.",
            body = """
                Prezadx candidatx,

                O resultado da primeira fase do Edital 01/2026 de Mobilidade Internacional foi publicado. Você pode consultar a lista nominal e as instruções para a segunda fase no link abaixo.

                Documentos para a fase 2 devem ser entregues até 30/04.
            """.trimIndent(),
            receivedAt = LocalDateTime.of(2026, 4, 17, 14, 7),
            starred = true,
            attachments = listOf(
                MessageAttachment(
                    kind = MessageAttachmentKind.Link,
                    title = "Resultado 1ª fase — Edital 01/2026",
                    host = "drive.google.com",
                    url = "drive.google.com/…/edital-01-2026.pdf",
                ),
            ),
        ),

        // ── This week
        Message(
            id = "m05",
            origin = MessageOrigin.Discipline,
            disciplineCode = "LPOO",
            sender = MessageSender("Carlos Mendes", "LPOO · Turma T01"),
            body = """
                Pessoal,

                Adicionei dois capítulos do Bloch (Effective Java) e o slide atualizado sobre mixins ao repositório da disciplina. Recomendo muito a leitura antes da aula de quinta.
            """.trimIndent(),
            receivedAt = LocalDateTime.of(2026, 4, 15, 11, 30),
            attachments = listOf(
                MessageAttachment(MessageAttachmentKind.Pdf, name = "effective-java-cap3.pdf", size = "2.1 MB"),
                MessageAttachment(MessageAttachmentKind.Pdf, name = "effective-java-cap4.pdf", size = "1.8 MB"),
                MessageAttachment(MessageAttachmentKind.Slides, name = "mixins-2026.pdf", size = "890 KB"),
            ),
        ),
        Message(
            id = "m06",
            origin = MessageOrigin.Secretariat,
            sender = MessageSender("Secretaria Acadêmica", "Renata da Silva Costa"),
            body = """
                Caros estudantes,

                Convidamos vocês para uma roda de conversa sobre Transtorno do Espectro Autista (TEA), organizada pelo núcleo de acessibilidade da UEFS.

                Data: 24 de abril, 14h.
                Local: Auditório Central.

                Inscrições até 22/04.
            """.trimIndent(),
            receivedAt = LocalDateTime.of(2026, 4, 14, 17, 17),
        ),
        Message(
            id = "m07",
            origin = MessageOrigin.App,
            sender = MessageSender("UNES", "Equipe do app"),
            subject = "Novidade: notas da prática e teórica separadas",
            preview = "A partir desta semana, disciplinas com múltiplas turmas mostram professores e notas separados por turma.",
            body = """
                Olá,

                A partir desta semana, disciplinas com múltiplas turmas (como Física II, que tem turma teórica e prática) mostram professores, notas e anexos separados por turma. Vocês podem filtrar pelo cabeçalho da disciplina.

                Como sempre, nos escrevam se encontrarem algo estranho — este app é feito por alunos da UEFS para alunos da UEFS.

                — UNES
            """.trimIndent(),
            receivedAt = LocalDateTime.of(2026, 4, 14, 10, 0),
            attachments = listOf(
                MessageAttachment(MessageAttachmentKind.Image, name = "preview-tudo-teorica-pratica.png"),
            ),
        ),

        // ── Earlier this month
        Message(
            id = "m08",
            origin = MessageOrigin.Discipline,
            disciplineCode = "CALC",
            sender = MessageSender("Renato Pessoa", "Cálculo III"),
            body = """
                Pessoal,

                Postei a lista 02 no moodle. Entrega individual via sistema até 23:59 de 25/04. Capítulos 4 e 5.

                Quem tiver dúvida, horário de atendimento continua o mesmo: quartas 14h–16h.
            """.trimIndent(),
            receivedAt = LocalDateTime.of(2026, 4, 10, 8, 45),
            attachments = listOf(
                MessageAttachment(MessageAttachmentKind.Pdf, name = "lista-02-calc3.pdf", size = "620 KB"),
            ),
        ),
        Message(
            id = "m09",
            origin = MessageOrigin.Module,
            moduleId = "biblioteca",
            sender = MessageSender("Biblioteca Central", "Módulo Biblioteca"),
            subject = "Renovação pendente: 3 livros",
            preview = "Você tem 3 livros com devolução até 20/04. Renove pelo app ou passe na biblioteca.",
            body = """
                Olá, Mariana.

                Você tem 3 livros emprestados com prazo de devolução até 20 de abril. Eles podem ser renovados pelo app ou presencialmente.

                - Clean Code (Martin)
                - Introduction to Algorithms (Cormen)
                - The Pragmatic Programmer (Hunt & Thomas)
            """.trimIndent(),
            receivedAt = LocalDateTime.of(2026, 4, 8, 9, 10),
        ),
        Message(
            id = "m10",
            origin = MessageOrigin.Campus,
            sender = MessageSender("Comunicação UEFS", "Campus wide"),
            body = """
                Estudantes,

                Devido à greve anunciada dos transportes municipais de Feira de Santana para o dia 15/04, as aulas presenciais serão opcionais. Os professores foram orientados a não aplicar avaliações.

                Acompanhem os canais oficiais para atualizações.
            """.trimIndent(),
            receivedAt = LocalDateTime.of(2026, 4, 7, 7, 6),
        ),
        Message(
            id = "m11",
            origin = MessageOrigin.Discipline,
            disciplineCode = "FIS2",
            sender = MessageSender("João Nascimento", "Física II · T01"),
            body = """
                Pessoal,

                Por motivos de saúde não teremos aula hoje, 03/04. A reposição será agendada na próxima semana — mandarei uma pesquisa de disponibilidade por aqui.

                Bom fim de semana,
                João
            """.trimIndent(),
            receivedAt = LocalDateTime.of(2026, 4, 3, 7, 6),
        ),

        // ── Older
        Message(
            id = "m12",
            origin = MessageOrigin.Discipline,
            disciplineCode = "LPOO",
            sender = MessageSender("Carlos Mendes", "LPOO · Turma T01"),
            body = """
                Pessoal,

                Classroom da disciplina foi criado. Entrem com o código 3qnn64a para não perderem os avisos paralelos ao UNES. Vou usar para compartilhar vídeos longos.
            """.trimIndent(),
            receivedAt = LocalDateTime.of(2026, 3, 11, 10, 0),
        ),
        Message(
            id = "m13",
            origin = MessageOrigin.App,
            sender = MessageSender("UNES", "Equipe do app"),
            subject = "Bem-vinda ao UNES",
            preview = "Oi Mariana! Seja bem-vinda. Se precisar de algo, nos mande uma mensagem — respondemos rápido.",
            body = """
                Oi, Mariana!

                Seja bem-vinda ao UNES. Se precisar de algo ou tiver sugestões, nos mande uma mensagem direto por aqui — respondemos rápido.

                Bons estudos!
                — Equipe UNES
            """.trimIndent(),
            receivedAt = LocalDateTime.of(2026, 3, 1, 11, 0),
        ),
    )
}
