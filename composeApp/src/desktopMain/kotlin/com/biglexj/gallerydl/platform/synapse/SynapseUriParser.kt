package com.biglexj.gallerydl.platform.synapse

import java.net.URI
import java.net.URLDecoder

/**
 * Parser de URIs y comandos del Aurora Synapse Protocol para Gallery-DL-GUI.
 * Interpreta esquemas `gallerydl://`, `aurora-synapse://gallerydl/`, URLs directas y payloads JSON.
 */
object SynapseUriParser {

    fun parse(rawInput: String?): SynapseAction? {
        if (rawInput.isNullOrBlank()) return null
        val input = rawInput.trim()

        if (input.equals("FOCUS", ignoreCase = true)) {
            return SynapseAction.Focus
        }

        // Direct HTTP / HTTPS URLs
        if (input.startsWith("http://", ignoreCase = true) || input.startsWith("https://", ignoreCase = true)) {
            return SynapseAction.Download(url = input, source = "DirectUrl")
        }

        // JSON format detection
        if (input.startsWith("{") && input.endsWith("}")) {
            return parseJsonPayload(input)
        }

        // URI schemes: gallerydl:// or aurora-synapse://
        if (input.startsWith("gallerydl://", ignoreCase = true) || input.startsWith("aurora-synapse://", ignoreCase = true)) {
            return parseUri(input)
        }

        return null
    }

    private fun parseUri(uriString: String): SynapseAction? {
        return runCatching {
            val uri = URI(uriString)
            val scheme = uri.scheme?.lowercase() ?: return null
            val host = uri.host?.lowercase() ?: ""
            val path = uri.path?.trimStart('/')?.lowercase() ?: ""
            val queryParams = parseQueryParams(uri.rawQuery)

            val actionName = when {
                scheme == "gallerydl" -> host.ifBlank { path }
                scheme == "aurora-synapse" && (host == "gallerydl" || host == "gallery-dl") -> path
                else -> host
            }

            when (actionName) {
                "download" -> {
                    val url = queryParams["url"] ?: return null
                    val dest = queryParams["dest"]
                    val silent = queryParams["silent"]?.toBooleanStrictOrNull() ?: false
                    val source = queryParams["source"] ?: "UriScheme"
                    SynapseAction.Download(url = url, destination = dest, silent = silent, source = source)
                }
                "enqueue" -> {
                    val url = queryParams["url"] ?: return null
                    val source = queryParams["source"] ?: "UriScheme"
                    SynapseAction.Enqueue(url = url, source = source)
                }
                "open_folder", "openfolder" -> {
                    val folderPath = queryParams["path"]
                    SynapseAction.OpenFolder(path = folderPath)
                }
                "focus" -> SynapseAction.Focus
                else -> null
            }
        }.getOrNull()
    }

    private fun unescapeJsonString(str: String): String {
        return str.replace("\\\\", "\\")
            .replace("\\\"", "\"")
            .replace("\\/", "/")
            .replace("\\t", "\t")
            .replace("\\n", "\n")
    }

    private fun parseJsonPayload(json: String): SynapseAction? {
        val actionPattern = """\"action\"\s*:\s*\"([^\"]+)\"""".toRegex()
        val urlPattern = """\"url\"\s*:\s*\"([^\"]+)\"""".toRegex()
        val destPattern = """\"(?:destination|dest)\"\s*:\s*\"([^\"]+)\"""".toRegex()
        val sourcePattern = """\"source\"\s*:\s*\"([^\"]+)\"""".toRegex()
        val silentPattern = """\"silent\"\s*:\s*(true|false)""".toRegex()

        val action = actionPattern.find(json)?.groupValues?.get(1)?.let { unescapeJsonString(it).lowercase() } ?: "download"
        val url = urlPattern.find(json)?.groupValues?.get(1)?.let { unescapeJsonString(it) }
        val dest = destPattern.find(json)?.groupValues?.get(1)?.let { unescapeJsonString(it) }
        val source = sourcePattern.find(json)?.groupValues?.get(1)?.let { unescapeJsonString(it) } ?: "JsonSynapse"
        val silent = silentPattern.find(json)?.groupValues?.get(1)?.toBooleanStrictOrNull() ?: false

        return when (action) {
            "download" -> url?.let { SynapseAction.Download(url = it, destination = dest, silent = silent, source = source) }
            "enqueue" -> url?.let { SynapseAction.Enqueue(url = it, source = source) }
            "open_folder", "openfolder" -> SynapseAction.OpenFolder(path = dest)
            "focus" -> SynapseAction.Focus
            else -> url?.let { SynapseAction.Download(url = it, destination = dest, silent = silent, source = source) }
        }
    }

    private fun parseQueryParams(rawQuery: String?): Map<String, String> {
        if (rawQuery.isNullOrBlank()) return emptyMap()
        return rawQuery.split("&").mapNotNull { pair ->
            val parts = pair.split("=", limit = 2)
            if (parts.isNotEmpty()) {
                val key = URLDecoder.decode(parts[0], "UTF-8").lowercase()
                val value = if (parts.size > 1) URLDecoder.decode(parts[1], "UTF-8") else ""
                key to value
            } else null
        }.toMap()
    }
}
