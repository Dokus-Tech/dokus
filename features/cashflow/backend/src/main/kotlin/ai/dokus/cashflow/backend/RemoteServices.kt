package ai.dokus.cashflow.backend

import ai.dokus.cashflow.backend.repository.AttachmentRepository
import ai.dokus.cashflow.backend.repository.ExpenseRepository
import ai.dokus.cashflow.backend.repository.InvoiceRepository
import ai.dokus.cashflow.backend.rpc.CashflowRemoteServiceImpl
import ai.dokus.cashflow.backend.service.DocumentStorageService
import ai.dokus.foundation.domain.rpc.CashflowRemoteService
import ai.dokus.foundation.ktor.security.AuthInfoProvider
import io.ktor.server.routing.Route
import io.ktor.server.auth.authenticate
import kotlinx.rpc.krpc.ktor.server.rpc
import kotlinx.rpc.krpc.serialization.json.json
import org.koin.ktor.ext.get
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("RemoteServices")

fun Route.withRemoteServices() {
    // Require JWT authentication for all Cashflow RPC calls
    authenticate("jwt-auth") {
        rpc("/rpc") {
            rpcConfig {
                serialization {
                    json()
                }
            }

            registerService<CashflowRemoteService> {
                CashflowRemoteServiceImpl(
                    authInfoProvider = AuthInfoProvider(call),
                    attachmentRepository = get<AttachmentRepository>(),
                    documentStorageService = get<DocumentStorageService>(),
                    invoiceRepository = get<InvoiceRepository>(),
                    expenseRepository = get<ExpenseRepository>()
                )
            }
        }
    }

    logger.info("RPC APIs registered at /rpc")
}