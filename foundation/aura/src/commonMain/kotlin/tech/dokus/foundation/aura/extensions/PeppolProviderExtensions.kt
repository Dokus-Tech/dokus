package tech.dokus.foundation.aura.extensions

import tech.dokus.domain.model.PeppolProvider

val PeppolProvider.websiteUrl: String
    get() = when (this) {
        PeppolProvider.Recommand -> "https://app.recommand.eu"
    }
