package tech.dokus.features.ai.retry

import tech.dokus.features.ai.validation.AuditCheck
import tech.dokus.features.ai.validation.AuditReport
import tech.dokus.features.ai.validation.AuditStatus
import tech.dokus.features.ai.validation.CheckType
import tech.dokus.features.ai.validation.Severity
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class FeedbackPromptBuilderTest {

    // =========================================================================
    // Math Feedback Tests
    // =========================================================================

    @Test
    fun `math feedback includes expected and actual values`() {
        val check = AuditCheck.criticalFailure(
            type = CheckType.MATH,
            field = "totalAmount",
            message = "100.00 + 21.00 != 120.00",
            hint = "Check the total line",
            expected = "121.00",
            actual = "120.00"
        )

        val feedback = FeedbackPromptBuilder.buildCheckFeedback(check)

        assertContains(feedback, "121.00")
        assertContains(feedback, "120.00")
        assertContains(feedback, "MATH ERROR")
        assertContains(feedback, "TOTALS")
    }

    @Test
    fun `math feedback includes common OCR mistakes`() {
        val check = AuditCheck.criticalFailure(
            type = CheckType.MATH,
            field = "subtotal",
            message = "Math error",
            hint = "Check digits"
        )

        val feedback = FeedbackPromptBuilder.buildCheckFeedback(check)

        assertContains(feedback, "1↔7")
        assertContains(feedback, "0↔6")
    }

    // =========================================================================
    // OGM Feedback Tests
    // =========================================================================

    @Test
    fun `OGM feedback includes check digit information`() {
        val check = AuditCheck.criticalFailure(
            type = CheckType.CHECKSUM_OGM,
            field = "paymentReference",
            message = "OGM checksum failed",
            hint = "Check for 8↔B confusion",
            expected = "39",
            actual = "40"
        )

        val feedback = FeedbackPromptBuilder.buildCheckFeedback(check)

        assertContains(feedback, "39")
        assertContains(feedback, "40")
        assertContains(feedback, "OGM CHECKSUM")
        assertContains(feedback, "PAYMENT SECTION")
    }

    @Test
    fun `OGM feedback includes all OCR correction hints`() {
        val check = AuditCheck.criticalFailure(
            type = CheckType.CHECKSUM_OGM,
            field = "paymentReference",
            message = "Invalid checksum",
            hint = ""
        )

        val feedback = FeedbackPromptBuilder.buildCheckFeedback(check)

        assertContains(feedback, "0 ↔ O")
        assertContains(feedback, "1 ↔ I")
        assertContains(feedback, "8 ↔ B")
        assertContains(feedback, "5 ↔ S")
        assertContains(feedback, "6 ↔ G")
    }

    // =========================================================================
    // IBAN Feedback Tests
    // =========================================================================

    @Test
    fun `IBAN feedback mentions Belgian format`() {
        val check = AuditCheck.criticalFailure(
            type = CheckType.CHECKSUM_IBAN,
            field = "iban",
            message = "IBAN checksum failed",
            hint = "Re-read bank details",
            actual = "BE6853900754703"
        )

        val feedback = FeedbackPromptBuilder.buildCheckFeedback(check)

        assertContains(feedback, "IBAN CHECKSUM")
        assertContains(feedback, "16")
        assertContains(feedback, "Belgian")
    }

    @Test
    fun `IBAN feedback shows extracted value and length issue`() {
        val check = AuditCheck.criticalFailure(
            type = CheckType.CHECKSUM_IBAN,
            field = "iban",
            message = "Belgian IBAN must be 16 characters",
            hint = "",
            actual = "BE6853900754703"
        )

        val feedback = FeedbackPromptBuilder.buildCheckFeedback(check)

        assertContains(feedback, "BE6853900754703")
        assertContains(feedback, "15 characters")
    }

    // =========================================================================
    // VAT Rate Feedback Tests
    // =========================================================================

    @Test
    fun `VAT rate feedback lists Belgian rates`() {
        val check = AuditCheck.warning(
            type = CheckType.VAT_RATE,
            field = "vatRate",
            message = "Unusual VAT rate: 18%",
            hint = "Check amounts",
            expected = "21%",
            actual = "18%"
        )

        val feedback = FeedbackPromptBuilder.buildCheckFeedback(check)

        assertContains(feedback, "0%")
        assertContains(feedback, "6%")
        assertContains(feedback, "12%")
        assertContains(feedback, "21%")
    }

    // =========================================================================
    // Company Feedback Tests
    // =========================================================================

    @Test
    fun `company not found feedback suggests re-reading VAT`() {
        val check = AuditCheck.warning(
            type = CheckType.COMPANY_EXISTS,
            field = "vendorVatNumber",
            message = "Company not found in registry",
            hint = "",
            actual = "BE0123456780"
        )

        val feedback = FeedbackPromptBuilder.buildCheckFeedback(check)

        assertContains(feedback, "COMPANY NOT FOUND")
        assertContains(feedback, "KBO")
        assertContains(feedback, "BE + 10 digits")
    }

    @Test
    fun `company name mismatch shows both names`() {
        val check = AuditCheck.warning(
            type = CheckType.COMPANY_NAME,
            field = "vendorName",
            message = "Name mismatch",
            hint = "",
            expected = "ACME Corporation NV",
            actual = "Acme Corp"
        )

        val feedback = FeedbackPromptBuilder.buildCheckFeedback(check)

        assertContains(feedback, "ACME Corporation NV")
        assertContains(feedback, "Acme Corp")
        assertContains(feedback, "MISMATCH")
    }

    // =========================================================================
    // Full Feedback Prompt Tests
    // =========================================================================

    @Test
    fun `full prompt includes header and attempt info`() {
        val report = AuditReport.fromChecks(
            listOf(
                AuditCheck.criticalFailure(
                    type = CheckType.MATH,
                    field = "total",
                    message = "Math error",
                    hint = "Check total"
                )
            )
        )

        val prompt = FeedbackPromptBuilder.buildFeedbackPrompt(report, attempt = 1, maxRetries = 2)

        assertContains(prompt, "CORRECTION REQUIRED")
        assertContains(prompt, "Attempt 1 of 2")
        assertContains(prompt, "1 issue")
    }

    @Test
    fun `full prompt includes final attempt warning`() {
        val report = AuditReport.fromChecks(
            listOf(
                AuditCheck.criticalFailure(
                    type = CheckType.MATH,
                    field = "total",
                    message = "Math error",
                    hint = "Check total"
                )
            )
        )

        val prompt = FeedbackPromptBuilder.buildFeedbackPrompt(report, attempt = 2, maxRetries = 2)

        assertContains(prompt, "FINAL attempt")
    }

    @Test
    fun `full prompt handles multiple failures`() {
        val report = AuditReport.fromChecks(
            listOf(
                AuditCheck.criticalFailure(
                    type = CheckType.MATH,
                    field = "total",
                    message = "Math error",
                    hint = "Check total"
                ),
                AuditCheck.criticalFailure(
                    type = CheckType.CHECKSUM_OGM,
                    field = "paymentReference",
                    message = "OGM invalid",
                    hint = "Check payment"
                )
            )
        )

        val prompt = FeedbackPromptBuilder.buildFeedbackPrompt(report, attempt = 1, maxRetries = 2)

        assertContains(prompt, "2 issue")
        assertContains(prompt, "Issue 1")
        assertContains(prompt, "Issue 2")
        assertContains(prompt, "Mathematical Verification")
        assertContains(prompt, "OGM Payment Reference")
    }

    @Test
    fun `correction summary groups by type`() {
        val failures = listOf(
            AuditCheck.criticalFailure(CheckType.MATH, "subtotal", "", ""),
            AuditCheck.criticalFailure(CheckType.MATH, "totalAmount", "", ""),
            AuditCheck.criticalFailure(CheckType.CHECKSUM_OGM, "paymentReference", "", "")
        )

        val summary = FeedbackPromptBuilder.buildCorrectionSummary(failures)

        assertContains(summary, "Mathematical Verification")
        assertContains(summary, "subtotal")
        assertContains(summary, "totalAmount")
        assertContains(summary, "OGM Payment Reference")
    }

    // =========================================================================
    // Edge Cases
    // =========================================================================

    @Test
    fun `empty failures list produces minimal prompt`() {
        val report = AuditReport.fromChecks(emptyList())

        val prompt = FeedbackPromptBuilder.buildFeedbackPrompt(report, attempt = 1, maxRetries = 2)

        assertContains(prompt, "No specific failures")
    }

    @Test
    fun `hint is included when present`() {
        val check = AuditCheck.criticalFailure(
            type = CheckType.MATH,
            field = "total",
            message = "Error",
            hint = "SPECIFIC HINT: Look at line 3"
        )

        val feedback = FeedbackPromptBuilder.buildCheckFeedback(check)

        assertContains(feedback, "SPECIFIC HINT: Look at line 3")
    }
}
