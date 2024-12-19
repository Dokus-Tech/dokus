package ai.thepredict.domain

import kotlinx.serialization.Serializable

@Serializable
data class Contact(
    val name: String,
    val phoneNumber: String? = null,
    val email: String? = null,
    val taxNumber: String? = null,
    val companyName: String? = null,
    val notes: List<Note> = emptyList(),
    val url: String? = null,
    val logo: String? = null,
)
