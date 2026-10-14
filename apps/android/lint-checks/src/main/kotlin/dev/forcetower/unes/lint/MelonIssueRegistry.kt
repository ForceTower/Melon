package dev.forcetower.unes.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API

class MelonIssueRegistry : IssueRegistry() {
    override val api = CURRENT_API
    override val minApi = CURRENT_API
    override val vendor = Vendor(
        vendorName = "Melon",
        identifier = "dev.forcetower.unes.lint",
        feedbackUrl = "https://github.com/ForceTower/Melon/issues",
    )
    override val issues = listOf(
        ThemeDetector.COLOR,
        ThemeDetector.TYPOGRAPHY,
        ThemeDetector.MOTION,
        StringResourceDetector.ISSUE,
        InternalVisibilityDetector.ISSUE,
    )
}
