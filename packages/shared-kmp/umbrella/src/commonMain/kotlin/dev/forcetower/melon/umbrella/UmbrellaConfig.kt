package dev.forcetower.melon.umbrella

import dev.forcetower.melon.core.logging.LoggingConfig

data class UmbrellaConfig(
    val baseUrl: String,
    val logging: LoggingConfig = LoggingConfig(),
)
