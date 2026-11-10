package com.example.invernadero_digital.datos

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MedicionAmbiental::class],
    version = 1,
    exportSchema = false
)
abstract class BaseDatosInvernadero : RoomDatabase() {
    abstract fun medicionDao(): MedicionDao

    companion object {
        @Volatile
        private var instancia: BaseDatosInvernadero? = null

        fun obtener(context: Context): BaseDatosInvernadero =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    BaseDatosInvernadero::class.java,
                    "invernadero.db"
                ).build().also { instancia = it }
            }
    }
}