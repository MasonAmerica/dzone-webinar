package com.masonx.masonchat

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.masonx.masonchat.ChatMessageEvent
import com.masonx.masonchat.ws.ChatService

class MessageListAdapter(private val messageList: List<ChatMessageEvent>) :
    RecyclerView.Adapter<MessageListAdapter.MessageHolder>() {

    companion object {
        private const val VIEW_TYPE_MESSAGE_SENT = 1
        private const val VIEW_TYPE_MESSAGE_RECEIVED = 2

        private const val TAG = "MessageListAdapter"
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    // Determines the appropriate ViewType according to the sender of the message.
    override fun getItemViewType(position: Int): Int {
        val chat = messageList[position]
        return if (chat.sender == ChatService.nickname) {
            // If the current user is the sender of the message
            VIEW_TYPE_MESSAGE_SENT
        } else {
            // If some other user sent the message
            VIEW_TYPE_MESSAGE_RECEIVED
        }
    }

    // Inflates the appropriate layout according to the ViewType.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageHolder {
        val view: View
        Log.d(TAG, "onCreateViewHolder: $viewType")
        return when (viewType) {
            VIEW_TYPE_MESSAGE_SENT -> {
                view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_sent, parent, false)
                SentMessageHolder(view)
            }
            VIEW_TYPE_MESSAGE_RECEIVED -> {
                view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_received, parent, false)
                ReceivedMessageHolder(view)
            }
            else -> {
                throw IllegalArgumentException("Invalid view type")
            }
        }
    }

    // Passes the message object to a ViewHolder so that the contents can be bound to UI.
    override fun onBindViewHolder(holder: MessageHolder, position: Int) {
        val message = messageList[position]
        when (holder.itemViewType) {
            VIEW_TYPE_MESSAGE_SENT -> (holder as SentMessageHolder?)!!.bind(message)
            VIEW_TYPE_MESSAGE_RECEIVED -> (holder as ReceivedMessageHolder?)!!.bind(message)
        }
    }

    open inner class MessageHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private inner class SentMessageHolder constructor(itemView: View) : MessageHolder(itemView) {
        var messageText: TextView = itemView.findViewById<View>(R.id.text_message_body) as TextView
        var timeText: TextView = itemView.findViewById<View>(R.id.text_message_time) as TextView

        fun bind(message: ChatMessageEvent) {
            messageText.text = message.message

            // Format the stored timestamp into a readable String using method.
            //timeText.setText(Utils.formatDateTime(message.getCreatedAt()));
        }

    }

    private inner class ReceivedMessageHolder constructor(itemView: View) : MessageHolder(itemView) {
        var messageText: TextView = itemView.findViewById<View>(R.id.text_message_body) as TextView
        var timeText: TextView = itemView.findViewById<View>(R.id.text_message_time) as TextView
        var nameText: TextView = itemView.findViewById<View>(R.id.text_message_name) as TextView

        fun bind(message: ChatMessageEvent) {
            messageText.text = message.message

            // Format the stored timestamp into a readable String using method.
            //timeText.setText(Utils.formatDateTime(message.getCreatedAt()));
            nameText.text = message.sender
        }

    }
}