package org.unishiasy.vehiclelog.data.catalog

import java.util.Locale

internal data class VehicleCatalogEntry(
    val key: String,
    val manufacturer: String,
    val model: String,
    val variant: String? = null,
    val aliases: List<String> = emptyList(),
) {
    val displayLabel: String get() = listOfNotNull(manufacturer, model, variant).joinToString(" ")
}

internal class VehicleCatalog(entries: List<VehicleCatalogEntry>) {
    private val index = entries.sortedBy { it.key }.map { entry ->
        entry to normalizeVehicleSearch((listOf(entry.displayLabel) + entry.aliases).joinToString(" "))
    }

    val isEmpty: Boolean get() = index.isEmpty()

    fun search(query: String): List<VehicleCatalogEntry> {
        val normalized = normalizeVehicleSearch(query)
        if (normalized.isEmpty()) return index.map { it.first }

        val terms = normalized.split(' ')
        return index.mapNotNull { (entry, text) ->
            entry.takeIf { terms.all { text.contains(it) } }
        }
    }
}

internal fun normalizeVehicleSearch(text: String): String = buildString {
    var spacePending = false
    for (character in text) {
        if (character == '\u200C') continue
        if (character.isWhitespace()) {
            spacePending = isNotEmpty()
            continue
        }
        if (spacePending) append(' ')
        spacePending = false
        append(
            when (character) {
                'ي', 'ى' -> 'ی'
                'ك' -> 'ک'
                in '۰'..'۹' -> '0' + (character - '۰')
                in '٠'..'٩' -> '0' + (character - '٠')
                else -> character
            },
        )
    }
}.lowercase(Locale.ROOT)
