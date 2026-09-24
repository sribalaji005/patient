package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patient_records")
data class PatientRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patientName: String = "John Doe",
    val patientId: String = "PT-8092",
    val timestamp: Long = System.currentTimeMillis(),
    val heartRate: Int = 72,
    val spo2: Int = 98,
    val temperature: Float = 36.8f,
    val fallDetected: Boolean = false,
    val fallSeverity: String = "NONE", // NONE, IMPACT, FALL_DETECTED
    val ecgRhythm: String = "Normal Sinus Rhythm",
    val source: String = "ESP32", // ESP32, EXCEL_IMPORT, SIMULATION
    val notes: String = ""
)
