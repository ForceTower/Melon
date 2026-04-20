package dev.forcetower.melon.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.forcetower.melon.core.database.entity.CourseEntity
import dev.forcetower.melon.core.database.entity.StudentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Query("SELECT * FROM Student LIMIT 1")
    fun observeCurrent(): Flow<StudentEntity?>

    @Query("SELECT lastSyncCompletedAt FROM Student LIMIT 1")
    fun observeLastSyncCompletedAt(): Flow<String?>

    @Query("SELECT * FROM Student LIMIT 1")
    suspend fun getCurrent(): StudentEntity?

    @Upsert
    suspend fun upsertStudent(student: StudentEntity)

    @Upsert
    suspend fun upsertCourse(course: CourseEntity)

    @Query("SELECT * FROM Course WHERE id = :id")
    suspend fun getCourse(id: String): CourseEntity?

    @Query("DELETE FROM Student")
    suspend fun clearStudents()

    @Query("DELETE FROM Course")
    suspend fun clearCourses()
}
