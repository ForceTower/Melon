package com.forcetower.core.lifecycle

sealed class LiveEventConfig {
    /**
     * Supports multi-observers on all cases the same.
     */
    data object Normal : LiveEventConfig()

    /**
     * Prefer the first observer when user emit the event, register observer, then the `onStart`
     * get called. In this case the _first observer_ will receive the _last event_.
     *
     * This scenario is specially useful when you want to emit the event in the `init` of
     * `ViewModel`, and expect the first observer receive it after `onStart`.
     */
    data object PreferFirstObserver : LiveEventConfig()
}
