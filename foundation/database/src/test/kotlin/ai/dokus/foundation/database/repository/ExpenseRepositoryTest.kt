package ai.dokus.foundation.database.repository

import ai.dokus.foundation.database.TestDatabaseFactory
import ai.dokus.foundation.database.TestFixtures
import ai.dokus.foundation.domain.*
import ai.dokus.foundation.domain.enums.ExpenseCategory
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for ExpenseRepository
 * Tests expense CRUD operations, category filtering, date range queries, and VAT calculations
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExpenseRepositoryTest {
    private lateinit var repository: ExpenseRepository

    @BeforeAll
    fun setup() {
        TestDatabaseFactory.init()
        repository = ExpenseRepository()
    }

    @BeforeEach
    fun cleanDatabase() {
        TestDatabaseFactory.clean()
    }

    @Test
    fun `create expense should persist to database`() = runBlocking {
        // Given
        val expense = TestFixtures.createTestExpense()

        // When
        val result = TestDatabaseFactory.dbQuery {
            repository.create(expense)
        }

        // Then
        assertNotNull(result)
        assertEquals(expense.id, result.id)
        assertEquals(expense.merchant, result.merchant)
        assertEquals(expense.amount, result.amount)
        assertEquals(expense.category, result.category)
    }

    @Test
    fun `findById should return expense when exists`() = runBlocking {
        // Given
        val expense = TestFixtures.createTestExpense()
        TestDatabaseFactory.dbQuery {
            repository.create(expense)
        }

        // When
        val result = TestDatabaseFactory.dbQuery {
            repository.findById(expense.id, expense.tenantId)
        }

        // Then
        assertNotNull(result)
        assertEquals(expense.id, result.id)
    }

    @Test
    fun `findById should enforce tenant isolation`() = runBlocking {
        // Given - Create expense for tenant1
        val expense = TestFixtures.createTestExpense(tenantId = TestFixtures.tenant1Id)
        TestDatabaseFactory.dbQuery {
            repository.create(expense)
        }

        // When - Try to access with tenant2
        val result = TestDatabaseFactory.dbQuery {
            repository.findById(expense.id, TestFixtures.tenant2Id)
        }

        // Then - Should not find expense from different tenant
        assertNull(result)
    }

    @Test
    fun `findByTenantId should return all expenses for tenant`() = runBlocking {
        // Given - Create 2 expenses for tenant1 and 1 for tenant2
        val expense1 = TestFixtures.createTestExpense(
            id = TestFixtures.expense1Id,
            tenantId = TestFixtures.tenant1Id,
            merchant = "Merchant 1"
        )
        val expense2 = TestFixtures.createTestExpense(
            id = TestFixtures.expense2Id,
            tenantId = TestFixtures.tenant1Id,
            merchant = "Merchant 2"
        )
        val expense3 = TestFixtures.createTestExpense(
            id = ExpenseId("30000000-0000-0000-0000-000000000003"),
            tenantId = TestFixtures.tenant2Id,
            merchant = "Merchant 3"
        )

        TestDatabaseFactory.dbQuery {
            repository.create(expense1)
            repository.create(expense2)
            repository.create(expense3)
        }

        // When
        val result = TestDatabaseFactory.dbQuery {
            repository.findByTenantId(TestFixtures.tenant1Id, limit = 10, offset = 0)
        }

        // Then - Should only return expenses for tenant1
        assertEquals(2, result.size)
        assertTrue(result.all { it.tenantId == TestFixtures.tenant1Id })
    }

    @Test
    fun `findByCategory should filter expenses by category`() = runBlocking {
        // Given - Create expenses in different categories
        val officeExpense = TestFixtures.createTestExpense(
            id = TestFixtures.expense1Id,
            category = ExpenseCategory.OfficeSupplies,
            merchant = "Office Store"
        )
        val travelExpense = TestFixtures.createTestExpense(
            id = TestFixtures.expense2Id,
            category = ExpenseCategory.Travel,
            merchant = "Airline"
        )

        TestDatabaseFactory.dbQuery {
            repository.create(officeExpense)
            repository.create(travelExpense)
        }

        // When - Find office expenses
        val result = TestDatabaseFactory.dbQuery {
            repository.findByCategory(
                TestFixtures.tenant1Id,
                ExpenseCategory.OfficeSupplies,
                limit = 10
            )
        }

        // Then
        assertEquals(1, result.size)
        assertEquals(ExpenseCategory.OfficeSupplies, result.first().category)
    }

    @Test
    fun `findByDateRange should filter expenses by date`() = runBlocking {
        // Given - Create expenses on different dates
        val lastWeek = TestFixtures.today.minus(7, DateTimeUnit.DAY)
        val nextWeek = TestFixtures.today.plus(7, DateTimeUnit.DAY)

        val oldExpense = TestFixtures.createTestExpense(
            id = TestFixtures.expense1Id,
            date = lastWeek,
            merchant = "Old Merchant"
        )
        val recentExpense = TestFixtures.createTestExpense(
            id = TestFixtures.expense2Id,
            date = TestFixtures.today,
            merchant = "Recent Merchant"
        )

        TestDatabaseFactory.dbQuery {
            repository.create(oldExpense)
            repository.create(recentExpense)
        }

        // When - Find expenses from yesterday to tomorrow
        val result = TestDatabaseFactory.dbQuery {
            repository.findByDateRange(
                TestFixtures.tenant1Id,
                TestFixtures.yesterday,
                TestFixtures.tomorrow,
                limit = 10
            )
        }

        // Then - Should only return recent expense
        assertEquals(1, result.size)
        assertEquals("Recent Merchant", result.first().merchant)
    }

    @Test
    fun `calculateTotalByCategory should sum amounts per category`() = runBlocking {
        // Given - Create expenses in same category
        val expense1 = TestFixtures.createTestExpense(
            id = TestFixtures.expense1Id,
            category = ExpenseCategory.OfficeSupplies,
            amount = Money("100.00")
        )
        val expense2 = TestFixtures.createTestExpense(
            id = TestFixtures.expense2Id,
            category = ExpenseCategory.OfficeSupplies,
            amount = Money("150.00")
        )

        TestDatabaseFactory.dbQuery {
            repository.create(expense1)
            repository.create(expense2)
        }

        // When
        val result = TestDatabaseFactory.dbQuery {
            repository.calculateTotalByCategory(TestFixtures.tenant1Id, ExpenseCategory.OfficeSupplies)
        }

        // Then
        assertEquals(Money("250.00"), result)
    }

    @Test
    fun `calculateDeductibleVat should sum deductible VAT amounts`() = runBlocking {
        // Given - Create expenses with VAT
        val fullyDeductible = TestFixtures.createTestExpense(
            id = TestFixtures.expense1Id,
            vatAmount = Money("50.00"),
            isDeductible = true,
            deductiblePercentage = Percentage.FULL
        )
        val partiallyDeductible = TestFixtures.createTestExpense(
            id = TestFixtures.expense2Id,
            vatAmount = Money("100.00"),
            isDeductible = true,
            deductiblePercentage = Percentage("50.00") // 50%
        )
        val notDeductible = TestFixtures.createTestExpense(
            id = ExpenseId("30000000-0000-0000-0000-000000000003"),
            vatAmount = Money("30.00"),
            isDeductible = false
        )

        TestDatabaseFactory.dbQuery {
            repository.create(fullyDeductible)
            repository.create(partiallyDeductible)
            repository.create(notDeductible)
        }

        // When
        val result = TestDatabaseFactory.dbQuery {
            repository.calculateDeductibleVat(
                TestFixtures.tenant1Id,
                TestFixtures.yesterday,
                TestFixtures.tomorrow
            )
        }

        // Then - Should be 50 + (100 * 0.5) = 100.00
        assertEquals(Money("100.00"), result)
    }

    @Test
    fun `update should modify existing expense`() = runBlocking {
        // Given
        val original = TestFixtures.createTestExpense(merchant = "Original Merchant")
        TestDatabaseFactory.dbQuery {
            repository.create(original)
        }

        // When
        val updated = original.copy(
            merchant = "Updated Merchant",
            amount = Money("500.00"),
            category = ExpenseCategory.Travel
        )
        val result = TestDatabaseFactory.dbQuery {
            repository.update(updated)
        }

        // Then
        assertNotNull(result)
        assertEquals("Updated Merchant", result?.merchant)
        assertEquals(Money("500.00"), result?.amount)
        assertEquals(ExpenseCategory.Travel, result?.category)
    }

    @Test
    fun `delete should remove expense from database`() = runBlocking {
        // Given
        val expense = TestFixtures.createTestExpense()
        TestDatabaseFactory.dbQuery {
            repository.create(expense)
        }

        // When
        TestDatabaseFactory.dbQuery {
            repository.delete(expense.id, expense.tenantId)
        }

        // Then - Expense should not be found
        val result = TestDatabaseFactory.dbQuery {
            repository.findById(expense.id, expense.tenantId)
        }
        assertNull(result)
    }

    @Test
    fun `findRecurring should return only recurring expenses`() = runBlocking {
        // Given
        val recurring = TestFixtures.createTestExpense(
            id = TestFixtures.expense1Id,
            merchant = "Monthly Subscription",
            isRecurring = true
        )
        val oneTime = TestFixtures.createTestExpense(
            id = TestFixtures.expense2Id,
            merchant = "One-time Purchase",
            isRecurring = false
        )

        TestDatabaseFactory.dbQuery {
            repository.create(recurring)
            repository.create(oneTime)
        }

        // When
        val result = TestDatabaseFactory.dbQuery {
            repository.findRecurring(TestFixtures.tenant1Id, limit = 10)
        }

        // Then
        assertEquals(1, result.size)
        assertEquals("Monthly Subscription", result.first().merchant)
        assertTrue(result.first().isRecurring)
    }

    @Test
    fun `count should return correct number of expenses`() = runBlocking {
        // Given - Create 3 expenses
        repeat(3) { index ->
            val expense = TestFixtures.createTestExpense(
                id = ExpenseId("30000000-0000-0000-0000-00000000000$index")
            )
            TestDatabaseFactory.dbQuery {
                repository.create(expense)
            }
        }

        // When
        val count = TestDatabaseFactory.dbQuery {
            repository.count(TestFixtures.tenant1Id)
        }

        // Then
        assertEquals(3, count)
    }
}
