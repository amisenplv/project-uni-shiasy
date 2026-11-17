package org.unishiasy.vehiclelog.data

data class OdometerInput(
    val vehicleId: String,
    val occurredOn: String,
    val kilometers: Long,
)

sealed interface LinkedReadingEdit {
    data object Keep : LinkedReadingEdit
    data class Set(val kilometers: Long) : LinkedReadingEdit
    data object Remove : LinkedReadingEdit
}
