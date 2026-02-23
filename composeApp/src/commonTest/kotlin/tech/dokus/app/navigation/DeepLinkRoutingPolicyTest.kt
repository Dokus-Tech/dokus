package tech.dokus.app.navigation

import tech.dokus.domain.model.common.DeepLink
import tech.dokus.navigation.destinations.AuthDestination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DeepLinkRoutingPolicyTest {

    @Test
    fun `auth reset password deep link resolves to root destination`() {
        val target = resolveDeepLinkNavigationTarget(
            DeepLink("dokus://auth/reset-password?token=abc")
        )

        assertEquals(DeepLinkTargetOwner.Root, target.owner)
        val rootTarget = assertIs<DeepLinkNavigationTarget.RootDestination>(target)
        assertEquals(AuthDestination.ResetPassword("abc"), rootTarget.destination)
    }

    @Test
    fun `auth verify email deep link resolves to root destination`() {
        val target = resolveDeepLinkNavigationTarget(
            DeepLink("https://app.dokus.tech/auth/verify-email?token=mail-123")
        )

        assertEquals(DeepLinkTargetOwner.Root, target.owner)
        val rootTarget = assertIs<DeepLinkNavigationTarget.RootDestination>(target)
        assertEquals(AuthDestination.VerifyEmail("mail-123"), rootTarget.destination)
    }

    @Test
    fun `document review deep link resolves to home command`() {
        val target = resolveDeepLinkNavigationTarget(
            DeepLink("dokus://documents/review?documentId=doc-9")
        )

        assertEquals(DeepLinkTargetOwner.Home, target.owner)
        val homeTarget = assertIs<DeepLinkNavigationTarget.HomeCommand>(target)
        assertEquals(HomeNavigationCommand.OpenDocumentReview("doc-9"), homeTarget.command)
    }
}
