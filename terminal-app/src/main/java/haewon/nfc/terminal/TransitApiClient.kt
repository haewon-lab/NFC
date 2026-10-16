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

data class CardBalanceResult(
    val cardId: String,
    val balance: Int
)

data class TransitHistoryItem(
    val transactionId: Long,
    val cardId: String,
    val transactionType: String,
    val timestamp: String,
    val amount: Int,
    val balance: Int,
    val success: Boolean,
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

    fun getCardBalance(cardId: String): CardBalanceResult {

        val encodedCardId = java.net.URLEncoder.encode(
            cardId,
            "UTF-8"
        )

        val url = URL(
            "$BASE_URL/api/transit/cards/$encodedCardId"
        )

        val connection = url.openConnection()
                as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

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
                    json.optString(
                        "message",
                        "카드 조회에 실패했습니다."
                    )
                } catch (e: Exception) {
                    "카드 조회에 실패했습니다. (HTTP $statusCode)"
                }

                throw IllegalStateException(errorMessage)
            }

            val json = JSONObject(responseText)

            return CardBalanceResult(
                cardId = json.getString("cardId"),
                balance = json.getInt("balance")
            )

        } finally {
            connection.disconnect()
        }
    }

    fun getHistory(cardId: String): List<TransitHistoryItem> {
        val encodedCardId = java.net.URLEncoder.encode(
            cardId,
            "UTF-8"
        )

        val connection = URL(
            "$BASE_URL/api/transit/cards/$encodedCardId/history"
        ).openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

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
                val message = try {
                    org.json.JSONObject(responseText)
                        .optString("message", "거래 내역 조회 실패")
                } catch (e: Exception) {
                    "거래 내역 조회 실패 (HTTP $statusCode)"
                }

                throw IllegalStateException(message)
            }

            val array = org.json.JSONArray(responseText)
            val history = mutableListOf<TransitHistoryItem>()

            for (i in 0 until array.length()) {
                val json = array.getJSONObject(i)

                history.add(
                    TransitHistoryItem(
                        transactionId = json.getLong("transactionId"),
                        cardId = json.getString("cardId"),
                        transactionType = json.getString("transactionType"),
                        timestamp = json.getString("timestamp"),
                        amount = json.getInt("amount"),
                        balance = json.getInt("balance"),
                        success = json.getBoolean("success"),
                        message = json.getString("message")
                    )
                )
            }

            return history

        } finally {
            connection.disconnect()
        }
    }
}