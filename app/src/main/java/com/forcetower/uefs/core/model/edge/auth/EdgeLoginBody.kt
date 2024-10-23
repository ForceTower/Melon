package com.forcetower.uefs.core.model.edge.auth

data class EdgeLoginBody(
    val username: String,
    val password: String,
    val provider: String = "SNOWPIERCER"
)
