package com.example.data

import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PatientRepository(private val dao: PatientRecordDao) {

    val allRecords: Flow<List<PatientRecord>> = dao.getAllRecords()
    val fallAlerts: Flow<List<PatientRecord>> = dao.getFallAlerts()

    fun getRecentRecords(limit: Int = 50): Flow<List<PatientRecord>> = dao.getRecentRecords(limit)

    suspend fun insertRecord(record: PatientRecord): Long = dao.insertRecord(record)

    suspend fun insertRecords(records: List<PatientRecord>) = dao.insertRecords(records)

    suspend fun deleteRecord(record: PatientRecord) = dao.deleteRecord(record)

    suspend fun clearAll() = dao.clearAll()

    /**
     * Converts records to a CSV string compatible with Google Sheets and Excel.
     */
    fun exportToCsv(records: List<PatientRecord>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val sb = StringBuilder()
        sb.append("Timestamp,Formatted Date,Patient Name,Patient ID,Heart Rate (BPM),SpO2 (%),Temperature (°C),Fall Detected,Fall Severity,ECG Rhythm,Source,Notes\n")
        records.forEach { r ->
            val dateStr = dateFormat.format(Date(r.timestamp))
            val cleanNotes = r.notes.replace("\"", "\"\"")
            val cleanRhythm = r.ecgRhythm.replace("\"", "\"\"")
            sb.append("${r.timestamp},\"$dateStr\",\"${r.patientName}\",\"${r.patientId}\",${r.heartRate},${r.spo2},${r.temperature},${r.fallDetected},\"${r.fallSeverity}\",\"$cleanRhythm\",\"${r.source}\",\"$cleanNotes\"\n")
        }
        return sb.toString()
    }

    /**
     * Parses CSV text exported from Excel or Google Sheets and merges records.
     */
    fun parseCsv(csvText: String, defaultPatientName: String = "John Doe", defaultPatientId: String = "PT-8092"): List<PatientRecord> {
        val lines = csvText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.size <= 1) return emptyList()

        val records = mutableListOf<PatientRecord>()
        val header = lines.first().lowercase()

        // Check if header contains expected columns or is generic
        val isFirstLineHeader = header.contains("heart") || header.contains("bpm") || header.contains("timestamp") || header.contains("spo2")
        val dataLines = if (isFirstLineHeader) lines.drop(1) else lines

        for (line in dataLines) {
            val tokens = parseCsvLine(line)
            if (tokens.isEmpty()) continue

            try {
                // Try flexible column parsing
                var timestamp = System.currentTimeMillis()
                var pName = defaultPatientName
                var pId = defaultPatientId
                var hr = 75
                var spo2 = 98
                var temp = 36.8f
                var fall = false
                var fallSev = "NONE"
                var rhythm = "Normal Sinus Rhythm"
                var notes = ""

                if (tokens.size >= 7) {
                    // Standard export format:
                    // 0: Timestamp, 1: Date, 2: Name, 3: ID, 4: HR, 5: SpO2, 6: Temp, 7: Fall, 8: Sev, 9: Rhythm, 10: Source, 11: Notes
                    tokens[0].toLongOrNull()?.let { timestamp = it }
                    if (tokens.size > 2 && tokens[2].isNotBlank()) pName = tokens[2]
                    if (tokens.size > 3 && tokens[3].isNotBlank()) pId = tokens[3]
                    tokens.getOrNull(4)?.toIntOrNull()?.let { hr = it }
                    tokens.getOrNull(5)?.toIntOrNull()?.let { spo2 = it }
                    tokens.getOrNull(6)?.toFloatOrNull()?.let { temp = it }
                    if (tokens.size > 7) {
                        fall = tokens[7].equals("true", ignoreCase = true) || tokens[7] == "1"
                    }
                    if (tokens.size > 8 && tokens[8].isNotBlank()) fallSev = tokens[8]
                    if (tokens.size > 9 && tokens[9].isNotBlank()) rhythm = tokens[9]
                    if (tokens.size > 11 && tokens[11].isNotBlank()) notes = tokens[11]
                } else if (tokens.size in 3..6) {
                    // Simple format: HeartRate, SpO2, Temp, [Fall]
                    tokens[0].toIntOrNull()?.let { hr = it }
                    tokens.getOrNull(1)?.toIntOrNull()?.let { spo2 = it }
                    tokens.getOrNull(2)?.toFloatOrNull()?.let { temp = it }
                    if (tokens.size > 3) {
                        fall = tokens[3].equals("true", ignoreCase = true) || tokens[3] == "1"
                        if (fall) fallSev = "IMPACT"
                    }
                }

                records.add(
                    PatientRecord(
                        patientName = pName,
                        patientId = pId,
                        timestamp = timestamp,
                        heartRate = hr.coerceIn(30, 220),
                        spo2 = spo2.coerceIn(50, 100),
                        temperature = temp.coerceIn(30.0f, 45.0f),
                        fallDetected = fall,
                        fallSeverity = fallSev,
                        ecgRhythm = rhythm,
                        source = "EXCEL_IMPORT",
                        notes = notes
                    )
                )
            } catch (_: Exception) {
                // Skip malformed row
            }
        }
        return records
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = java.lang.StringBuilder()
        var inQuotes = false

        for (ch in line) {
            when (ch) {
                '"' -> inQuotes = !inQuotes
                ',' -> {
                    if (inQuotes) {
                        current.append(ch)
                    } else {
                        result.add(current.toString().trim())
                        current.setLength(0)
                    }
                }
                else -> current.append(ch)
            }
        }
        result.add(current.toString().trim())
        return result
    }
}
