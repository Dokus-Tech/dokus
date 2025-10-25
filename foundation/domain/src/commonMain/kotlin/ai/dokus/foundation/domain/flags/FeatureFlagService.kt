package ai.dokus.foundation.domain.flags

interface FeatureFlagService {
    operator fun get(flag: FeatureFlag): Boolean

    companion object {
        val defaultsOnly by lazy {
            object : FeatureFlagService {
                override fun get(flag: FeatureFlag): Boolean = flag.defaultState
            }
        }
    }
}