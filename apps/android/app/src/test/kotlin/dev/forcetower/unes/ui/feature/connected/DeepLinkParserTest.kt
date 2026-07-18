package dev.forcetower.unes.ui.feature.connected

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeepLinkParserTest {

    @Test
    fun `tab hosts resolve to their tabs`() {
        assertEquals(DeepLinkTarget.Tab(ConnectedTab.Overview), parseDeepLink("unes://home"))
        assertEquals(DeepLinkTarget.Tab(ConnectedTab.Schedule), parseDeepLink("unes://schedule"))
        assertEquals(DeepLinkTarget.Tab(ConnectedTab.Classes), parseDeepLink("unes://classes"))
        assertEquals(DeepLinkTarget.Tab(ConnectedTab.Messages), parseDeepLink("unes://messages"))
        assertEquals(DeepLinkTarget.Tab(ConnectedTab.Me), parseDeepLink("unes://me"))
    }

    @Test
    fun `scheme and host are case-insensitive, ids are not`() {
        assertEquals(DeepLinkTarget.Tab(ConnectedTab.Messages), parseDeepLink("UNES://Messages"))
        assertEquals(DeepLinkTarget.Message("AbC"), parseDeepLink("unes://messages/AbC"))
    }

    @Test
    fun `trailing slash still resolves the tab`() {
        assertEquals(DeepLinkTarget.Tab(ConnectedTab.Messages), parseDeepLink("unes://messages/"))
    }

    @Test
    fun `message detail carries the id`() {
        assertEquals(
            DeepLinkTarget.Message("0d4e9a52-77aa-4e2e-9646-c7a5599e6d2b"),
            parseDeepLink("unes://messages/0d4e9a52-77aa-4e2e-9646-c7a5599e6d2b"),
        )
    }

    @Test
    fun `materials discipline shelf carries the discipline id`() {
        assertEquals(
            DeepLinkTarget.MaterialsDiscipline("6f1a2b3c-4d5e-6f70-8192-a3b4c5d6e7f8"),
            parseDeepLink("unes://materials/discipline/6f1a2b3c-4d5e-6f70-8192-a3b4c5d6e7f8"),
        )
    }

    @Test
    fun `material detail carries the material id`() {
        assertEquals(
            DeepLinkTarget.MaterialDetail("6f1a2b3c-4d5e-6f70-8192-a3b4c5d6e7f8"),
            parseDeepLink("unes://materials/6f1a2b3c-4d5e-6f70-8192-a3b4c5d6e7f8"),
        )
    }

    @Test
    fun `query params are ignored`() {
        assertEquals(
            DeepLinkTarget.MaterialDetail("mat-1"),
            parseDeepLink("unes://materials/mat-1?utm=push"),
        )
    }

    @Test
    fun `unknown shapes are dropped`() {
        assertNull(parseDeepLink("unes://materials"))
        assertNull(parseDeepLink("unes://settings"))
        assertNull(parseDeepLink("unes://messages/a/b"))
        assertNull(parseDeepLink("unes://materials/discipline/a/b"))
        assertNull(parseDeepLink("unes://"))
        assertNull(parseDeepLink(""))
    }

    @Test
    fun `other schemes are not ours`() {
        assertNull(parseDeepLink("https://messages/abc"))
        assertNull(parseDeepLink("unes:messages"))
    }

    @Test
    fun `fragment is ignored`() {
        assertEquals(DeepLinkTarget.Message("abc"), parseDeepLink("unes://messages/abc#section"))
    }
}
