package com.forcetower.uefs.core.model.edge

data class Rp(val name: String, val id: String)
data class User(val name: String, val displayName: String, val id: String)
data class PublicKeyCredParam(val alg: Int, val type: String)
data class ExcludedCredential(val type: String, val id: String)
data class AuthenticatorSelection(
    val authenticatorAttachment: String?,
    val requireResidentKey: Boolean?,
    val residentKey: String?,
    val userVerification: String?
)
data class Extensions(val credProps: Boolean)

data class PublicKey(
    val rp: Rp,
    val user: User,
    val challenge: String,
    val pubKeyCredParams: List<PublicKeyCredParam>,
    val timeout: Int,
    val excludeCredentials: List<ExcludedCredential>,
    val authenticatorSelection: AuthenticatorSelection,
    val attestation: String,
    val extensions: Extensions
)

data class PasskeyRegister(
    val publicKey: PublicKey
)
