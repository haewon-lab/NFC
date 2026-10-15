package haewon.nfc.terminal

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var reader: NfcReaderManager

    private lateinit var statusText: TextView
    private lateinit var descriptionText: TextView
    private lateinit var cardIdText: TextView
    private lateinit var nfcIcon: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.tvStatus)
        descriptionText = findViewById(R.id.tvDescription)
        cardIdText = findViewById(R.id.tvCardId)
        nfcIcon = findViewById(R.id.tvNfcIcon)

        showWaiting()

        reader = NfcReaderManager(
            activity = this,

            onCardRead = { cardId ->
                runOnUiThread {
                    showCardRead(cardId)
                }
            },

            onError = { message ->
                runOnUiThread {
                    showError(message)
                }
            }
        )
    }

    private fun showWaiting() {
        statusText.text = "NFC 카드를 태그해주세요"
        descriptionText.text =
            "휴대폰 뒷면에 카드를 가까이 대주세요"

        nfcIcon.text = "◎"
        nfcIcon.setTextColor(0xFF2463D3.toInt())

        cardIdText.visibility = View.GONE
    }

    private fun showCardRead(cardId: String) {
        statusText.text = "카드 인식 완료"
        descriptionText.text =
            "카드 정보를 정상적으로 읽었습니다"

        nfcIcon.text = "✓"
        nfcIcon.setTextColor(0xFF16A36A.toInt())

        cardIdText.text = "카드 ID: $cardId"
        cardIdText.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        statusText.text = "카드 처리 오류"
        descriptionText.text = message

        nfcIcon.text = "!"
        nfcIcon.setTextColor(0xFFE05252.toInt())

        cardIdText.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        showWaiting()
        reader.start()
    }

    override fun onPause() {
        reader.stop()
        super.onPause()
    }
}