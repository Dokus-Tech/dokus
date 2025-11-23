package ai.dokus.app.auth.viewmodel

import ai.dokus.app.core.state.DokusState
import ai.dokus.app.core.viewmodel.BaseViewModel

internal class CompanyCreateViewModel : BaseViewModel<DokusState<Unit>>(DokusState.idle()) {

}