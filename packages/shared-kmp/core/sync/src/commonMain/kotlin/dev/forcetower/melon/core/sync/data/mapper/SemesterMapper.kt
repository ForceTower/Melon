package dev.forcetower.melon.core.sync.data.mapper

import dev.forcetower.melon.core.database.entity.ClassAllocationEntity
import dev.forcetower.melon.core.database.entity.ClassEntity
import dev.forcetower.melon.core.database.entity.ClassEvaluationEntity
import dev.forcetower.melon.core.database.entity.ClassSpaceEntity
import dev.forcetower.melon.core.database.entity.ClassTeacherEntity
import dev.forcetower.melon.core.database.entity.DisciplineEntity
import dev.forcetower.melon.core.database.entity.DisciplineOfferEntity
import dev.forcetower.melon.core.database.entity.SemesterEntity
import dev.forcetower.melon.core.database.entity.StudentClassEntity
import dev.forcetower.melon.core.database.entity.StudentGradeEntity
import dev.forcetower.melon.core.database.entity.TeacherEntity
import dev.forcetower.melon.core.sync.data.dto.AllocationDto
import dev.forcetower.melon.core.sync.data.dto.ClassDto
import dev.forcetower.melon.core.sync.data.dto.ClassTeacherDto
import dev.forcetower.melon.core.sync.data.dto.DisciplineDto
import dev.forcetower.melon.core.sync.data.dto.DisciplineOfferDto
import dev.forcetower.melon.core.sync.data.dto.EvaluationDto
import dev.forcetower.melon.core.sync.data.dto.SemesterDto
import dev.forcetower.melon.core.sync.data.dto.SemesterListItemDto
import dev.forcetower.melon.core.sync.data.dto.SpaceDto
import dev.forcetower.melon.core.sync.data.dto.StudentClassDto
import dev.forcetower.melon.core.sync.data.dto.StudentGradeDto
import dev.forcetower.melon.core.sync.data.dto.TeacherDto

internal fun SemesterDto.toEntity(): SemesterEntity =
    SemesterEntity(
        id = id,
        platformId = platformId,
        code = code,
        description = description,
        startDate = startDate,
        endDate = endDate,
        track = track,
    )

internal fun SemesterListItemDto.toEntity(): SemesterEntity =
    SemesterEntity(
        id = id,
        platformId = platformId,
        code = code,
        description = description,
        startDate = startDate,
        endDate = endDate,
        track = track,
    )

internal fun DisciplineDto.toEntity(): DisciplineEntity =
    DisciplineEntity(
        id = id,
        code = code,
        platformId = platformId,
        name = name,
        hours = hours,
        department = department,
        program = program,
    )

internal fun DisciplineOfferDto.toEntity(): DisciplineOfferEntity =
    DisciplineOfferEntity(
        id = id,
        disciplineId = disciplineId,
        semesterId = semesterId,
        platformId = platformId,
        hours = hours,
        program = program,
    )

internal fun ClassDto.toEntity(): ClassEntity =
    ClassEntity(
        id = id,
        offerId = offerId,
        platformId = platformId,
        groupName = groupName,
        type = type,
        hours = hours,
        program = program,
    )

internal fun TeacherDto.toEntity(): TeacherEntity =
    TeacherEntity(id = id, platformId = platformId, name = name)

internal fun ClassTeacherDto.toEntity(): ClassTeacherEntity =
    ClassTeacherEntity(classId = classId, teacherId = teacherId)

internal fun SpaceDto.toEntity(): ClassSpaceEntity =
    ClassSpaceEntity(
        id = id,
        platformId = platformId,
        type = type,
        campus = campus,
        location = location,
        modulo = modulo,
    )

internal fun AllocationDto.toEntity(): ClassAllocationEntity =
    ClassAllocationEntity(
        id = id,
        classId = classId,
        spaceId = spaceId,
        timePlatformId = timePlatformId,
        day = day,
        startTime = startTime,
        endTime = endTime,
    )

internal fun StudentClassDto.toEntity(): StudentClassEntity =
    StudentClassEntity(
        id = id,
        classId = classId,
        finalGrade = finalGrade,
        missedClasses = missedClasses,
        resultDescription = resultDescription,
        approved = approved,
        underRevision = underRevision,
        wentToFinals = wentToFinals,
        resultSyncedAt = resultSyncedAt,
    )

internal fun EvaluationDto.toEntity(): ClassEvaluationEntity =
    ClassEvaluationEntity(
        id = id,
        classId = classId,
        platformId = platformId,
        name = name,
        position = position,
    )

internal fun StudentGradeDto.toEntity(): StudentGradeEntity =
    StudentGradeEntity(
        id = id,
        studentClassId = studentClassId,
        evaluationId = evaluationId,
        platformId = platformId,
        name = name,
        nameShort = nameShort,
        ordinal = ordinal,
        weight = weight,
        value = value,
        date = date,
    )
