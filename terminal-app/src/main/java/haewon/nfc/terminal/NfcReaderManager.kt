package haewon.nfc.terminal

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import java.io.IOException
import haewon.nfc.protocol.ApduProtocol as P

class NfcReaderManager(
    private val activity: Activity,
    private val onCardRead: (String) -> Unit,
    private val onError: (String) -> Unit
): NfcAdapter.ReaderCallback {
    private val adapter =
        NfcAdapter.getDefaultAdapter(activity)

    fun start() {
        if (adapter == null || !adapter.isEnabled) {
            onError("NFC를 사용할 수 없습니다.")
            return
        }

        val flags =
            NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK

        adapter.enableReaderMode(
            activity,
            this,
            flags,
            Bundle()
        )
    }

    fun stop() {
        adapter?.disableReaderMode(activity)
    }

    override fun onTagDiscovered(tag: Tag) {
        val isoDep = IsoDep.get(tag)

        if (isoDep == null) {
            onError("ISO-DEP 카드가 아닙니다.")
            return
        }

        try {
            isoDep.use { connection ->
                connection.connect()
                connection.timeout = 3000

                val selectResponse =
                    connection.transceive(P.SELECT_AID)

                if (!selectResponse.contentEquals(P.SUCCESS)) {
                    onError("TapGo 카드 선택 실패")
                    return
                }

                val response =
                    connection.transceive(P.GET_CARD)

                if (response.size < 2 ||
                    !response.takeLast(2)
                        .toByteArray()
                        .contentEquals(P.SUCCESS)
                ) {
                    onError("카드 조회 실패")
                    return
                }

                val cardId = String(
                    response.copyOfRange(
                        0,
                        response.size - 2
                    ),
                    Charsets.UTF_8
                )

                onCardRead(cardId)
            }
        } catch (e: IOException) {
            onError("NFC 통신 오류: ${e.message}")
        } catch (e: Exception) {
            onError("카드 처리 오류: ${e.message}")
        }
    }
}