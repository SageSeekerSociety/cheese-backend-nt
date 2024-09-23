package org.rucca.cheese.common.helper

import java.time.LocalDateTime
import java.time.ZoneId

fun LocalDateTime.toEpochMilli(): Long = this.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
