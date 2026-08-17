package com.biglexj.gallerydl.features.synapse

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Envoltorio Estándar de Acción (Synapse Action Envelope).
 * Conforme a la especificación oficial Aurora Synapse Protocol (v1.0.0, Nivel 4).
 */
@Serializable
data class SynapseEnvelope(
    @SerialName("synapse_version")
    val synapseVersion: String = "1.0",

    @SerialName("source_app")
    val sourceApp: String,

    @SerialName("target_app")
    val targetApp: String = "prisma",

    @SerialName("action")
    val action: String,

    @SerialName("timestamp_utc")
    val timestampUtc: String? = null,

    @SerialName("idempotency_key")
    val idempotencyKey: String? = null,

    @SerialName("payload")
    val payload: JsonObject? = null,
) {
    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }

        fun fromJson(rawJson: String): SynapseEnvelope? = runCatching {
            json.decodeFromString<SynapseEnvelope>(rawJson)
        }.getOrNull()

        fun toJson(envelope: SynapseEnvelope): String = json.encodeToString(serializer(), envelope)
    }

    fun payloadString(key: String): String? = payload?.get(key)?.jsonPrimitive?.content

    fun payloadBoolean(key: String, default: Boolean = false): Boolean =
        payload?.get(key)?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: default

    fun payloadInt(key: String): Int? =
        payload?.get(key)?.jsonPrimitive?.content?.toIntOrNull()
}
