# Project-specific ProGuard / R8 rules.
#
# Libraries mostly ship their own consumer rules; add app-specific keep rules
# here as we encounter them.

# Play In-App Review. `review-ktx`'s generated `OnSuccessListener` SAM class
# carries a reference to a GMS annotation that no artifact on our classpath
# actually ships, and R8 fails the release build on the dangling reference.
# The annotation is compile-time only, so suppressing it is safe — this is the
# rule AGP itself writes into `missing_rules.txt`.
-dontwarn com.google.android.gms.common.annotation.NoNullnessRewrite
