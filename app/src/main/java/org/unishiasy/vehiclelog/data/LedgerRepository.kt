package org.unishiasy.vehiclelog.data

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.unishiasy.vehiclelog.data.local.EntryEntity
import org.unishiasy.vehiclelog.data.local.HistoryEntry
import org.unishiasy.vehiclelog.data.local.OdometerReadingEntity
import org.unishiasy.vehiclelog.data.local.VehicleDatabase

enum class EntrySaveResult {
    Saved,
    NotFound,
    InvalidCategory,
    InvalidOccurrenceDate,
    InvalidAmount,
    InvalidFuelVolume,
    InvalidKilometers,
    MileageConflict,
    ReadingRequiresVehicleConfirmation,
    ConstraintViolation,
    StorageFailure,
}

private val unpricedServiceCategories = setOf("oil_change", "repair", "parts")
private val categories = unpricedServiceCategories + setOf("fuel", "insurance", "wash", "other")

internal fun allowsUnpricedEntry(category: String): Boolean = category in unpricedServiceCategories
internal fun isKnownEntryCategory(category: String): Boolean = category in categories

data class EntryWithReading(val entry: EntryEntity, val reading: OdometerReadingEntity?)

class LedgerRepository internal constructor(private val database: VehicleDatabase) {
    private val dao = database.ledgerDao()
    private val readings = database.odometerDao()

    fun observeForVehicle(vehicleId: String): Flow<List<EntryEntity>> = dao.observeForVehicle(vehicleId)

    internal fun observeMonthlyReport(vehicleId: String): Flow<VehicleMonthlyReport> =
        observeForVehicle(vehicleId).map { calculateMonthlyReport(vehicleId, it) }.flowOn(Dispatchers.Default)

    internal fun observeRecentForVehicle(vehicleId: String): Flow<List<EntryEntity>> = dao.observeRecentForVehicle(vehicleId)

    fun observeHistoryForVehicle(vehicleId: String): Flow<List<HistoryEntry>> = dao.observeHistoryForVehicle(vehicleId)

    suspend fun findById(id: UUID): EntryEntity? = dao.findById(id.toString())

    suspend fun findWithReading(id: UUID): EntryWithReading? = database.withTransaction {
        val entry = dao.findById(id.toString()) ?: return@withTransaction null
        EntryWithReading(entry, readings.findForEntry(entry.id))
    }

    suspend fun findReadingById(id: UUID): OdometerReadingEntity? = readings.findById(id.toString())

    fun observeCurrentMileage(vehicleId: String): Flow<Long?> = readings.observeCurrentMileage(vehicleId)

    fun observeStandaloneReadings(vehicleId: String): Flow<List<OdometerReadingEntity>> =
        readings.observeStandaloneForVehicle(vehicleId)

    suspend fun create(
        id: UUID,
        createdAtEpochMillis: Long,
        input: EntryInput,
        kilometers: Long? = null,
    ): EntrySaveResult = saveEntry(input) { notes, fuelVolumeMl ->
        val entry = EntryEntity(
            id = id.toString(),
            vehicleId = input.vehicleId,
            category = input.category,
            occurredOn = input.occurredOn,
            createdAtEpochMillis = createdAtEpochMillis,
            notes = notes,
            amountRials = input.amountRials,
            fuelVolumeMl = fuelVolumeMl,
            stationName = input.stationName?.trim()?.takeIf { it.isNotBlank() },
            isFullTank = input.isFullTank,
        )
        val existing = dao.findById(entry.id)
        if (existing != null) {
            val existingReading = readings.findForEntry(entry.id)
            val sameReading = if (existingReading == null) {
                kilometers == null
            } else {
                kilometers != null && existingReading == linkedReading(entry.id, input, kilometers, existingReading.id)
            }
            return@saveEntry if (existing == entry && sameReading) {
                EntrySaveResult.Saved
            } else {
                EntrySaveResult.ConstraintViolation
            }
        }

        val reading = kilometers?.let { linkedReading(entry.id, input, it) }
        if (reading != null) {
            readingError(reading)?.let { return@saveEntry it }
        }
        dao.insert(entry)
        reading?.let { readings.insert(it) }
        EntrySaveResult.Saved
    }

    suspend fun update(
        id: UUID,
        input: EntryInput,
        readingEdit: LinkedReadingEdit = LinkedReadingEdit.Keep,
    ): EntrySaveResult = saveEntry(input) { notes, fuelVolumeMl ->
        val entryId = id.toString()
        val existing = dao.findById(entryId) ?: return@saveEntry EntrySaveResult.NotFound
        val existingReading = readings.findForEntry(entryId)
        val reading = when (readingEdit) {
            LinkedReadingEdit.Keep -> existingReading?.let {
                if (existing.vehicleId != input.vehicleId) {
                    return@saveEntry EntrySaveResult.ReadingRequiresVehicleConfirmation
                }
                linkedReading(entryId, input, it.kilometers, it.id)
            }
            is LinkedReadingEdit.Set -> linkedReading(entryId, input, readingEdit.kilometers, existingReading?.id)
            LinkedReadingEdit.Remove -> null
        }
        if (reading != null && reading != existingReading) {
            readingError(reading)?.let { return@saveEntry it }
        }
        val updated = dao.update(
            id = entryId,
            vehicleId = input.vehicleId,
            category = input.category,
            occurredOn = input.occurredOn,
            notes = notes,
            amountRials = input.amountRials,
            fuelVolumeMl = fuelVolumeMl,
            stationName = input.stationName?.trim()?.takeIf { it.isNotBlank() },
            isFullTank = input.isFullTank,
        )
        if (updated == 0) return@saveEntry EntrySaveResult.NotFound
        when {
            reading == existingReading -> Unit
            reading == null -> readings.deleteForEntry(entryId)
            existingReading == null -> readings.insert(reading)
            else -> readings.update(reading)
        }
        EntrySaveResult.Saved
    }

    suspend fun deleteEntry(id: UUID): EntrySaveResult = write {
        if (dao.delete(id.toString()) == 0) EntrySaveResult.NotFound else EntrySaveResult.Saved
    }

    suspend fun deleteEntry(vehicleId: String, id: UUID): EntrySaveResult = write {
        if (dao.deleteForVehicle(id.toString(), vehicleId) == 0) EntrySaveResult.NotFound else EntrySaveResult.Saved
    }

    suspend fun createStandaloneReading(id: UUID, input: OdometerInput): EntrySaveResult =
        saveStandalone(input) {
            val reading = OdometerReadingEntity(id.toString(), input.vehicleId, input.occurredOn, input.kilometers, null)
            val existing = readings.findById(reading.id)
            if (existing != null) {
                return@saveStandalone if (existing == reading) EntrySaveResult.Saved else EntrySaveResult.ConstraintViolation
            }
            readingError(reading)?.let { return@saveStandalone it }
            readings.insert(reading)
            EntrySaveResult.Saved
        }

    suspend fun updateStandaloneReading(id: UUID, input: OdometerInput): EntrySaveResult =
        saveStandalone(input) {
            val existing = readings.findById(id.toString()) ?: return@saveStandalone EntrySaveResult.NotFound
            if (existing.entryId != null) return@saveStandalone EntrySaveResult.NotFound
            val reading = existing.copy(
                vehicleId = input.vehicleId,
                occurredOn = input.occurredOn,
                kilometers = input.kilometers,
            )
            readingError(reading)?.let { return@saveStandalone it }
            readings.update(reading)
            EntrySaveResult.Saved
        }

    suspend fun deleteStandaloneReading(id: UUID): EntrySaveResult = write {
        if (readings.deleteStandalone(id.toString()) == 0) EntrySaveResult.NotFound else EntrySaveResult.Saved
    }

    private fun linkedReading(
        entryId: String,
        input: EntryInput,
        kilometers: Long,
        readingId: String? = null,
    ) = OdometerReadingEntity(
        id = readingId ?: UUID.randomUUID().toString(),
        vehicleId = input.vehicleId,
        occurredOn = input.occurredOn,
        kilometers = kilometers,
        entryId = entryId,
    )

    // Call inside the write transaction so no other save can invalidate these bounds.
    private suspend fun readingError(reading: OdometerReadingEntity): EntrySaveResult? {
        if (reading.kilometers < 0) return EntrySaveResult.InvalidKilometers
        val lower = readings.earlierMaximum(reading.vehicleId, reading.occurredOn, reading.id)
        val upper = readings.laterMinimum(reading.vehicleId, reading.occurredOn, reading.id)
        return if ((lower != null && reading.kilometers < lower) || (upper != null && reading.kilometers > upper)) {
            EntrySaveResult.MileageConflict
        } else {
            null
        }
    }

    private suspend fun saveStandalone(input: OdometerInput, operation: suspend () -> EntrySaveResult): EntrySaveResult {
        if (!isValidOccurrenceDate(input.occurredOn)) return EntrySaveResult.InvalidOccurrenceDate
        if (input.kilometers < 0) return EntrySaveResult.InvalidKilometers
        return write(operation)
    }

    private suspend fun saveEntry(
        input: EntryInput,
        operation: suspend (notes: String?, fuelVolumeMl: Long?) -> EntrySaveResult,
    ): EntrySaveResult {
        if (!isKnownEntryCategory(input.category)) return EntrySaveResult.InvalidCategory
        if (!isValidOccurrenceDate(input.occurredOn)) return EntrySaveResult.InvalidOccurrenceDate
        val amount = input.amountRials
        val invalidAmount = if (amount == null) !allowsUnpricedEntry(input.category) else amount <= 0
        if (invalidAmount) return EntrySaveResult.InvalidAmount

        val liters = input.fuelVolumeLiters
        val fuelVolumeMl = if (input.category == "fuel") {
            liters?.let(::parseFuelVolumeMl) ?: return EntrySaveResult.InvalidFuelVolume
        } else {
            if (liters != null) return EntrySaveResult.InvalidFuelVolume
            null
        }

        return write { operation(input.notes?.takeUnless { it.isBlank() }, fuelVolumeMl) }
    }

    private suspend fun write(operation: suspend () -> EntrySaveResult): EntrySaveResult =
        try {
            database.withTransaction { operation() }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: SQLiteConstraintException) {
            EntrySaveResult.ConstraintViolation
        } catch (_: SQLiteException) {
            EntrySaveResult.StorageFailure
        } catch (_: IllegalStateException) {
            EntrySaveResult.StorageFailure
        }
}
