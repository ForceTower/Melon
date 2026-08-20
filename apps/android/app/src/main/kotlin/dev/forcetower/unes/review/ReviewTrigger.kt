package dev.forcetower.unes.review

// `tag` is the analytics value and the lever allow-list token at once — a
// rename splits the funnel and drops a published allow-list entry.
internal enum class ReviewTrigger(val tag: String) {
    PositiveVerdict("positive_verdict"),
    GradeFromPush("grade_from_push"),
    MaterialUseful("material_useful"),
    ParadoxoDepth("paradoxo_depth"),
    ;

    companion object {
        fun fromTag(tag: String): ReviewTrigger? =
            entries.firstOrNull { it.tag.equals(tag.trim(), ignoreCase = true) }
    }
}
