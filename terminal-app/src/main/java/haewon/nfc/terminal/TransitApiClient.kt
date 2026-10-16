package haewon.nfc.terminal

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class TapResult(
    val success: Boolean,
    val cardId: String,
    val fare: Int,
    val balance: Int,
    val message: String
)

object TransitApiClient {
    // 반드시 PC의 실제 IPv4 주소로 변경
    private const val BASE_URL = "http://192.168.45.34:8080"

    fun tap(cardId: String): TapResult {
        val url = URL("$BASE_URL/api/transit/tap")

        val connection =
            url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.doOutput = true
            connection.setRequestProperty(
                "Content-Type",
                "application/json; charset=UTF-8"
            )

            val requestBody = JSONObject().apply {
                put("cardId", cardId)
            }

            connection.outputStream.use { output ->
                output.write(
                    requestBody.toString()
                        .toByteArray(Charsets.UTF_8)
                )
            }

            val statusCode = connection.responseCode

            val stream = if (statusCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val responseText = stream?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()
            if (statusCode !in 200..299) {
                val errorMessage = try {
                    val json = JSONObject(responseText)
                    json.optString("message", "서버 요청에 실패했습니다.")
                } catch (e: Exception) {
                    "서버 요청에 실패했습니다. (HTTP $statusCode)"
                }

                throw IllegalStateException(errorMessage)
            }

            // HTTP 200~299인 경우에만 정상 응답 파싱
            val json = JSONObject(responseText)

            return TapResult(
                success = json.getBoolean("success"),
                cardId = json.getString("cardId"),
                fare = json.getInt("fare"),
                balance = json.getInt("balance"),
                message = json.getString("message")
            )
        } finally {
            connection.disconnect()
        }
    }
}