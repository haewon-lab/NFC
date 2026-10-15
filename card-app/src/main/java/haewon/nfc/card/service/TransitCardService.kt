package haewon.nfc.card.service

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import haewon.nfc.protocol.ApduProtocol as P
class TransitCardService: HostApduService() {
    private var selected = false

    override fun processCommandApdu(
        commandApdu: ByteArray,
        extras: Bundle?
    ): ByteArray {

        return when {
            commandApdu.contentEquals(P.SELECT_AID) -> {
                selected = true
                P.SUCCESS
            }

            commandApdu.contentEquals(P.GET_CARD) -> {
                if (!selected) {
                    P.NOT_FOUND
                } else {
                    P.response(
                        P.CARD_ID.toByteArray(Charsets.UTF_8)
                    )
                }
            }

            else -> P.NOT_SUPPORTED
        }
    }

    override fun onDeactivated(reason: Int) {
        selected = false
    }
}