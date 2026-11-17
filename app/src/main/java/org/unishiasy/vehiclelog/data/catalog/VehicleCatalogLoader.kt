package org.unishiasy.vehiclelog.data.catalog

import java.io.IOException
import java.io.Reader
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal object VehicleCatalogLoader {
    private val keyPattern = Regex("[a-z0-9][a-z0-9._-]*")

    fun loadIfAvailable(assetPath: String?, openReader: (String) -> Reader): VehicleCatalog? {
        if (assetPath.isNullOrBlank()) return null
        return try {
            openReader(assetPath).use(::load).takeUnless { it.isEmpty }
        } catch (_: IOException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    fun load(reader: Reader): VehicleCatalog {
        val document = reader.readText()
        val root = try {
            Json.parseToJsonElement(document) as? JsonObject
        } catch (error: SerializationException) {
            throw IllegalArgumentException("Malformed vehicle catalog JSON.", error)
        } ?: throw IllegalArgumentException("Vehicle catalog must be a JSON object.")

        val version = root["schemaVersion"] as? JsonPrimitive
        require(version != null && !version.isString && version.content == "1") {
            "Vehicle catalog schemaVersion must be 1."
        }
        val records = root["entries"] as? JsonArray
            ?: throw IllegalArgumentException("Vehicle catalog entries must be an array.")

        val keys = mutableSetOf<String>()
        val entries = records.mapIndexed { index, record ->
            val path = "entries[$index]"
            val entry = record as? JsonObject
                ?: throw IllegalArgumentException("$path must be an object.")
            val key = requiredText(entry["key"], "$path.key")
            require(keyPattern.matches(key)) { "$path.key is not a valid catalog key." }
            require(keys.add(key)) { "Duplicate vehicle catalog key at $path.key: $key" }

            VehicleCatalogEntry(
                key = key,
                manufacturer = requiredText(entry["manufacturer"], "$path.manufacturer"),
                model = requiredText(entry["model"], "$path.model"),
                variant = entry["variant"]?.takeUnless { it == JsonNull }?.let {
                    requiredText(it, "$path.variant")
                },
                aliases = entry["aliases"]?.let { value ->
                    val aliases = value as? JsonArray
                        ?: throw IllegalArgumentException("$path.aliases must be an array.")
                    aliases.mapIndexed { aliasIndex, alias ->
                        requiredText(alias, "$path.aliases[$aliasIndex]")
                    }
                }.orEmpty(),
            )
        }
        return VehicleCatalog(entries)
    }

    private fun requiredText(value: JsonElement?, field: String): String {
        val text = value as? JsonPrimitive
        require(text != null && text.isString && text.content.isNotBlank()) {
            "$field must be a nonblank string."
        }
        return text.content
    }
}
