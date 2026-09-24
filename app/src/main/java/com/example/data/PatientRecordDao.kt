package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientRecordDao {
    @Query("SELECT * FROM patient_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<PatientRecord>>

    @Query("SELECT * FROM patient_records ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentRecords(limit: Int): Flow<List<PatientRecord>>

    @Query("SELECT * FROM patient_records WHERE fallDetected = 1 ORDER BY timestamp DESC")
    fun getFallAlerts(): Flow<List<PatientRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: PatientRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<PatientRecord>)

    @Delete
    suspend fun deleteRecord(record: PatientRecord)

    @Query("DELETE FROM patient_records")
    suspend fun clearAll()
}
