package dev.forcetower.melon.core.common

// Host-supplied application context. On Android this carries a real
// `android.content.Context` (used by Room/DataStore/Settings.Secure); on iOS
// and the JVM unit-test target it's an empty marker. Constructed by the host
// app and passed into `UmbrellaConfig`, after which Metro provides it as an
// `AppScope` binding to anyone in `androidMain` that needs Context.
expect class ApplicationContext
