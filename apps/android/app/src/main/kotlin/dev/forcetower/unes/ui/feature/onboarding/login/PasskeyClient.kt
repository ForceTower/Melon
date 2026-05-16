package dev.forcetower.unes.ui.feature.onboarding.login

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.forcetower.melon.feature.auth.domain.model.PasskeyAssertion
import dev.forcetower.melon.feature.auth.domain.model.PasskeyChallenge
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/**
 * Bridges shared-kmp's `PasskeyChallenge` / `PasskeyAssertion` over
 * androidx.credentials.CredentialManager — the Android counterpart of the iOS
 * `PasskeyAuthenticator`. The platform layer already returns base64url-encoded
 * binary fields, which is exactly what the server expects, so we just forward.
 */
@Singleton
class PasskeyClient @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    suspend fun assert(challenge: PasskeyChallenge, activity: Activity): PasskeyAssertion {
        val requestJson = encodeRequestJson(challenge)
        val option = GetPublicKeyCredentialOption(requestJson = requestJson)
        val request = GetCredentialRequest(credentialOptions = listOf(option))

        val response = try {
            CredentialManager.create(context).getCredential(activity, request)
        } catch (cancel: GetCredentialCancellationException) {
            throw PasskeyException.NoCredential(cancel)
        } catch (none: NoCredentialException) {
            throw PasskeyException.NoCredential(none)
        } catch (e: GetCredentialException) {
            throw PasskeyException.Unknown(e)
        }

        val credential = response.credential as? PublicKeyCredential
            ?: throw PasskeyException.NoCredential(IllegalStateException("non-public-key credential"))

        return decodeAssertion(credential.authenticationResponseJson)
    }

    private fun encodeRequestJson(challenge: PasskeyChallenge): String {
        val json = buildJsonObject {
            put("challenge", challenge.challenge)
            put("rpId", challenge.rpId)
            challenge.userVerification?.let { put("userVerification", it) }
            challenge.timeout?.let { put("timeout", it) }
            put(
                "allowCredentials",
                buildJsonArray {
                    challenge.allowCredentials.forEach { allowed ->
                        addJsonObject {
                            put("type", allowed.type)
                            put("id", allowed.id)
                            put(
                                "transports",
                                buildJsonArray { allowed.transports.forEach { add(it) } },
                            )
                        }
                    }
                },
            )
        }
        return json.toString()
    }

    private fun decodeAssertion(responseJson: String): PasskeyAssertion {
        val parsed = try {
            JSON.parseToJsonElement(responseJson).jsonObject
        } catch (e: Exception) {
            throw PasskeyException.InvalidChallenge(e)
        }

        val response = parsed["response"]?.jsonObject
            ?: throw PasskeyException.InvalidChallenge(IllegalStateException("missing response"))

        return PasskeyAssertion(
            id = parsed.requireString("id"),
            rawId = parsed.requireString("rawId"),
            authenticatorAttachment = parsed["authenticatorAttachment"]?.optionalString(),
            clientDataJSON = response.requireString("clientDataJSON"),
            authenticatorData = response.requireString("authenticatorData"),
            signature = response.requireString("signature"),
            userHandle = response["userHandle"]?.optionalString(),
        )
    }

    private fun JsonObject.requireString(key: String): String =
        get(key)?.optionalString()
            ?: throw PasskeyException.InvalidChallenge(IllegalStateException("missing $key"))

    private fun JsonElement.optionalString(): String? = when (this) {
        is JsonNull -> null
        is JsonPrimitive -> content
        else -> null
    }

    sealed class PasskeyException(cause: Throwable?) : Exception(cause) {
        class NotSupported(cause: Throwable? = null) : PasskeyException(cause)
        class InvalidChallenge(cause: Throwable? = null) : PasskeyException(cause)
        class NoCredential(cause: Throwable? = null) : PasskeyException(cause)
        class Unknown(cause: Throwable? = null) : PasskeyException(cause)
    }

    private companion object {
        val JSON = Json { ignoreUnknownKeys = true }
    }
}
