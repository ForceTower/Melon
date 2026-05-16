# Project-specific ProGuard / R8 rules.

# WorkManager reflectively instantiates `InputMerger` subclasses (notably
# `OverwritingInputMerger`, used by Glance to coalesce widget state updates)
# via their no-arg constructor. R8 strips the constructor under
# proguard-android-optimize.txt because no app code calls it directly, which
# crashes the WorkerWrapper and leaves the home-screen widget stuck on its
# initial loading layout.
-keep class * extends androidx.work.InputMerger {
    <init>();
}
