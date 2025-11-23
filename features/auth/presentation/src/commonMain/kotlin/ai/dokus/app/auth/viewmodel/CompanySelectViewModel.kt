package ai.dokus.app.auth.viewmodel

import ai.dokus.app.core.state.DokusState
import ai.dokus.app.core.viewmodel.BaseViewModel
import ai.dokus.foundation.domain.model.Organization

internal class CompanySelectViewModel : BaseViewModel<DokusState<List<Organization>>>(DokusState.idle()) {
}