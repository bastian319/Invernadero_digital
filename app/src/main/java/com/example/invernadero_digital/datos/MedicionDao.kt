package com.example.invernadero_digital.datos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicionDao {
    @Insert
    suspend fun insertar(medicion: MedicionAmbiental)

    @Query("SELECT * FROM mediciones ORDER BY fechaHora DESC")
    fun observarTodas(): Flow<List<MedicionAmbiental>>
}