package org.nattapon.websocket.model

data class PokerMessage(
    val type: String,
    val roomId: String,
    val username: String,
    val vote: String? = null
)