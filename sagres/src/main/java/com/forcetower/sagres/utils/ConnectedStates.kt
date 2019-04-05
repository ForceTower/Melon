package com.forcetower.sagres.utils

/**
 * Connected -> Login happened correctly (continue sync)
 * Session Timeout -> Another session or Sagres latency made the login fail (depends on will and reschedules)
 * Invalid -> Login is probably incorrect (stops sync)
 * Unknown -> Document could'nt be parsed (stops sync and reschedule)
 */
enum class ConnectedStates {
    CONNECTED, SESSION_TIMEOUT, INVALID, UNKNOWN
}