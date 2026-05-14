package com.myhealth.app.data.room

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "symptom_log")
data class SymptomLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bodyLocation: String,
    val painScale: Int,
    val durationHours: Double? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Dao
interface SymptomLogDao {
    @Insert
    suspend fun insert(entry: SymptomLogEntity): Long

    @Query("SELECT * FROM symptom_log ORDER BY createdAt DESC")
    fun getAll(): Flow<List<SymptomLogEntity>>

    @Query("DELETE FROM symptom_log WHERE id = :id")
    suspend fun deleteById(id: Long)
}
