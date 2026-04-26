package dev.forcetower.unes.ui.feature.messages

import dev.forcetower.melon.feature.messages.domain.model.MessageFeedAttachment
import dev.forcetower.melon.feature.messages.domain.model.MessageFeedAttachmentKind
import dev.forcetower.melon.feature.messages.domain.model.MessageFeedDetail
import dev.forcetower.melon.feature.messages.domain.model.MessageFeedItem
import dev.forcetower.melon.feature.messages.domain.model.MessageFeedOrigin
import dev.forcetower.melon.feature.messages.domain.model.MessageFeedSource

// KMP-shaped fixtures for `@Preview` blocks. Mirrors `screens-messages-data.jsx`
// and the iOS `MessageFixtures` ids/order so the preview canvas keeps rendering
// without Hilt/UmbrellaGraph wiring. Same attachments + timestamps end up
// landing in the same date buckets as iOS once the mapper runs.
//
// The KMP feed has no `MODULE` origin — the two "module" fixtures from the
// iOS bundle become app-origin entries here so the preview still exercises
// the subject + preview row layout.
internal object MessagesFixtures {
    val items: List<MessageFeedItem> = listOf(
        // ── Today
        item(
            id = "m01",
            origin = MessageFeedOrigin.CAMPUS,
            senderName = "Reitoria UEFS",
            content = """
                Prezadxs estudantes,

                Confirmamos o retorno das atividades presenciais na próxima quarta-feira, 22 de abril, após a semana de recesso do feriado de Tiradentes.

                A secretaria está disponível para tirar dúvidas sobre trancamento e matrícula extemporânea até sexta-feira.

                Atenciosamente,
                Reitoria UEFS
            """.trimIndent(),
            timestamp = "2026-04-18T09:14:00Z",
            isUnread = true,
            attachmentCount = 1,
        ),
        item(
            id = "m02",
            origin = MessageFeedOrigin.DISCIPLINE,
            disciplineCode = "ALGI",
            disciplineName = "Algoritmos e Programação II",
            senderName = "Adriana Lima",
            content = """
                Pessoal,

                Subi o gabarito comentado da primeira avaliação no moodle. Qualquer dúvida, usem o fórum da disciplina ou me procurem no início da aula de segunda.

                A média da turma foi 6.2 — vamos revisar ponteiros duplos com calma na terça.

                Abs,
                Adriana
            """.trimIndent(),
            timestamp = "2026-04-18T08:02:00Z",
            isUnread = true,
            isStarred = true,
            attachmentCount = 1,
        ),

        // ── Yesterday
        item(
            id = "m03",
            origin = MessageFeedOrigin.DIRECT,
            senderName = "Beatriz Sampaio",
            disciplineName = "Professora · Física II",
            content = """
                Mariana,

                Notei que você não compareceu ao laboratório 03 ontem. Se foi alguma questão de saúde ou imprevisto, me avise em particular para que eu possa ajustar a nota de participação.

                Lembre que a recuperação prática acontece no final do semestre, mas prefiro não deixar acumular.

                Att,
                Beatriz
            """.trimIndent(),
            timestamp = "2026-04-17T18:42:00Z",
            isUnread = true,
        ),
        item(
            id = "m04",
            origin = MessageFeedOrigin.APP,
            senderName = "Mobilidade Internacional",
            subject = "Edital 01/2026 — resultado da 1ª fase",
            content = """
                Prezadx candidatx,

                O resultado da primeira fase do Edital 01/2026 de Mobilidade Internacional foi publicado. Você pode consultar a lista nominal e as instruções para a segunda fase no link abaixo.

                Documentos para a fase 2 devem ser entregues até 30/04.
            """.trimIndent(),
            timestamp = "2026-04-17T14:07:00Z",
            isStarred = true,
            attachmentCount = 1,
        ),

        // ── This week
        item(
            id = "m05",
            origin = MessageFeedOrigin.DISCIPLINE,
            disciplineCode = "LPOO",
            disciplineName = "LPOO · Turma T01",
            senderName = "Carlos Mendes",
            content = """
                Pessoal,

                Adicionei dois capítulos do Bloch (Effective Java) e o slide atualizado sobre mixins ao repositório da disciplina. Recomendo muito a leitura antes da aula de quinta.
            """.trimIndent(),
            timestamp = "2026-04-15T11:30:00Z",
            attachmentCount = 3,
        ),
        item(
            id = "m06",
            origin = MessageFeedOrigin.SECRETARIAT,
            senderName = "Secretaria Acadêmica",
            content = """
                Caros estudantes,

                Convidamos vocês para uma roda de conversa sobre Transtorno do Espectro Autista (TEA), organizada pelo núcleo de acessibilidade da UEFS.

                Data: 24 de abril, 14h.
                Local: Auditório Central.

                Inscrições até 22/04.
            """.trimIndent(),
            timestamp = "2026-04-14T17:17:00Z",
        ),
        item(
            id = "m07",
            origin = MessageFeedOrigin.APP,
            senderName = "UNES",
            subject = "Novidade: notas da prática e teórica separadas",
            content = """
                Olá,

                A partir desta semana, disciplinas com múltiplas turmas (como Física II, que tem turma teórica e prática) mostram professores, notas e anexos separados por turma. Vocês podem filtrar pelo cabeçalho da disciplina.

                Como sempre, nos escrevam se encontrarem algo estranho — este app é feito por alunos da UEFS para alunos da UEFS.

                — UNES
            """.trimIndent(),
            timestamp = "2026-04-14T10:00:00Z",
            attachmentCount = 1,
            imageCount = 1,
        ),

        // ── Earlier this month
        item(
            id = "m08",
            origin = MessageFeedOrigin.DISCIPLINE,
            disciplineCode = "CALC",
            disciplineName = "Cálculo III",
            senderName = "Renato Pessoa",
            content = """
                Pessoal,

                Postei a lista 02 no moodle. Entrega individual via sistema até 23:59 de 25/04. Capítulos 4 e 5.

                Quem tiver dúvida, horário de atendimento continua o mesmo: quartas 14h–16h.
            """.trimIndent(),
            timestamp = "2026-04-10T08:45:00Z",
            attachmentCount = 1,
        ),
        item(
            id = "m09",
            origin = MessageFeedOrigin.APP,
            senderName = "Biblioteca Central",
            subject = "Renovação pendente: 3 livros",
            content = """
                Olá, Mariana.

                Você tem 3 livros emprestados com prazo de devolução até 20 de abril. Eles podem ser renovados pelo app ou presencialmente.

                - Clean Code (Martin)
                - Introduction to Algorithms (Cormen)
                - The Pragmatic Programmer (Hunt & Thomas)
            """.trimIndent(),
            timestamp = "2026-04-08T09:10:00Z",
        ),
        item(
            id = "m10",
            origin = MessageFeedOrigin.CAMPUS,
            senderName = "Comunicação UEFS",
            content = """
                Estudantes,

                Devido à greve anunciada dos transportes municipais de Feira de Santana para o dia 15/04, as aulas presenciais serão opcionais. Os professores foram orientados a não aplicar avaliações.

                Acompanhem os canais oficiais para atualizações.
            """.trimIndent(),
            timestamp = "2026-04-07T07:06:00Z",
        ),
        item(
            id = "m11",
            origin = MessageFeedOrigin.DISCIPLINE,
            disciplineCode = "FIS2",
            disciplineName = "Física II · T01",
            senderName = "João Nascimento",
            content = """
                Pessoal,

                Por motivos de saúde não teremos aula hoje, 03/04. A reposição será agendada na próxima semana — mandarei uma pesquisa de disponibilidade por aqui.

                Bom fim de semana,
                João
            """.trimIndent(),
            timestamp = "2026-04-03T07:06:00Z",
        ),

        // ── Older
        item(
            id = "m12",
            origin = MessageFeedOrigin.DISCIPLINE,
            disciplineCode = "LPOO",
            disciplineName = "LPOO · Turma T01",
            senderName = "Carlos Mendes",
            content = """
                Pessoal,

                Classroom da disciplina foi criado. Entrem com o código 3qnn64a para não perderem os avisos paralelos ao UNES. Vou usar para compartilhar vídeos longos.
            """.trimIndent(),
            timestamp = "2026-03-11T10:00:00Z",
        ),
        item(
            id = "m13",
            origin = MessageFeedOrigin.APP,
            senderName = "UNES",
            subject = "Bem-vinda ao UNES",
            content = """
                Oi, Mariana!

                Seja bem-vinda ao UNES. Se precisar de algo ou tiver sugestões, nos mande uma mensagem direto por aqui — respondemos rápido.

                Bons estudos!
                — Equipe UNES
            """.trimIndent(),
            timestamp = "2026-03-01T11:00:00Z",
        ),
    )

    val detailById: Map<String, MessageFeedDetail> = mapOf(
        "m02" to detail(
            base = items.first { it.id == "m02" },
            attachments = listOf(
                attachment("m02-a1", MessageFeedAttachmentKind.PDF, "gabarito-av1-algi.pdf"),
            ),
        ),
    )

    fun previewState(): MessagesUiState = MessagesUiState(
        rawItems = items,
        isLoading = false,
    )

    private fun item(
        id: String,
        origin: MessageFeedOrigin,
        senderName: String,
        content: String,
        timestamp: String,
        disciplineCode: String? = null,
        disciplineName: String? = null,
        subject: String? = null,
        isUnread: Boolean = false,
        isStarred: Boolean = false,
        attachmentCount: Int = 0,
        imageCount: Int = 0,
    ): MessageFeedItem = MessageFeedItem(
        id = id,
        source = MessageFeedSource.UPSTREAM,
        origin = origin,
        disciplineCode = disciplineCode,
        disciplineName = disciplineName,
        subject = subject,
        content = content,
        senderName = senderName,
        senderType = null,
        timestamp = timestamp,
        isUnread = isUnread,
        isStarred = isStarred,
        attachmentCount = attachmentCount,
        imageCount = imageCount,
    )

    private fun detail(
        base: MessageFeedItem,
        attachments: List<MessageFeedAttachment>,
    ): MessageFeedDetail = MessageFeedDetail(
        id = base.id,
        source = base.source,
        origin = base.origin,
        disciplineCode = base.disciplineCode,
        disciplineName = base.disciplineName,
        subject = base.subject,
        content = base.content,
        senderName = base.senderName,
        senderType = base.senderType,
        timestamp = base.timestamp,
        isUnread = base.isUnread,
        isStarred = base.isStarred,
        attachments = attachments,
    )

    private fun attachment(
        id: String,
        kind: MessageFeedAttachmentKind,
        name: String,
        url: String = "https://melon.example/$id",
        position: Int = 0,
    ): MessageFeedAttachment = MessageFeedAttachment(
        id = id,
        kind = kind,
        name = name,
        url = url,
        position = position,
    )
}
