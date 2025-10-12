package ai.dokus.foundation.database

import ai.dokus.foundation.database.enums.Language
import ai.dokus.foundation.database.enums.TenantPlan
import ai.dokus.foundation.database.enums.TenantStatus
import ai.dokus.foundation.database.mappers.TenantMapper.toTenant
import ai.dokus.foundation.database.repository.TenantRepository
import ai.dokus.foundation.database.tables.TenantsTable
import ai.dokus.foundation.database.utils.DatabaseFactory
import ai.dokus.foundation.database.utils.dbQuery
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Test to verify that UUID conversion between Kotlin Uuid (domain) and Java UUID (database) works correctly
 */
@OptIn(ExperimentalUuidApi::class)
class UuidConversionTest {

    private lateinit var repository: TenantRepository

    @BeforeEach
    fun setup() {
        // Initialize in-memory H2 database for testing
        DatabaseFactory.init(
            jdbcUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;",
            driver = "org.h2.Driver",
            user = "sa",
            password = "",
            maximumPoolSize = 1
        )

        repository = TenantRepository()
    }

    @Test
    fun `test UUID conversion flow - Domain to Database and back`() = runBlocking {
        // Step 1: Create a tenant (returns Kotlin Uuid)
        val createdId: Uuid = repository.create(
            name = "Test Company",
            email = "test@example.com",
            plan = TenantPlan.PROFESSIONAL,
            country = "US",
            language = Language.EN,
            vatNumber = "US123456789"
        )

        assertNotNull(createdId)
        println("Created tenant with Kotlin Uuid: $createdId")

        // Step 2: Retrieve the tenant using Kotlin Uuid
        val retrievedTenant = repository.findById(createdId)

        assertNotNull(retrievedTenant)
        assertEquals("Test Company", retrievedTenant.name)
        assertEquals("test@example.com", retrievedTenant.email)
        assertEquals(TenantPlan.PROFESSIONAL, retrievedTenant.plan)
        assertEquals(createdId, retrievedTenant.id)

        // Step 3: Verify that the database is actually storing Java UUID
        val databaseRecord = dbQuery {
            TenantsTable
                .selectAll()
                .where { TenantsTable.email eq "test@example.com" }
                .singleOrNull()
        }

        assertNotNull(databaseRecord)
        val dbUuid = databaseRecord[TenantsTable.id].value // This is java.util.UUID

        // Verify it's actually a Java UUID in the database
        assert(dbUuid is java.util.UUID)

        // Step 4: Verify that mapper correctly converts Java UUID to Kotlin Uuid
        val mappedTenant = databaseRecord.toTenant()
        assertEquals(createdId, mappedTenant.id)

        // Step 5: Test settings operations which also use Kotlin Uuid
        val settings = repository.getSettings(createdId)
        assertNotNull(settings)
        assertEquals(createdId, settings.tenantId)
        assertEquals("INV", settings.invoicePrefix)

        println("Test passed! UUID conversion flow works correctly:")
        println("  - Repository accepts Kotlin Uuid in public API")
        println("  - Database stores Java UUID internally")
        println("  - Mappers convert Java UUID back to Kotlin Uuid")
        println("  - Domain models use Kotlin Uuid consistently")
    }

    @Test
    fun `test invoice number generation with Kotlin Uuid`() = runBlocking {
        // Create a tenant
        val tenantId: Uuid = repository.create(
            name = "Invoice Test Company",
            email = "invoice@example.com"
        )

        // Get next invoice number (uses Kotlin Uuid)
        val invoiceNumber1 = repository.getNextInvoiceNumber(tenantId)
        assertEquals("INV-0001", invoiceNumber1)

        val invoiceNumber2 = repository.getNextInvoiceNumber(tenantId)
        assertEquals("INV-0002", invoiceNumber2)

        val invoiceNumber3 = repository.getNextInvoiceNumber(tenantId)
        assertEquals("INV-0003", invoiceNumber3)

        println("Invoice number generation with Kotlin Uuid works correctly")
    }

    @Test
    fun `test finding tenant by email returns correct Kotlin Uuid`() = runBlocking {
        // Create a tenant
        val createdId: Uuid = repository.create(
            name = "Email Test Company",
            email = "email-test@example.com"
        )

        // Find by email
        val foundTenant = repository.findByEmail("email-test@example.com")

        assertNotNull(foundTenant)
        assertEquals(createdId, foundTenant.id)
        assertEquals("Email Test Company", foundTenant.name)

        println("Finding tenant by email returns correct Kotlin Uuid")
    }

    @Test
    fun `test listing active tenants returns list with Kotlin Uuid`() = runBlocking {
        // Create multiple tenants
        val id1 = repository.create("Company 1", "company1@example.com")
        val id2 = repository.create("Company 2", "company2@example.com")
        val id3 = repository.create("Company 3", "company3@example.com")

        // List all active tenants
        val activeTenants = repository.listActiveTenants()

        assertEquals(3, activeTenants.size)

        // Verify all IDs are Kotlin Uuids
        val ids = activeTenants.map { it.id }
        assert(id1 in ids)
        assert(id2 in ids)
        assert(id3 in ids)

        // Verify all returned tenants have Kotlin Uuid type
        activeTenants.forEach { tenant ->
            assert(tenant.id is Uuid)
        }

        println("Listing active tenants returns list with proper Kotlin Uuid types")
    }
}