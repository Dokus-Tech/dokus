package tech.dokus.backend.services.business

enum class EnrichmentTrigger(val reason: String) {
    ContactCreated("CONTACT_CREATED"),
    ContactNameChanged("CONTACT_NAME_CHANGED"),
    WebsiteChanged("WEBSITE_CHANGED"),
    TenantCreated("TENANT_CREATED"),
    TenantAddressUpdated("TENANT_ADDRESS_UPDATED"),
}
