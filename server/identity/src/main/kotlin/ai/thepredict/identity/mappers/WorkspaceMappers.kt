package ai.thepredict.identity.mappers

import ai.thepredict.database.tables.WorkspaceEntity
import ai.thepredict.data.Workspace

val WorkspaceEntity.asWorkspaceApi: Workspace
    get() = Workspace(
        id = Workspace.Id.random,
        name = name,
        legalName = legalName,
    )