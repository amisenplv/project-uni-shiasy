package org.unishiasy.vehiclelog.data.backup

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.time.Instant
import java.time.format.DateTimeParseException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.unishiasy.vehiclelog.data.local.EntryEntity
import org.unishiasy.vehiclelog.data.local.OdometerReadingEntity
import org.unishiasy.vehiclelog.data.local.ReminderEntity
import org.unishiasy.vehiclelog.data.local.VehicleEntity

internal object BackupCodec {
    const val MAX_BYTES = 4 * 1024 * 1024
    const val MAX_VEHICLES = 100
    const val MAX_ENTRIES = 20_000
    const val MAX_READINGS = 20_000
    const val MAX_REMINDERS = 5_000
    private const val VERSION = 1L
    private const val CURRENCY = "legacy_irr"

    fun decode(bytes: ByteArray): BackupResult<BackupSnapshot> = checked {
        checkSnapshot(bytes.size <= MAX_BYTES, BackupError.SizeLimit)
        val text = Charsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString()
        StrictBackupJson(text).validate()
        val root = Json.parseToJsonElement(text).asObject()
        checkSnapshot(root.required("formatVersion").integer() == VERSION, BackupError.UnsupportedVersion)
        val envelope = Fields(root, "formatVersion", "exportedAt", "currencyUnit", "vehicles", "entries", "readings", "reminders")
        checkSnapshot(envelope.text("currencyUnit") == CURRENCY, BackupError.UnsupportedCurrency)
        val timestamp = envelope.text("exportedAt")
        val exportedAt = Instant.parse(timestamp)
        checkSnapshot(exportedAt.toString() == timestamp, BackupError.InvalidField)
        val vehicles = envelope.collection("vehicles", MAX_VEHICLES)
        val entries = envelope.collection("entries", MAX_ENTRIES)
        val readings = envelope.collection("readings", MAX_READINGS)
        val reminders = envelope.collection("reminders", MAX_REMINDERS)
        BackupSnapshot(
            exportedAt,
            vehicles.map(::readVehicle).sortedBy { it.id },
            entries.map(::readEntry).sortedBy { it.id },
            readings.map(::readReading).sortedBy { it.id },
            reminders.map(::readReminder).sortedBy { it.id },
        ).also(::validateSnapshot)
    }

    fun encode(snapshot: BackupSnapshot): BackupResult<ByteArray> = checked {
        validateSnapshot(snapshot)
        val output = StringBuilder()
        output.append("""{"formatVersion":$VERSION,"exportedAt":""")
        output.append(JsonPrimitive(snapshot.exportedAt.toString()))
        output.append(",\"currencyUnit\":").append(JsonPrimitive(CURRENCY))
        output.appendRows("vehicles", snapshot.vehicles.sortedBy { it.id }) {
            record(
                "id" to JsonPrimitive(it.id), "kind" to JsonPrimitive(it.kind), "displayLabel" to JsonPrimitive(it.displayLabel),
                "catalogKey" to JsonPrimitive(it.catalogKey), "productionYear" to JsonPrimitive(it.productionYear),
                "productionYearCalendar" to JsonPrimitive(it.productionYearCalendar),
            )
        }
        output.appendRows("entries", snapshot.entries.sortedBy { it.id }) {
            record(
                "id" to JsonPrimitive(it.id), "vehicleId" to JsonPrimitive(it.vehicleId), "category" to JsonPrimitive(it.category),
                "occurredOn" to JsonPrimitive(it.occurredOn), "createdAtEpochMillis" to JsonPrimitive(it.createdAtEpochMillis),
                "notes" to JsonPrimitive(it.notes), "amountRials" to JsonPrimitive(it.amountRials), "fuelVolumeMl" to JsonPrimitive(it.fuelVolumeMl),
                "stationName" to JsonPrimitive(it.stationName), "isFullTank" to JsonPrimitive(it.isFullTank),
            )
        }
        output.appendRows("readings", snapshot.readings.sortedBy { it.id }) {
            record(
                "id" to JsonPrimitive(it.id), "vehicleId" to JsonPrimitive(it.vehicleId), "occurredOn" to JsonPrimitive(it.occurredOn),
                "kilometers" to JsonPrimitive(it.kilometers), "entryId" to JsonPrimitive(it.entryId),
            )
        }
        output.appendRows("reminders", snapshot.reminders.sortedBy { it.id }) {
            record(
                "id" to JsonPrimitive(it.id), "vehicleId" to JsonPrimitive(it.vehicleId), "title" to JsonPrimitive(it.title),
                "dueOn" to JsonPrimitive(it.dueOn), "dueKilometers" to JsonPrimitive(it.dueKilometers), "state" to JsonPrimitive(it.state),
            )
        }
        output.append('}').toString().toByteArray(Charsets.UTF_8).also {
            checkSnapshot(it.size <= MAX_BYTES, BackupError.SizeLimit)
        }
    }

    private fun readVehicle(row: JsonElement): VehicleEntity {
        val f = Fields(row.asObject(), "id", "kind", "displayLabel", "catalogKey", "productionYear", "productionYearCalendar")
        val year = f.optionalInteger("productionYear")
        checkSnapshot(year == null || year in 1..9999, BackupError.InvalidVehicle)
        return VehicleEntity(f.text("id"), f.text("kind"), f.text("displayLabel"), f.optionalText("catalogKey"),
            year?.toInt(), f.optionalText("productionYearCalendar"))
    }

    private fun readEntry(row: JsonElement): EntryEntity {
        val f = Fields(
            row.asObject(),
            required = setOf("id", "vehicleId", "category", "occurredOn", "createdAtEpochMillis", "notes", "amountRials", "fuelVolumeMl"),
            optional = setOf("stationName", "isFullTank"),
        )
        return EntryEntity(
            f.text("id"), f.text("vehicleId"), f.text("category"), f.text("occurredOn"),
            f.integer("createdAtEpochMillis"), f.optionalText("notes"), f.optionalInteger("amountRials"), f.optionalInteger("fuelVolumeMl"),
            f.optionalText("stationName"), f.boolean("isFullTank", default = false),
        )
    }

    private fun readReading(row: JsonElement): OdometerReadingEntity {
        val f = Fields(row.asObject(), "id", "vehicleId", "occurredOn", "kilometers", "entryId")
        return OdometerReadingEntity(f.text("id"), f.text("vehicleId"), f.text("occurredOn"), f.integer("kilometers"), f.optionalText("entryId"))
    }

    private fun readReminder(row: JsonElement): ReminderEntity {
        val f = Fields(row.asObject(), "id", "vehicleId", "title", "dueOn", "dueKilometers", "state")
        return ReminderEntity(f.text("id"), f.text("vehicleId"), f.text("title"), f.optionalText("dueOn"), f.optionalInteger("dueKilometers"), f.text("state"))
    }

    private fun record(vararg fields: Pair<String, JsonPrimitive>) = JsonObject(linkedMapOf(*fields))

    private fun <T> StringBuilder.appendRows(name: String, rows: List<T>, toJson: (T) -> JsonObject) {
        append(",\"").append(name).append("\":[")
        rows.forEachIndexed { index, row ->
            if (index > 0) append(',')
            append(toJson(row))
            checkSnapshot(length <= MAX_BYTES, BackupError.SizeLimit)
        }
        append(']')
    }

    private inline fun <T> checked(action: () -> T): BackupResult<T> = try {
        BackupResult.Success(action())
    } catch (failure: InvalidSnapshot) {
        BackupResult.Failure(failure.reason)
    } catch (_: SerializationException) {
        BackupResult.Failure(BackupError.MalformedJson)
    } catch (_: CharacterCodingException) {
        BackupResult.Failure(BackupError.MalformedJson)
    } catch (_: DateTimeParseException) {
        BackupResult.Failure(BackupError.InvalidField)
    }
}

private class Fields(
    private val objectValue: JsonObject,
    required: Set<String>,
    optional: Set<String> = emptySet(),
) {
    constructor(objectValue: JsonObject, vararg names: String) : this(objectValue, names.toSet(), emptySet())

    init {
        val allAllowed = required + optional
        checkSnapshot(objectValue.keys.all { it in allAllowed }, BackupError.UnknownField)
        checkSnapshot(objectValue.keys.containsAll(required), BackupError.MissingField)
    }

    fun text(key: String): String = objectValue.required(key).text()
    fun integer(key: String): Long = objectValue.required(key).integer()
    fun optionalText(key: String): String? = objectValue[key]?.takeUnless { it == JsonNull }?.text()
    fun optionalInteger(key: String): Long? = objectValue[key]?.takeUnless { it == JsonNull }?.integer()
    fun boolean(key: String, default: Boolean = false): Boolean {
        val element = objectValue[key] ?: return default
        return element.boolean()
    }
    fun collection(key: String, max: Int): JsonArray {
        val array = objectValue.required(key) as? JsonArray ?: throw InvalidSnapshot(BackupError.InvalidField)
        checkSnapshot(array.size <= max, BackupError.CollectionLimit)
        return array
    }
}

private fun JsonObject.required(key: String): JsonElement = get(key) ?: throw InvalidSnapshot(BackupError.MissingField)
private fun JsonElement.asObject(): JsonObject = this as? JsonObject ?: throw InvalidSnapshot(BackupError.InvalidField)
private fun JsonElement.text(): String =
    (this as? JsonPrimitive)?.takeIf { it.isString }?.content ?: throw InvalidSnapshot(BackupError.InvalidField)
private fun JsonElement.integer(): Long =
    (this as? JsonPrimitive)?.takeUnless { it.isString }?.content?.toLongOrNull() ?: throw InvalidSnapshot(BackupError.InvalidField)
private fun JsonElement.boolean(): Boolean =
    (this as? JsonPrimitive)?.takeIf { !it.isString }?.content?.toBooleanStrictOrNull() ?: throw InvalidSnapshot(BackupError.InvalidField)

