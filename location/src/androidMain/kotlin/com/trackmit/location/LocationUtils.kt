package com.trackmit.location

fun convertToLocation(androidLoc: android.location.Location, timeProvider: TimeProvider): Location =
    Location(
        latitude = androidLoc.latitude,
        longitude = androidLoc.longitude,
        accuracy = if (androidLoc.hasAccuracy()) androidLoc.accuracy.toDouble() else null,
        speed = if (androidLoc.hasSpeed()) androidLoc.speed.toDouble() else null,
        bearing = if (androidLoc.hasBearing()) androidLoc.bearing.toDouble() else null,
        timestamp = timeProvider.currentTimestamp()
    )
