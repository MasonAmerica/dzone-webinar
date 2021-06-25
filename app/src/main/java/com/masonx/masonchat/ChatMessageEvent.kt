package com.masonx.masonchat

import java.lang.IllegalArgumentException

abstract class ChatMessageEvent(val sender: String, val message: String) {
    private val createdAt: Long = System.currentTimeMillis()

    override fun toString(): String {
        return "${sender}:${message}"
    }
}

class SendMessageEvent(sender: String, message: String) : ChatMessageEvent(sender, message) {}
class IncomingMessageEvent(sender: String, message: String) : ChatMessageEvent(sender, message) {
    companion object {
        fun from(from: String): IncomingMessageEvent {
            val arr = from.split(":", limit=2)
            if (arr.size != 2) throw IllegalArgumentException("Need a colon delimited string")
            return IncomingMessageEvent(arr[0], arr[1])
        }
    }
}