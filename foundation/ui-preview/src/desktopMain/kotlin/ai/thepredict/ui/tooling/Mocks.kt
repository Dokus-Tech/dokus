package ai.thepredict.ui.tooling

import ai.thepredict.data.Workspace
import kotlinx.datetime.LocalDateTime

val LocalDateTime.Companion.mocked by lazy {
    LocalDateTime(2025, 1, 1, 0, 0)
}

val Workspace.Companion.mockedIv by lazy {
    Workspace(
        Workspace.Id.random,
        "Invoid Vision",
        "Invoid Vision BV",
        taxNumber = "BE0777887045",
        createdAt = LocalDateTime.mocked
    )
}

val Workspace.Companion.mockedPredict by lazy {
    Workspace(
        id = Workspace.Id.random,
        name = "Predict",
        legalName = "The Predict SRL",
        createdAt = LocalDateTime.mocked
    )
}