package ai.thepredict.repository.api

import ai.thepredict.configuration.ServerEndpoint
import kotlin.coroutines.CoroutineContext

class UnifiedApi private constructor(
    identityApi: IdentityApi,
    contactsApi: ContactsApi,
    documentsApi: DocumentsApi,
    predictionApi: PredictionApi,
    simulationApi: SimulationApi,
) : IdentityApi by identityApi,
    ContactsApi by contactsApi,
    DocumentsApi by documentsApi,
    PredictionApi by predictionApi,
    SimulationApi by simulationApi {

    companion object {
        fun create(
            coroutineContext: CoroutineContext,
            gateway: ServerEndpoint.Gateway,
        ): UnifiedApi {
            return UnifiedApi(
                IdentityApi.create(coroutineContext, gateway),
                ContactsApi.create(coroutineContext, gateway),
                DocumentsApi.create(coroutineContext, gateway),
                PredictionApi.create(coroutineContext, gateway),
                SimulationApi.create(coroutineContext, gateway)
            )
        }

        fun create(
            coroutineContext: CoroutineContext,
            identity: ServerEndpoint.Identity,
            contacts: ServerEndpoint.Contacts,
            documents: ServerEndpoint.Documents,
            prediction: ServerEndpoint.Prediction,
            simulation: ServerEndpoint.Simulation,
        ): UnifiedApi {
            return UnifiedApi(
                IdentityApi.create(coroutineContext, identity),
                ContactsApi.create(coroutineContext, contacts),
                DocumentsApi.create(coroutineContext, documents),
                PredictionApi.create(coroutineContext, prediction),
                SimulationApi.create(coroutineContext, simulation)
            )
        }
    }
}

interface ApiCompanion<ApiClass, EndpointType : ServerEndpoint> {
    fun create(
        coroutineContext: CoroutineContext,
        endpoint: EndpointType,
    ): ApiClass

    fun create(
        coroutineContext: CoroutineContext,
        endpoint: ServerEndpoint.Gateway,
    ): ApiClass
}