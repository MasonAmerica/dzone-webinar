package com.masonx.masonchat

import android.content.Intent
import android.os.*
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.masonx.masonchat.ChatMessageEvent
import com.masonx.masonchat.SendMessageEvent
import com.masonx.masonchat.ws.ChatService
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MainActivity : AppCompatActivity() {

    private val TAG: String = "MainActivity"

    private lateinit var messageRecycler: RecyclerView
    private lateinit var messageAdapter: MessageListAdapter

    private val messageList: ArrayList<ChatMessageEvent> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.message_list_activity)

        messageRecycler = findViewById(R.id.recycler_gchat)
        messageRecycler.layoutManager = LinearLayoutManager(this)

        messageAdapter = MessageListAdapter(messageList)
        messageRecycler.adapter = messageAdapter

        val textEntry = findViewById<EditText>(R.id.edit_gchat_message)
        val sender = findViewById<Button>(R.id.button_gchat_send)

        sender.setOnClickListener {
            if (textEntry.text != null) {
                Log.d(TAG, "clicked: " + textEntry.text)
                sendChat(textEntry.text.toString())
                textEntry.text = null
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val intent = Intent(this, ChatService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStart() {
        EventBus.getDefault().register(this);
        super.onStart()
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun handleEvent(event: IncomingMessageEvent) {
        messageList.add(event)
        messageAdapter.notifyDataSetChanged()
        Log.d(TAG, "handleEvent: $event ${event.javaClass.name}")
    }

    private fun sendChat(chat: String) {
        EventBus.getDefault().postSticky(SendMessageEvent(ChatService.nickname, chat))
    }

}
