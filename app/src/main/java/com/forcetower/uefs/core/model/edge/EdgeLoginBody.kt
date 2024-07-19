package com.forcetower.uefs.core.model.edge

data class EdgeLoginBody(
    val username: String,
    val password: String,
    val provider: String = "SNOWPIERCER"
)