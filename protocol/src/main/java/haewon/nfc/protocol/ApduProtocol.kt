package haewon.nfc.protocol

object ApduProtocol {
    val SELECT_AID = byteArrayOf(
        0x00, 0xA4.toByte(), 0x04, 0x00,
        0x07, 0xF0.toByte(), 0x54, 0x41,
        0x50, 0x47, 0x4F, 0x01
    )

    // 카드 정보 요청 명령
    val GET_CARD = byteArrayOf(
        0x80.toByte(), 0x10, 0x00, 0x00, 0x00
    )

    // 정상 응답
    val SUCCESS = byteArrayOf(
        0x90.toByte(), 0x00
    )

    // 선택되지 않은 카드 또는 대상 없음
    val NOT_FOUND = byteArrayOf(
        0x6A.toByte(), 0x82.toByte()
    )

    // 지원하지 않는 명령
    val NOT_SUPPORTED = byteArrayOf(
        0x6D.toByte(), 0x00
    )

    // 데모용 가상 카드 ID
    const val CARD_ID = "TOUCHON-0001"

    // 데이터 뒤에 정상 응답 코드(9000)를 붙임
    fun response(data: ByteArray): ByteArray {
        return data + SUCCESS
    }
}