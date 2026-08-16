package com.ticketcheck.offline.ui.navigation

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val SCANNER = "scanner"
    const val MANAGE = "manage"
    const val TICKET_LIST = "ticket_list"
    const val TICKET_DETAIL = "ticket_detail/{ticketId}"
    const val QR_GENERATE = "qr_generate"
    const val QR_GENERATE_FOR = "qr_generate/{ticketCode}"
    const val QR_GENERATE_BULK = "qr_generate_bulk/{codes}"
    const val SCAN_HISTORY = "scan_history"
    const val BACKUP = "backup"
    const val SETTINGS = "settings"

    fun ticketDetail(id: Long) = "ticket_detail/$id"
    fun qrGenerateFor(code: String) = "qr_generate/$code"
    fun qrGenerateBulk(codes: List<String>) = "qr_generate_bulk/${codes.joinToString(",")}"
}
