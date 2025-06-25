package org.nattapon.websocket.controller

import org.nattapon.websocket.model.PokerMessage
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.web.bind.annotation.RestController

@RestController
class PokerController(private val messagingTemplate: SimpMessagingTemplate) {

    data class Room(val owner: String, val votes: MutableMap<String, String?> = mutableMapOf(), var revealed: Boolean = false)

    private val rooms = mutableMapOf<String, Room>()

    @MessageMapping("/poker")
    fun handle(msg: PokerMessage) {
        val room = rooms.computeIfAbsent(msg.roomId) { Room(owner = msg.username) }

        when (msg.type) {
            "create" -> rooms[msg.roomId] = Room(owner = msg.username)
            "join" -> room.votes[msg.username] = null
            "vote" -> room.votes[msg.username] = msg.vote
            "reveal" -> if (msg.username == room.owner) room.revealed = true
            "reset" -> if (msg.username == room.owner) {
                room.revealed = false
                room.votes.keys.forEach { room.votes[it] = null }
            }
            "leave" -> room.votes.remove(msg.username)
        }

        sendUpdate(msg.roomId)
    }

    private fun sendUpdate(roomId: String) {
        val room = rooms[roomId] ?: return
        val payload = mapOf(
            "participants" to room.votes.map { (user, vote) ->
                mapOf("name" to user, "vote" to if (room.revealed) vote else if (vote != null) "🟡" else "")
            },
            "owner" to room.owner,
            "revealed" to room.revealed,
            "count" to room.votes.size
        )
        messagingTemplate.convertAndSend("/topic/$roomId", payload)
    }
}

