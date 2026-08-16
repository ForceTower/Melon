package dev.forcetower.melon.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

// One edge of the curriculum graph: `entryCode` needs `requiresCode` first
// (`prerequisite`) or alongside it (`corequisite`). Kept as rows rather than a
// serialized list so the fluxograma's trail walk is a plain query and the
// grid stays a real relational mirror.
//
// The referenced code isn't a foreign key: the portal occasionally names a
// discipline that isn't itself a slot in this curriculum, and dropping the
// edge would silently understate what blocks a discipline.
@Entity(
    tableName = "CurriculumPrerequisite",
    primaryKeys = ["curriculumId", "entryCode", "requiresCode", "kind"],
    foreignKeys = [
        ForeignKey(
            entity = CurriculumEntity::class,
            parentColumns = ["id"],
            childColumns = ["curriculumId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("curriculumId"), Index("curriculumId", "entryCode")],
)
data class CurriculumPrerequisiteEntity(
    val curriculumId: String,
    val entryCode: String,
    val requiresCode: String,
    // prerequisite | corequisite
    val kind: String,
    val position: Int,
) {
    companion object {
        const val KIND_PREREQUISITE = "prerequisite"
        const val KIND_COREQUISITE = "corequisite"
    }
}
