package com.forcetower.uefs.core.model.edge.sync

import com.google.gson.annotations.SerializedName
import java.time.ZonedDateTime

data class PublicDisciplineData(
    @SerializedName("semester")
    val semester: PublicSemesterData,
    @SerializedName("disciplines")
    val disciplines: List<PublicStudentDisciplineData>
)

data class PublicSemesterData(
    @SerializedName("platformId")
    val platformId: Long,
    @SerializedName("name")
    val name: String,
    @SerializedName("codename")
    val codename: String,
    @SerializedName("start")
    val start: ZonedDateTime?,
    @SerializedName("finish")
    val finish: ZonedDateTime?
)

data class PublicStudentDisciplineData(
    @SerializedName("sequence")
    val sequence: String,
    @SerializedName("creditsOverride")
    val creditsOverride: Int,
    @SerializedName("discipline")
    val discipline: PublicDiscipline,
    @SerializedName("grades")
    val grades: List<PublicGrade>
)

data class PublicDiscipline(
    @SerializedName("name")
    val name: String,
    @SerializedName("code")
    val code: String,
    @SerializedName("credits")
    val credits: Int,
    @SerializedName("department")
    val department: String?,
    @SerializedName("program")
    val program: String?
)

data class PublicGrade(
    @SerializedName("name")
    val name: String,
    @SerializedName("groupingName")
    val groupingName: String?,
    @SerializedName("date")
    val date: String?,
    @SerializedName("grade")
    val grade: Double?,
)