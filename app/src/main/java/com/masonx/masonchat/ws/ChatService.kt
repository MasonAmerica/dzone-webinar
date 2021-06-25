package com.masonx.masonchat.ws

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import com.masonx.masonchat.R
import com.masonx.masonchat.SendMessageEvent
import com.masonx.masonchat.Utils
import io.netty.channel.ChannelFuture
import io.netty.channel.epoll.EpollEventLoopGroup
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.net.URI
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ChatService : Service() {

    private val TAG: String = "ChatService"
    private val group = EpollEventLoopGroup()
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    private var chatThread: ChatThread? = null

    companion object {
        val nickname = Build.SERIAL
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        chatThread = ChatThread()
        executor.submit(chatThread)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException("Not implemented")
    }

    inner class ChatThread : HandlerThread("ChatThread") {

        private var future: ChannelFuture? = null
        private var nettyHandler: ChatHandler

        @Subscribe(threadMode = ThreadMode.ASYNC)
        fun handleEvent(event: SendMessageEvent) {
            Log.d(TAG, "handleEvent: $event ${event.javaClass.name}")
            nettyHandler.send(event.toString())
        }

        init {
            val uri = URI.create(resources.getString(R.string.serviceUri) + "/" + nickname)
            nettyHandler = ChatHandler(group, uri)
            EventBus.getDefault().register(this)
        }

        override fun run() {
            try {
                future = nettyHandler.start()
                future!!.sync().channel().closeFuture().sync()
            } catch (e: Exception) {
                Log.e(TAG, e.message, e)
            } finally {
                stopWS()
            }
        }

        private fun stopWS() {
            if (Utils.DEBUG) {
                Log.e(TAG, "shutting down")
            }
            group.shutdownGracefully()
        }
    }
}