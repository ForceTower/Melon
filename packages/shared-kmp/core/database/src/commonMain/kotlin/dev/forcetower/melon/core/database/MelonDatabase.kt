package dev.forcetower.melon.core.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import dev.forcetower.melon.core.database.dao.AcademicDao
import dev.forcetower.melon.core.database.dao.MessageDao
import dev.forcetower.melon.core.database.dao.PendingMutationDao
import dev.forcetower.melon.core.database.dao.SemesterDao
import dev.forcetower.melon.core.database.dao.SettingsDao
import dev.forcetower.melon.core.database.dao.StudentDao
import dev.forcetower.melon.core.database.dao.SyncStateDao
import dev.forcetower.melon.core.database.dao.UserDao
import dev.forcetower.melon.core.database.entity.ClassAllocationEntity
import dev.forcetower.melon.core.database.entity.ClassEntity
import dev.forcetower.melon.core.database.entity.ClassEvaluationEntity
import dev.forcetower.melon.core.database.entity.ClassLectureEntity
import dev.forcetower.melon.core.database.entity.ClassSpaceEntity
import dev.forcetower.melon.core.database.entity.ClassTeacherEntity
import dev.forcetower.melon.core.database.entity.CourseEntity
import dev.forcetower.melon.core.database.entity.DisciplineEntity
import dev.forcetower.melon.core.database.entity.DisciplineOfferEntity
import dev.forcetower.melon.core.database.entity.LectureMaterialEntity
import dev.forcetower.melon.core.database.entity.MessageAttachmentEntity
import dev.forcetower.melon.core.database.entity.MessageEntity
import dev.forcetower.melon.core.database.entity.MessageScopeEntity
import dev.forcetower.melon.core.database.entity.MessageStateEntity
import dev.forcetower.melon.core.database.entity.PendingMutationEntity
import dev.forcetower.melon.core.database.entity.SemesterEntity
import dev.forcetower.melon.core.database.entity.SettingsEntity
import dev.forcetower.melon.core.database.entity.StudentClassEntity
import dev.forcetower.melon.core.database.entity.StudentEntity
import dev.forcetower.melon.core.database.entity.StudentGradeEntity
import dev.forcetower.melon.core.database.entity.SyncStateEntity
import dev.forcetower.melon.core.database.entity.TeacherEntity
import dev.forcetower.melon.core.database.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        StudentEntity::class,
        CourseEntity::class,
        SemesterEntity::class,
        DisciplineEntity::class,
        DisciplineOfferEntity::class,
        TeacherEntity::class,
        ClassSpaceEntity::class,
        ClassEntity::class,
        ClassTeacherEntity::class,
        ClassAllocationEntity::class,
        StudentClassEntity::class,
        ClassEvaluationEntity::class,
        StudentGradeEntity::class,
        ClassLectureEntity::class,
        LectureMaterialEntity::class,
        MessageEntity::class,
        MessageScopeEntity::class,
        MessageAttachmentEntity::class,
        MessageStateEntity::class,
        SettingsEntity::class,
        SyncStateEntity::class,
        PendingMutationEntity::class,
    ],
    version = 4,
)
@ConstructedBy(MelonDatabaseConstructor::class)
abstract class MelonDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun studentDao(): StudentDao
    abstract fun semesterDao(): SemesterDao
    abstract fun academicDao(): AcademicDao
    abstract fun messageDao(): MessageDao
    abstract fun settingsDao(): SettingsDao
    abstract fun syncStateDao(): SyncStateDao
    abstract fun pendingMutationDao(): PendingMutationDao
}

// Room KSP generates the actual per-target; the expect declaration lives here.
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object MelonDatabaseConstructor : RoomDatabaseConstructor<MelonDatabase> {
    override fun initialize(): MelonDatabase
}
