#!/bin/bash
# Database module migration script

set -e

echo "Starting database module migration..."

# Copy shared database utilities to each backend
BACKENDS=("auth" "invoicing" "expense" "payment" "reporting" "audit" "banking")

for backend in "${BACKENDS[@]}"; do
    echo "Creating database utils for $backend..."
    mkdir -p "features/$backend/backend/src/main/kotlin/ai/dokus/$backend/backend/database/utils"

    # Copy DatabaseFactory and dbQuery
    cp "foundation/database/src/main/kotlin/ai/dokus/foundation/database/utils/DatabaseFactory.kt" \
       "features/$backend/backend/src/main/kotlin/ai/dokus/$backend/backend/database/utils/"

    # Update package names
    sed -i '' "s/package ai.dokus.foundation.database.utils/package ai.dokus.$backend.backend.database.utils/g" \
        "features/$backend/backend/src/main/kotlin/ai/dokus/$backend/backend/database/utils/DatabaseFactory.kt"
done

echo "Auth Backend - Moving tables..."
mkdir -p "features/auth/backend/src/main/kotlin/ai/dokus/auth/backend/database/tables"
cp foundation/database/src/main/kotlin/ai/dokus/foundation/database/tables/{UsersTable,RefreshTokensTable,TenantsTable,TenantSettingsTable}.kt \
   features/auth/backend/src/main/kotlin/ai/dokus/auth/backend/database/tables/

echo "Invoicing Backend - Moving tables..."
mkdir -p "features/invoicing/backend/src/main/kotlin/ai/dokus/invoicing/backend/database/tables"
cp foundation/database/src/main/kotlin/ai/dokus/foundation/database/tables/{InvoicesTable,InvoiceItemsTable,ClientsTable}.kt \
   features/invoicing/backend/src/main/kotlin/ai/dokus/invoicing/backend/database/tables/

echo "Expense Backend - Moving tables..."
mkdir -p "features/expense/backend/src/main/kotlin/ai/dokus/expense/backend/database/tables"
cp foundation/database/src/main/kotlin/ai/dokus/foundation/database/tables/{ExpensesTable,AttachmentsTable}.kt \
   features/expense/backend/src/main/kotlin/ai/dokus/expense/backend/database/tables/

echo "Payment Backend - Moving tables..."
mkdir -p "features/payment/backend/src/main/kotlin/ai/dokus/payment/backend/database/tables"
cp foundation/database/src/main/kotlin/ai/dokus/foundation/database/tables/PaymentsTable.kt \
   features/payment/backend/src/main/kotlin/ai/dokus/payment/backend/database/tables/

echo "Reporting Backend - Moving tables..."
mkdir -p "features/reporting/backend/src/main/kotlin/ai/dokus/reporting/backend/database/tables"
cp foundation/database/src/main/kotlin/ai/dokus/foundation/database/tables/VatReturnsTable.kt \
   features/reporting/backend/src/main/kotlin/ai/dokus/reporting/backend/database/tables/

echo "Audit Backend - Moving tables..."
mkdir -p "features/audit/backend/src/main/kotlin/ai/dokus/audit/backend/database/tables"
cp foundation/database/src/main/kotlin/ai/dokus/foundation/database/tables/AuditLogsTable.kt \
   features/audit/backend/src/main/kotlin/ai/dokus/audit/backend/database/tables/

echo "Banking Backend - Moving tables..."
mkdir -p "features/banking/backend/src/main/kotlin/ai/dokus/banking/backend/database/tables"
cp foundation/database/src/main/kotlin/ai/dokus/foundation/database/tables/{BankConnectionsTable,BankTransactionsTable}.kt \
   features/banking/backend/src/main/kotlin/ai/dokus/banking/backend/database/tables/

echo "Migration script completed!"
echo "Note: You still need to update package names in moved files"
