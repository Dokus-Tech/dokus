package tech.dokus.backend.services.auth

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import tech.dokus.database.repository.auth.FirmRepository
import tech.dokus.database.repository.auth.InvitationRepository
import tech.dokus.database.repository.auth.TenantRepository
import tech.dokus.database.repository.auth.UserRepository
import tech.dokus.domain.DisplayName
import tech.dokus.domain.enums.FirmAccessStatus
import tech.dokus.domain.ids.FirmId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.Firm
import tech.dokus.domain.model.FirmAccess

class TeamServiceBookkeeperAccessTest {
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val tenantRepository = mockk<TenantRepository>(relaxed = true)
    private val invitationRepository = mockk<InvitationRepository>(relaxed = true)
    private val firmRepository = mockk<FirmRepository>(relaxed = true)

    private val service = TeamService(
        userRepository = userRepository,
        tenantRepository = tenantRepository,
        invitationRepository = invitationRepository,
        firmRepository = firmRepository,
    )

    @Test
    fun `searchBookkeeperFirms maps alreadyConnected correctly`() = runTest {
        val tenantId = TenantId.generate()
        val connectedFirmId = FirmId.generate()
        val disconnectedFirmId = FirmId.generate()
        val now = LocalDateTime.parse("2026-03-05T12:00:00")

        coEvery { firmRepository.searchActiveFirmsByNameOrVat("kantoor", 20) } returns listOf(
            Firm(
                id = connectedFirmId,
                name = DisplayName("Kantoor One"),
                vatNumber = VatNumber("BE0123456789"),
                isActive = true,
                createdAt = now,
                updatedAt = now,
            ),
            Firm(
                id = disconnectedFirmId,
                name = DisplayName("Kantoor Two"),
                vatNumber = VatNumber("BE9876543210"),
                isActive = true,
                createdAt = now,
                updatedAt = now,
            ),
        )
        coEvery { firmRepository.listActiveAccessByTenant(tenantId) } returns listOf(
            FirmAccess(
                firmId = connectedFirmId,
                tenantId = tenantId,
                status = FirmAccessStatus.Active,
                grantedByUserId = UserId.generate(),
                createdAt = now,
                updatedAt = now,
            )
        )

        val result = service.searchBookkeeperFirms(
            tenantId = tenantId,
            query = "kantoor",
            limit = 20,
        )

        assertEquals(2, result.size)
        assertTrue(result.first { it.firmId == connectedFirmId }.alreadyConnected)
        assertFalse(result.first { it.firmId == disconnectedFirmId }.alreadyConnected)
    }

    @Test
    fun `listBookkeeperAccess returns firms sorted by name`() = runTest {
        val tenantId = TenantId.generate()
        val firmAId = FirmId.generate()
        val firmBId = FirmId.generate()
        val now = LocalDateTime.parse("2026-03-05T12:00:00")

        coEvery { firmRepository.listActiveAccessByTenant(tenantId) } returns listOf(
            FirmAccess(
                firmId = firmBId,
                tenantId = tenantId,
                status = FirmAccessStatus.Active,
                grantedByUserId = UserId.generate(),
                createdAt = now,
                updatedAt = now,
            ),
            FirmAccess(
                firmId = firmAId,
                tenantId = tenantId,
                status = FirmAccessStatus.Active,
                grantedByUserId = UserId.generate(),
                createdAt = now,
                updatedAt = now,
            ),
        )
        coEvery { firmRepository.listFirmsByIds(any()) } returns listOf(
            Firm(
                id = firmBId,
                name = DisplayName("Zeta Accounting"),
                vatNumber = VatNumber("BE9999999999"),
                isActive = true,
                createdAt = now,
                updatedAt = now,
            ),
            Firm(
                id = firmAId,
                name = DisplayName("Alpha Accounting"),
                vatNumber = VatNumber("BE1111111111"),
                isActive = true,
                createdAt = now,
                updatedAt = now,
            ),
        )

        val result = service.listBookkeeperAccess(tenantId)

        assertEquals(2, result.size)
        assertEquals("Alpha Accounting", result[0].firmName.value)
        assertEquals("Zeta Accounting", result[1].firmName.value)
    }

    @Test
    fun `grantBookkeeperAccess returns failure when firm does not exist`() = runTest {
        val tenantId = TenantId.generate()
        val firmId = FirmId.generate()

        coEvery { firmRepository.findById(firmId) } returns null

        val result = service.grantBookkeeperAccess(
            tenantId = tenantId,
            firmId = firmId,
            grantedBy = UserId.generate(),
        )

        assertTrue(result.isFailure)
        assertIs<IllegalArgumentException>(result.exceptionOrNull())
    }

    @Test
    fun `grantBookkeeperAccess delegates to activateAccess`() = runTest {
        val tenantId = TenantId.generate()
        val firmId = FirmId.generate()
        val grantedBy = UserId.generate()
        val now = LocalDateTime.parse("2026-03-05T12:00:00")

        coEvery { firmRepository.findById(firmId) } returns Firm(
            id = firmId,
            name = DisplayName("Firm"),
            vatNumber = VatNumber("BE0123000000"),
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )
        coEvery {
            firmRepository.activateAccess(
                firmId = firmId,
                tenantId = tenantId,
                grantedByUserId = grantedBy,
            )
        } returns true

        val result = service.grantBookkeeperAccess(
            tenantId = tenantId,
            firmId = firmId,
            grantedBy = grantedBy,
        )

        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrThrow())
        coVerify(exactly = 1) {
            firmRepository.activateAccess(
                firmId = firmId,
                tenantId = tenantId,
                grantedByUserId = grantedBy,
            )
        }
    }

    @Test
    fun `revokeBookkeeperAccess delegates to revokeAccess`() = runTest {
        val tenantId = TenantId.generate()
        val firmId = FirmId.generate()

        coEvery { firmRepository.revokeAccess(firmId, tenantId) } returns true

        val result = service.revokeBookkeeperAccess(
            tenantId = tenantId,
            firmId = firmId,
        )

        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrThrow())
        coVerify(exactly = 1) { firmRepository.revokeAccess(firmId, tenantId) }
    }
}
