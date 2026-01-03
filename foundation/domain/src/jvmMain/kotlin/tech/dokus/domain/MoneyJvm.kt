package tech.dokus.domain

import java.math.BigDecimal

/**
 * JVM-specific extension functions for database conversion.
 * These use java.math.BigDecimal which is only available on JVM.
 */

// Database decimal scale constants
private const val CurrencyDecimalScale = 2
private const val BasisPointsDecimalScale = 4

// ============================================================================
// Money extensions
// ============================================================================

/**
 * Convert to database decimal value.
 * We store as 12345 (minor units), DB stores as DECIMAL(12,2) like 123.45.
 */
fun Money.toDbDecimal(): BigDecimal = BigDecimal.valueOf(minor, CurrencyDecimalScale)

/**
 * Create Money from a database decimal value.
 * DB stores as DECIMAL(12,2) like 123.45, we store as 12345 (minor units).
 */
fun Money.Companion.fromDbDecimal(dbValue: BigDecimal): Money =
    Money(dbValue.movePointRight(CurrencyDecimalScale).longValueExact())

// ============================================================================
// VatRate extensions
// ============================================================================

/**
 * Convert to database decimal value.
 * We store as 2100 bp, DB stores as DECIMAL(5,4) multiplier like 0.2100.
 */
fun VatRate.toDbDecimal(): BigDecimal = BigDecimal.valueOf(basisPoints.toLong(), BasisPointsDecimalScale)

/**
 * Create VatRate from a database decimal value.
 * DB stores as DECIMAL(5,4) multiplier like 0.2100 (21%), we store as 2100 bp.
 */
fun VatRate.Companion.fromDbDecimal(dbValue: BigDecimal): VatRate =
    VatRate(dbValue.movePointRight(BasisPointsDecimalScale).intValueExact())

// ============================================================================
// Percentage extensions
// ============================================================================

/**
 * Convert to database decimal value.
 * We store as 10000 bp (100%), DB stores as DECIMAL like 100.00.
 */
fun Percentage.toDbDecimal(): BigDecimal = BigDecimal.valueOf(basisPoints.toLong(), CurrencyDecimalScale)

/**
 * Create Percentage from a database decimal value.
 * DB stores as DECIMAL like 100.00 (100%), we store as 10000 bp.
 */
fun Percentage.Companion.fromDbDecimal(dbValue: BigDecimal): Percentage =
    Percentage(dbValue.movePointRight(CurrencyDecimalScale).intValueExact())

// ============================================================================
// Quantity extensions
// ============================================================================

/**
 * Convert to database decimal value.
 */
fun Quantity.toDbDecimal(): BigDecimal = BigDecimal.valueOf(value)

/**
 * Create Quantity from a database decimal value.
 */
fun Quantity.Companion.fromDbDecimal(dbValue: BigDecimal): Quantity =
    Quantity(dbValue.toDouble())
