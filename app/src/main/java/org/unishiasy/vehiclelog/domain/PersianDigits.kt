package org.unishiasy.vehiclelog.domain

import java.util.Locale

internal fun String.normalizeInputDigits(): String = map { character ->
    when (character) {
        in '۰'..'۹', in '٠'..'٩' -> '0' + character.digitToInt()
        else -> character
    }
}.joinToString("")

internal fun String.withPersianDigits(): String = map { character ->
    if (character in '0'..'9') '۰' + character.digitToInt() else character
}.joinToString("")

internal fun Long.toPersianGroupedDigits(): String =
    String.format(Locale.ROOT, "%,d", this).replace(',', '٬').withPersianDigits()

object PersianDigits {
    fun normalize(input: String): String = input.normalizeInputDigits()
    fun toPersianDigits(input: String): String = input.withPersianDigits()
    fun toPersianDigits(value: Long): String = value.toString().withPersianDigits()
    fun toPersianDigits(value: Int): String = value.toString().withPersianDigits()
    fun formatGrouped(value: Long): String = value.toPersianGroupedDigits()
}
