package ai.dokus.cashflow.backend

import ai.dokus.cashflow.backend.repository.AttachmentRepository
import ai.dokus.cashflow.backend.repository.ExpenseRepository
import ai.dokus.cashflow.backend.repository.InvoiceRepository
import ai.dokus.cashflow.backend.rpc.CashflowRemoteServiceImpl
import ai.dokus.cashflow.backend.service.DocumentStorageService
import ai.dokus.foundation.domain.rpc.CashflowRemoteService
import ai.dokus.foundation.ktor.security.AuthInfoProvider
import ai.dokus.foundation.ktor.security.JwtValidator
import io.ktor.server.routing.*
import kotlinx.rpc.krpc.ktor.server.rpc
import kotlinx.rpc.krpc.serialization.json.json
import org.koin.ktor.ext.get
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("RemoteServices")

fun Route.withRemoteServices() {
    val jwtValidator by inject<JwtValidator>()

    rpc("/rpc") {
        rpcConfig {
            serialization {
                json()
            }
        }

        registerService<CashflowRemoteService> {
            CashflowRemoteServiceImpl(
                authInfoProvider = AuthInfoProvider(call, jwtValidator),
                attachmentRepository = get<AttachmentRepository>(),
                documentStorageService = get<DocumentStorageService>(),
                invoiceRepository = get<InvoiceRepository>(),
                expenseRepository = get<ExpenseRepository>()
            )
        }
    }

    logger.info("RPC APIs registered at /rpc")
}