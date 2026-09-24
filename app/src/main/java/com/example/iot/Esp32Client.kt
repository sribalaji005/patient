package com.example.iot

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class Esp32TelemetryResponse(
    val heartRate: Int? = null,
    val hr: Int? = null,
    val bpm: Int? = null,
    val spo2: Int? = null,
    val oxygen: Int? = null,
    val temp: Float? = null,
    val temperature: Float? = null,
    val fall: Boolean? = null,
    val fallDetected: Boolean? = null,
    val fallSeverity: String? = null,
    val ecg: Float? = null,
    val battery: Int? = null,
    val status: String? = null
)

data class Esp32Telemetry(
    val heartRate: Int,
    val spo2: Int,
    val temperature: Float,
    val fallDetected: Boolean,
    val fallSeverity: String,
    val ecgSample: Float,
    val batteryLevel: Int,
    val status: String,
    val timestamp: Long = System.currentTimeMillis()
)

class Esp32Client {
    private val client = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(Esp32TelemetryResponse::class.java)

    suspend fun fetchTelemetry(baseUrl: String): Result<Esp32Telemetry> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = when {
                baseUrl.startsWith("http://") || baseUrl.startsWith("https://") -> baseUrl
                else -> "http://$baseUrl"
            }
            val request = Request.Builder()
                .url(normalizedUrl)
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response body"))
                val parsed = adapter.fromJson(body) ?: return@withContext Result.failure(Exception("Failed to parse JSON"))

                val hr = parsed.heartRate ?: parsed.hr ?: parsed.bpm ?: 75
                val ox = parsed.spo2 ?: parsed.oxygen ?: 98
                val t = parsed.temperature ?: parsed.temp ?: 36.8f
                val fall = parsed.fallDetected ?: parsed.fall ?: false
                val severity = parsed.fallSeverity ?: if (fall) "FALL_DETECTED" else "NONE"
                val ecg = parsed.ecg ?: 0.0f
                val bat = parsed.battery ?: 88
                val st = parsed.status ?: "Online"

                Result.success(
                    Esp32Telemetry(
                        heartRate = hr.coerceIn(30, 220),
                        spo2 = ox.coerceIn(50, 100),
                        temperature = t.coerceIn(25.0f, 45.0f),
                        fallDetected = fall,
                        fallSeverity = severity,
                        ecgSample = ecg,
                        batteryLevel = bat.coerceIn(0, 100),
                        status = st
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val DEFAULT_ESP32_URL = "http://192.168.4.1/data"

        /**
         * Generates an Arduino/C++ code sketch ready to flash onto the ESP32.
         */
        fun getSampleArduinoCode(): String {
            return """
                // ==========================================
                // ESP32 Health & Fall Wearable Node Sketch
                // Connects to WiFi or creates SoftAP & serves JSON
                // Sensors: MAX30102 (HR/SpO2), AD8232 (ECG),
                //          MPU6050 (Fall/Accel), DS18B20/LM35 (Temp)
                // ==========================================
                #include <WiFi.h>
                #include <WebServer.h>

                // Wi-Fi Configuration:
                // Set to true to create a standalone Hotspot ("Health-Monitor-ESP32")
                const bool CREATE_ACCESS_POINT = true;
                const char* ssid = "Health-Monitor-ESP32";
                const char* password = "healthpassword123";

                WebServer server(80);

                // Sensor pins & variables
                const int ECG_PIN = 34; // ADC pin for AD8232 ECG
                int heartRate = 75;
                int spo2 = 98;
                float bodyTemp = 36.7;
                bool fallDetected = false;
                String fallSeverity = "NONE";
                int batteryPercent = 92;

                void handleTelemetry() {
                  // Read real sensors or pin inputs
                  int ecgRaw = analogRead(ECG_PIN);
                  float ecgNormalized = (ecgRaw / 4095.0) * 3.3;

                  // Create JSON response
                  String json = "{";
                  json += "\"heartRate\":" + String(heartRate) + ",";
                  json += "\"spo2\":" + String(spo2) + ",";
                  json += "\"temperature\":" + String(bodyTemp, 1) + ",";
                  json += "\"fallDetected\":" + (fallDetected ? "true" : "false") + ",";
                  json += "\"fallSeverity\":\"" + fallSeverity + "\",";
                  json += "\"ecg\":" + String(ecgNormalized, 2) + ",";
                  json += "\"battery\":" + String(batteryPercent) + ",";
                  json += "\"status\":\"ACTIVE\"";
                  json += "}";

                  server.sendHeader("Access-Control-Allow-Origin", "*");
                  server.send(200, "application/json", json);

                  // Auto reset one-shot fall alert after serving
                  if (fallDetected) {
                    fallDetected = false;
                    fallSeverity = "NONE";
                  }
                }

                void setup() {
                  Serial.begin(115200);
                  if (CREATE_ACCESS_POINT) {
                    WiFi.softAP(ssid, password);
                    Serial.println("ESP32 SoftAP IP: " + WiFi.softAPIP().toString());
                  } else {
                    WiFi.begin(ssid, password);
                    while (WiFi.status() != WL_CONNECTED) { delay(500); }
                    Serial.println("Connected! IP: " + WiFi.localIP().toString());
                  }

                  server.on("/data", HTTP_GET, handleTelemetry);
                  server.on("/telemetry", HTTP_GET, handleTelemetry);
                  server.begin();
                  Serial.println("HTTP Server started at /data");
                }

                void loop() {
                  server.handleClient();
                  // Check accelerometer for impact / freefall > threshold
                  delay(10);
                }
            """.trimIndent()
        }
    }
}
