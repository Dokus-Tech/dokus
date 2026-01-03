package tech.dokus.domain.flags

enum class FeatureFlag(val defaultState: Boolean) {
    OwnServers(false),
    AddWorkspaceAvatar(false)
}
