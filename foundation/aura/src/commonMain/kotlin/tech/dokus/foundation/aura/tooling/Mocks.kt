package tech.dokus.foundation.aura.tooling

import tech.dokus.domain.Email
import tech.dokus.domain.Name
import tech.dokus.domain.ids.UserId
import kotlin.uuid.ExperimentalUuidApi

// Simple mock values for previews
val mockEmail = Email("john.doe@dokus.be")
val mockName = Name("John")
val mockLastName = Name("Doe")

@OptIn(ExperimentalUuidApi::class)
val mockUserId = UserId.generate()
