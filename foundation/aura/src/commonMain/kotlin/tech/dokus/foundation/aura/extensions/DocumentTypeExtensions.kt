package tech.dokus.foundation.aura.extensions

import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.Calculator
import com.composables.icons.lucide.File
import com.composables.icons.lucide.FileMinus
import com.composables.icons.lucide.FileText
import com.composables.icons.lucide.Landmark
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Receipt
import com.composables.icons.lucide.ScrollText
import com.composables.icons.lucide.Users
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.DocumentTypeCategory
import tech.dokus.domain.enums.category

val DocumentType.iconized: ImageVector
    get() = when (this) {
        DocumentType.Invoice -> Lucide.FileText
        DocumentType.CreditNote -> Lucide.FileMinus
        DocumentType.Receipt -> Lucide.Receipt
        else -> when (this.category) {
            DocumentTypeCategory.FINANCIAL -> Lucide.File
            DocumentTypeCategory.BANKING -> Lucide.Landmark
            DocumentTypeCategory.TAX -> Lucide.Calculator
            DocumentTypeCategory.PAYROLL -> Lucide.Users
            DocumentTypeCategory.LEGAL -> Lucide.ScrollText
        }
    }

@Suppress("NOTHING_TO_INLINE")
inline fun DocumentType?.iconizedOrDefault(): ImageVector = this?.iconized ?: Lucide.File
