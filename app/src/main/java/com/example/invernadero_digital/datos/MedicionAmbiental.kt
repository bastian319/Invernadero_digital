package com.example.invernadero_digital.datos

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mediciones")
data class MedicionAmbiental(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val temperaturaCelsius: Double,
    val humedadAire: Double,
    val humedadSuelo: Double,
    val nivelLuz: Int,
    val fechaHora: Long = System.currentTimeMillis()
)