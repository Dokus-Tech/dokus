package ai.dokus.foundation.database.repository

import ai.dokus.foundation.database.TestDatabaseFactory
import ai.dokus.foundation.database.TestFixtures
import ai.dokus.foundation.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi

/**
 * Unit tests for ClientRepository
 * Tests CRUD operations, tenant isolation, and Peppol functionality
 */
@OptIn(ExperimentalUuidApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClientRepositoryTest {
    private lateinit var repository: ClientRepository

    @BeforeAll
    fun setup() {
        TestDatabaseFactory.init()
        repository = ClientRepository()
    }

    @BeforeEach
    fun cleanDatabase() {
        TestDatabaseFactory.clean()
    }

    @Test
    fun `create client should persist to database`() = runBlocking {
        // When
        val clientId = TestDatabaseFactory.dbQuery {
            repository.create(
                tenantId = TestFixtures.tenant1Id,
                name = "Acme Corporation",
                email = "contact@acme.com",
                phone = "+32 2 123 4567",
                vatNumber = "BE0123456789",
                addressLine1 = "123 Main Street",
                city = "Brussels",
                postalCode = "1000",
                country = "BE",
                peppolId = "0208:BE0123456789",
                peppolEnabled = true
            )
        }

        // Then
        assertNotNull(clientId)

        // Verify by fetching
        val client = TestDatabaseFactory.dbQuery {
            repository.findById(clientId, TestFixtures.tenant1Id)
        }
        assertNotNull(client)
        assertEquals("Acme Corporation", client.name)
        assertEquals(Email("contact@acme.com"), client.email)
        assertEquals(VatNumber("BE0123456789"), client.vatNumber)
        assertEquals("0208:BE0123456789", client.peppolId)
        assertTrue(client.peppolEnabled)
    }

    @Test
    fun `findById should return client when exists`() = runBlocking {
        // Given
        val clientId = TestDatabaseFactory.dbQuery {
            repository.create(
                tenantId = TestFixtures.tenant1Id,
                name = "Test Client"
            )
        }

        // When
        val result = TestDatabaseFactory.dbQuery {
            repository.findById(clientId, TestFixtures.tenant1Id)
        }

        // Then
        assertNotNull(result)
        assertEquals(clientId, result.id)
        assertEquals("Test Client", result.name)
    }

    @Test
    fun `findById should return null when client does not exist`() = runBlocking {
        // When
        val result = TestDatabaseFactory.dbQuery {
            repository.findById(TestFixtures.client1Id, TestFixtures.tenant1Id)
        }

        // Then
        assertNull(result)
    }

    @Test
    fun `findById should enforce tenant isolation`() = runBlocking {
        // Given - Create client for tenant1
        val clientId = TestDatabaseFactory.dbQuery {
            repository.create(
                tenantId = TestFixtures.tenant1Id,
                name = "Tenant 1 Client"
            )
        }

        // When - Try to access with wrong tenant
        val result = TestDatabaseFactory.dbQuery {
            repository.findById(clientId, TestFixtures.tenant2Id)
        }

        // Then
        assertNull(result)
    }

    @Test
    fun `findByTenant should return all clients for tenant`() = runBlocking {
        // Given
        TestDatabaseFactory.dbQuery {
            repository.create(TestFixtures.tenant1Id, "Client 1")
            repository.create(TestFixtures.tenant1Id, "Client 2")
            repository.create(TestFixtures.tenant1Id, "Client 3")
            repository.create(TestFixtures.tenant2Id, "Tenant 2 Client")
        }

        // When
        val results = TestDatabaseFactory.dbQuery {
            repository.findByTenant(TestFixtures.tenant1Id)
        }

        // Then
        assertEquals(3, results.size)
        assertTrue(results.all { it.tenantId == TestFixtures.tenant1Id })
    }

    @Test
    fun `findByTenant with search should filter results`() = runBlocking {
        // Given
        TestDatabaseFactory.dbQuery {
            repository.create(TestFixtures.tenant1Id, "Acme Corporation", email = "acme@example.com")
            repository.create(TestFixtures.tenant1Id, "Beta Industries", email = "beta@example.com")
            repository.create(TestFixtures.tenant1Id, "Gamma Services", email = "gamma@example.com")
        }

        // When
        val results = TestDatabaseFactory.dbQuery {
            repository.findByTenant(TestFixtures.tenant1Id, search = "Acme")
        }

        // Then
        assertEquals(1, results.size)
        assertEquals("Acme Corporation", results.first().name)
    }

    @Test
    fun `findByTenant with isActive filter should work`() = runBlocking {
        // Given
        val clientId = TestDatabaseFactory.dbQuery {
            repository.create(TestFixtures.tenant1Id, "Active Client")
        }
        TestDatabaseFactory.dbQuery {
            repository.create(TestFixtures.tenant1Id, "Another Client")
        }

        // Deactivate one client
        TestDatabaseFactory.dbQuery {
            repository.delete(clientId, TestFixtures.tenant1Id)
        }

        // When - Find only active
        val activeResults = TestDatabaseFactory.dbQuery {
            repository.findByTenant(TestFixtures.tenant1Id, isActive = true)
        }

        // Then
        assertEquals(1, activeResults.size)
        assertEquals("Another Client", activeResults.first().name)
    }

    @Test
    fun `update client should modify fields`() = runBlocking {
        // Given
        val clientId = TestDatabaseFactory.dbQuery {
            repository.create(
                tenantId = TestFixtures.tenant1Id,
                name = "Original Name",
                email = "original@example.com"
            )
        }

        // When
        val updated = TestDatabaseFactory.dbQuery {
            repository.update(
                id = clientId,
                tenantId = TestFixtures.tenant1Id,
                name = "Updated Name",
                email = "updated@example.com",
                peppolEnabled = true
            )
        }

        // Then
        assertTrue(updated)

        val client = TestDatabaseFactory.dbQuery {
            repository.findById(clientId, TestFixtures.tenant1Id)
        }
        assertNotNull(client)
        assertEquals("Updated Name", client.name)
        assertEquals(Email("updated@example.com"), client.email)
        assertTrue(client.peppolEnabled)
    }

    @Test
    fun `update should enforce tenant isolation`() = runBlocking {
        // Given
        val clientId = TestDatabaseFactory.dbQuery {
            repository.create(TestFixtures.tenant1Id, "Client")
        }

        // When - Try to update with wrong tenant
        val updated = TestDatabaseFactory.dbQuery {
            repository.update(
                id = clientId,
                tenantId = TestFixtures.tenant2Id,
                name = "Hacked Name"
            )
        }

        // Then
        assertFalse(updated)

        // Verify original name unchanged
        val client = TestDatabaseFactory.dbQuery {
            repository.findById(clientId, TestFixtures.tenant1Id)
        }
        assertEquals("Client", client?.name)
    }

    @Test
    fun `delete client should soft delete`() = runBlocking {
        // Given
        val clientId = TestDatabaseFactory.dbQuery {
            repository.create(TestFixtures.tenant1Id, "To Delete")
        }

        // When
        val deleted = TestDatabaseFactory.dbQuery {
            repository.delete(clientId, TestFixtures.tenant1Id)
        }

        // Then
        assertTrue(deleted)

        // Client should still exist but be inactive
        val client = TestDatabaseFactory.dbQuery {
            repository.findById(clientId, TestFixtures.tenant1Id)
        }
        assertNotNull(client)
        assertFalse(client.isActive)
    }

    @Test
    fun `findByPeppolId should return client`() = runBlocking {
        // Given
        TestDatabaseFactory.dbQuery {
            repository.create(
                tenantId = TestFixtures.tenant1Id,
                name = "Peppol Client",
                peppolId = "0208:BE0123456789",
                peppolEnabled = true
            )
        }

        // When
        val client = TestDatabaseFactory.dbQuery {
            repository.findByPeppolId("0208:BE0123456789", TestFixtures.tenant1Id)
        }

        // Then
        assertNotNull(client)
        assertEquals("Peppol Client", client.name)
        assertEquals("0208:BE0123456789", client.peppolId)
    }

    @Test
    fun `countByTenant should return correct count`() = runBlocking {
        // Given
        TestDatabaseFactory.dbQuery {
            repository.create(TestFixtures.tenant1Id, "Client 1")
            repository.create(TestFixtures.tenant1Id, "Client 2")
            repository.create(TestFixtures.tenant2Id, "Other Tenant Client")
        }

        // When
        val count = TestDatabaseFactory.dbQuery {
            repository.countByTenant(TestFixtures.tenant1Id)
        }

        // Then
        assertEquals(2, count)
    }

    @Test
    fun `countByTenant with isActive filter should work`() = runBlocking {
        // Given
        val clientId = TestDatabaseFactory.dbQuery {
            repository.create(TestFixtures.tenant1Id, "Client 1")
        }
        TestDatabaseFactory.dbQuery {
            repository.create(TestFixtures.tenant1Id, "Client 2")
        }

        // Deactivate one
        TestDatabaseFactory.dbQuery {
            repository.delete(clientId, TestFixtures.tenant1Id)
        }

        // When
        val activeCount = TestDatabaseFactory.dbQuery {
            repository.countByTenant(TestFixtures.tenant1Id, isActive = true)
        }

        // Then
        assertEquals(1, activeCount)
    }
}
