package org.unishiasy.vehiclelog.domain

internal fun String.normalizeInputDigits(): String = map { character ->
    when (character) {
        in '۰'..'۹', in '٠'..'٩' -> '0' + character.digitToInt()
        else -> character
    }
}.joinToString("")

internal fun String.withPersianDigits(): String = map { character ->
    if (character in '0'..'9') '۰' + character.digitToInt() else character
}.joinToString("")
