package haewon.nfc.terminal

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import haewon.nfc.protocol.ApduProtocol
import java.util.concurrent.Executors

class MainActivity : Activity() {

    private lateinit var reader: NfcReaderManager

    private lateinit var statusText: TextView
    private lateinit var descriptionText: TextView
    private lateinit var cardIdText: TextView
    private lateinit var nfcIcon: TextView
    private lateinit var nfcStateText: TextView
    private lateinit var terminalStateText: TextView
    private lateinit var testButton: Button

    // 잔액조회
    private lateinit var balanceButton: Button
    private lateinit var balanceResultText: TextView

    private val executor = Executors.newSingleThreadExecutor()
    private var processing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.tvStatus)
        descriptionText = findViewById(R.id.tvDescription)
        cardIdText = findViewById(R.id.tvCardId)
        nfcIcon = findViewById(R.id.tvNfcIcon)
        nfcStateText = findViewById(R.id.tvNfcState)
        terminalStateText = findViewById(R.id.tvTerminalState)
        testButton = findViewById(R.id.btnTestTap)

        balanceButton = findViewById(R.id.btnCheckBalance)
        balanceResultText = findViewById(R.id.tvBalanceResult)

        showWaiting()

        reader = NfcReaderManager(
            activity = this,
            onCardRead = { cardId ->
                runOnUiThread {
                    requestTap(cardId)
                }
            },
            onError = { message ->
                runOnUiThread {
                    if (!processing) {
                        showError(message)
                    }
                }
            }
        )

        testButton.setOnClickListener {
            requestTap("TOUCHON-0001")
        }

        balanceButton.setOnClickListener {
            requestCardBalance(ApduProtocol.CARD_ID)
        }
    }

    private fun requestTap(cardId: String) {
        if (processing) return

        processing = true
        testButton.isEnabled = false

        statusText.text = "승차 처리 중"
        descriptionText.text = "서버에서 카드 정보를 확인하고 있습니다"
        nfcIcon.text = "…"
        cardIdText.visibility = View.GONE
        nfcStateText.text = "PROCESSING"
        terminalStateText.text = "단말기 상태  ·  승차 처리 중"

        executor.execute {
            try {
                val result = TransitApiClient.tap(cardId)

                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread

                    processing = false
                    testButton.isEnabled = true

                    if (result.success) {
                        showSuccess(result)
                    } else {
                        showTapFailure(result)
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread

                    processing = false
                    testButton.isEnabled = true
                    showError(
                        "서버 연결 실패: ${e.message ?: "알 수 없는 오류"}"
                    )
                }
            }
        }
    }

    private fun showWaiting() {
        statusText.text = "카드를 태그해주세요"
        descriptionText.text =
            "휴대폰 뒷면에 교통카드를 가까이 대주세요"

        nfcIcon.text = "◎"
        nfcIcon.setTextColor(0xFF2463D3.toInt())

        cardIdText.visibility = View.GONE
        nfcStateText.text = "NFC READY"
        terminalStateText.text = "단말기 상태  ·  정상 대기 중"
    }

    private fun showSuccess(result: TapResult) {
        statusText.text = "승차 처리 완료"
        descriptionText.text = result.message

        nfcIcon.text = "✓"
        nfcIcon.setTextColor(0xFF16A36A.toInt())

        cardIdText.text =
            "카드 ID: ${result.cardId}\n" +
                    "승차 요금: ${result.fare}원\n" +
                    "남은 잔액: ${result.balance}원"

        cardIdText.visibility = View.VISIBLE
        nfcStateText.text = "APPROVED"
        terminalStateText.text = "단말기 상태  ·  승차 승인"
    }

    private fun showTapFailure(result: TapResult) {
        statusText.text = "승차 처리 실패"
        descriptionText.text = result.message

        nfcIcon.text = "!"
        nfcIcon.setTextColor(0xFFE05252.toInt())

        cardIdText.text =
            "카드 ID: ${result.cardId}\n" +
                    "현재 잔액: ${result.balance}원"

        cardIdText.visibility = View.VISIBLE
        nfcStateText.text = "DECLINED"
        terminalStateText.text = "단말기 상태  ·  승차 거절"
    }

    private fun showError(message: String) {
        statusText.text = "처리 오류"
        descriptionText.text = message

        nfcIcon.text = "!"
        nfcIcon.setTextColor(0xFFE05252.toInt())

        cardIdText.visibility = View.GONE
        nfcStateText.text = "ERROR"
        terminalStateText.text = "단말기 상태  ·  오류 발생"
    }

    private fun requestCardBalance(cardId: String) {
        balanceButton.isEnabled = false
        balanceResultText.text = "잔액 조회 중..."

        executor.execute {
            try {
                val result = TransitApiClient.getCardBalance(cardId)

                runOnUiThread {
                    balanceResultText.text =
                        "현재 잔액: %,d원".format(result.balance)

                    balanceButton.isEnabled = true
                }
            } catch (e: Exception) {
                runOnUiThread {
                    balanceResultText.text =
                        e.message ?: "잔액 조회에 실패했습니다."

                    balanceButton.isEnabled = true
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (!processing) {
            showWaiting()
        }

        reader.start()
    }

    override fun onPause() {
        reader.stop()
        super.onPause()
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }
}