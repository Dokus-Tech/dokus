package ai.dokus.foundation.database.repository

import ai.dokus.foundation.database.TestDatabaseFactory
import ai.dokus.foundation.database.TestFixtures
import ai.dokus.foundation.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for ClientRepository
 * Tests CRUD operations, tenant isolation, and Peppol functionality
 */
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
        // Given
        val client = TestFixtures.createTestClient()

        // When
        val result = TestDatabaseFactory.dbQuery {
            repository.create(client)
        }

        // Then
        assertNotNull(result)
        assertEquals(client.id, result.id)
        assertEquals(client.name, result.name)
        assertEquals(client.email, result.email)
        assertEquals(client.vatNumber, result.vatNumber)
        assertEquals(client.peppolId, result.peppolId)
        assertTrue(result.peppolEnabled)
    }

    @Test
    fun `findById should return client when exists`() = runBlocking {
        // Given
        val client = TestFixtures.createTestClient()
        TestDatabaseFactory.dbQuery {
            repository.create(client)
        }

        // When
        val result = TestDatabaseFactory.dbQuery {
            repository.findById(client.id, client.tenantId)
        }

        // Then
        assertNotNull(result)
        assertEquals(client.id, result.id)
        assertEquals(client.name, result.name)
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
        val client = TestFixtures.createTestClient(
            tenantId = TestFixtures.tenant1Id
        )
        TestDatabaseFactory.dbQuery {
            repository.create(client)
        }

        // When - Try to access with tenant2
        val result = TestDatabaseFactory.dbQuery {
            repository.findById(client.id, TestFixtures.tenant2Id)
        }

        // Then - Should not find client from different tenant
        assertNull(result)
    }

    @Test
    fun `findByTenantId should return all clients for tenant`() = runBlocking {
        // Given - Create 2 clients for tenant1 and 1 for tenant2
        val client1 = TestFixtures.createTestClient(
            id = TestFixtures.client1Id,
            tenantId = TestFixtures.tenant1Id,
            name = "Client 1"
        )
        val client2 = TestFixtures.createTestClient(
            id = TestFixtures.client2Id,
            tenantId = TestFixtures.tenant1Id,
            name = "Client 2"
        )
        val client3 = TestFixtures.createTestClient(
            id = ClientId("10000000-0000-0000-0000-000000000003"),
            tenantId = TestFixtures.tenant2Id,
            name = "Client 3"
        )

        TestDatabaseFactory.dbQuery {
            repository.create(client1)
            repository.create(client2)
            repository.create(client3)
        }

        // When
        val result = TestDatabaseFactory.dbQuery {
            repository.findByTenantId(TestFixtures.tenant1Id, limit = 10, offset = 0)
        }

        // Then - Should only return clients for tenant1
        assertEquals(2, result.size)
        assertTrue(result.all { it.tenantId == TestFixtures.tenant1Id })
        assertTrue(result.any { it.name == "Client 1" })
        assertTrue(result.any { it.name == "Client 2" })
    }

    @Test
    fun `findByTenantId should respect pagination`() = runBlocking {
        // Given - Create 5 clients
        repeat(5) { index ->
            val client = TestFixtures.createTestClient(
                id = ClientId("10000000-0000-0000-0000-00000000000$index"),
                tenantId = TestFixtures.tenant1Id,
                name = "Client $index"
            )
            TestDatabaseFactory.dbQuery {
                repository.create(client)
            }
        }

        // When - Get first page (2 items)
        val page1 = TestDatabaseFactory.dbQuery {
            repository.findByTenantId(TestFixtures.tenant1Id, limit = 2, offset = 0)
        }

        // Then
        assertEquals(2, page1.size)

        // When - Get second page (2 items)
        val page2 = TestDatabaseFactory.dbQuery {
            repository.findByTenantId(TestFixtures.tenant1Id, limit = 2, offset = 2)
        }

        // Then
        assertEquals(2, page2.size)
        assertTrue(page1.map { it.id }.intersect(page2.map { it.id }.toSet()).isEmpty())
    }

    @Test
    fun `update should modify existing client`() = runBlocking {
        // Given
        val original = TestFixtures.createTestClient(name = "Original Name")
        TestDatabaseFactory.dbQuery {
            repository.create(original)
        }

        // When
        val updated = original.copy(
            name = "Updated Name",
            email = Email("newemail@example.com"),
            peppolEnabled = false
        )
        val result = TestDatabaseFactory.dbQuery {
            repository.update(updated)
        }

        // Then
        assertNotNull(result)
        assertEquals("Updated Name", result?.name)
        assertEquals(Email("newemail@example.com"), result?.email)
        assertEquals(false, result?.peppolEnabled)
    }

    @Test
    fun `delete should soft delete client`() = runBlocking {
        // Given
        val client = TestFixtures.createTestClient()
        TestDatabaseFactory.dbQuery {
            repository.create(client)
        }

        // When
        TestDatabaseFactory.dbQuery {
            repository.delete(client.id, client.tenantId)
        }

        // Then - Client should not be found (soft deleted)
        val result = TestDatabaseFactory.dbQuery {
            repository.findById(client.id, client.tenantId)
        }
        assertNull(result)
    }

    @Test
    fun `count should return correct number of clients`() = runBlocking {
        // Given - Create 3 clients for tenant1
        repeat(3) { index ->
            val client = TestFixtures.createTestClient(
                id = ClientId("10000000-0000-0000-0000-00000000000$index"),
                tenantId = TestFixtures.tenant1Id
            )
            TestDatabaseFactory.dbQuery {
                repository.create(client)
            }
        }

        // When
        val count = TestDatabaseFactory.dbQuery {
            repository.count(TestFixtures.tenant1Id)
        }

        // Then
        assertEquals(3, count)
    }

    @Test
    fun `findByPeppolId should find client by Peppol ID`() = runBlocking {
        // Given
        val client = TestFixtures.createTestClient(
            peppolId = "0208:BE0987654321",
            peppolEnabled = true
        )
        TestDatabaseFactory.dbQuery {
            repository.create(client)
        }

        // When
        val result = TestDatabaseFactory.dbQuery {
            repository.findByPeppolId("0208:BE0987654321", client.tenantId)
        }

        // Then
        assertNotNull(result)
        assertEquals(client.id, result.id)
        assertEquals("0208:BE0987654321", result.peppolId)
    }

    @Test
    fun `findByPeppolId should return null when Peppol ID not found`() = runBlocking {
        // When
        val result = TestDatabaseFactory.dbQuery {
            repository.findByPeppolId("0208:NONEXISTENT", TestFixtures.tenant1Id)
        }

        // Then
        assertNull(result)
    }

    @Test
    fun `search should find clients by name`() = runBlocking {
        // Given
        val client1 = TestFixtures.createTestClient(
            id = TestFixtures.client1Id,
            name = "Acme Corporation"
        )
        val client2 = TestFixtures.createTestClient(
            id = TestFixtures.client2Id,
            name = "Tech Solutions Ltd"
        )
        TestDatabaseFactory.dbQuery {
            repository.create(client1)
            repository.create(client2)
        }

        // When - Search for "Acme"
        val results = TestDatabaseFactory.dbQuery {
            repository.search(TestFixtures.tenant1Id, "Acme", limit = 10)
        }

        // Then
        assertEquals(1, results.size)
        assertEquals("Acme Corporation", results.first().name)
    }

    @Test
    fun `search should find clients by email`() = runBlocking {
        // Given
        val client = TestFixtures.createTestClient(
            email = Email("unique@example.com")
        )
        TestDatabaseFactory.dbQuery {
            repository.create(client)
        }

        // When - Search by email
        val results = TestDatabaseFactory.dbQuery {
            repository.search(TestFixtures.tenant1Id, "unique@example.com", limit = 10)
        }

        // Then
        assertEquals(1, results.size)
        assertEquals(Email("unique@example.com"), results.first().email)
    }
}
