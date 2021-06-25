package com.masonx.masonchat.ws

import android.util.Log
import com.masonx.masonchat.ChatMessageEvent
import com.masonx.masonchat.IncomingMessageEvent
import com.masonx.masonchat.Utils
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.*
import io.netty.channel.epoll.EpollSocketChannel
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.HttpClientCodec
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.websocketx.*
import io.netty.handler.flow.FlowControlHandler
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.util.CharsetUtil
import org.greenrobot.eventbus.EventBus
import java.net.URI

class ChatHandler(private val eventLoopGroup: EventLoopGroup, private val uri: URI) :
    SimpleChannelInboundHandler<Any>() {
    private var outboundChannel: Channel? = null

    private lateinit var handshakeFuture: ChannelPromise
    private lateinit var handshaker: WebSocketClientHandshaker

    @Throws(Exception::class)
    fun start(): ChannelFuture? {
        val channelClass: Class<out Channel>
        channelClass = EpollSocketChannel::class.java
        val sslCtx: SslContext? = if (uri.scheme == "wss" || uri.scheme == "https") {
            SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE).build()
        } else {
            null
        }

        // the websocket handshaker
        // we can add custom headers here, if necessary
        handshaker = WebSocketClientHandshakerFactory.newHandshaker(
            uri, WebSocketVersion.V13, null, true, null
        )

        // initialize the channel
        val b = Bootstrap()
        b.group(eventLoopGroup)
            .channel(channelClass)
            .handler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    val p = ch.pipeline()
                    if (sslCtx != null) {
                        p.addLast(
                            sslCtx.newHandler(
                                ch.alloc(), uri.host,
                                uri.port
                            )
                        )
                    }
                    p.addLast(
                        HttpClientCodec(),
                        HttpObjectAggregator(65536)
                    )
                    if (Utils.DEBUG) {
                        p.addLast(LoggingHandler(LogLevel.DEBUG))
                    }
                    p.addLast(FlowControlHandler())
                    p.addLast(this@ChatHandler)
                }
            })
        outboundChannel = b.connect(uri.host, uri.port).sync().channel()
        return handshakeFuture
    }

    fun send(msg: String?) {
        Log.d(TAG, "send: $msg")
        outboundChannel?.writeAndFlush(TextWebSocketFrame(msg))
    }

    override fun handlerAdded(ctx: ChannelHandlerContext) {
        if (Utils.DEBUG) {
            Log.d(TAG, "handlerAdded")
        }
        handshakeFuture = ctx.newPromise()
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        if (Utils.DEBUG) {
            Log.d(TAG, "channelActive")
        }
        handshaker.handshake(ctx.channel())
    }

    @Throws(Exception::class)
    override fun channelRead0(ctx: ChannelHandlerContext, msg: Any) {
        if (Utils.DEBUG) {
            Log.d(TAG, "channelRead0: $msg")
        }
        val ch = ctx.channel()
        if (!handshaker.isHandshakeComplete) {
            try {
                handshaker.finishHandshake(ch, msg as FullHttpResponse)
                handshakeFuture.setSuccess()
            } catch (e: WebSocketHandshakeException) {
                Log.e(TAG, e.message, e)
                handshakeFuture.setFailure(e)
            }
            return
        }
        if (msg is FullHttpResponse) {
            // http/ws negotiation
            throw IllegalStateException(
                "Unexpected FullHttpResponse (getStatus=" + msg.status() +
                        ", content=" + msg.content().toString(CharsetUtil.UTF_8) + ')'
            )
        }

        when (val frame = msg as WebSocketFrame) {
            is CloseWebSocketFrame -> {
                if (Utils.DEBUG) {
                    Log.i(TAG, "WebSocket client recieved close frame")
                }
                ch.close()
            }
            is PingWebSocketFrame -> {
                if (Utils.DEBUG) {
                    Log.i(TAG, "WebSocket client recieved ping frame")
                }

                // keepalive
                val pong = PongWebSocketFrame()
                outboundChannel!!.writeAndFlush(pong)
                    .addListener(ChannelFutureListener { future: ChannelFuture ->
                        if (future.isSuccess) {
                            ctx.channel().read()
                        } else {
                            Log.e(TAG, future.cause().message, future.cause())
                            future.channel().close()
                        }
                    })
            }
            is TextWebSocketFrame -> {
                // incoming message
                val chat = IncomingMessageEvent.from(frame.text())
                Log.d(TAG, "TextMessage: $chat" )
                EventBus.getDefault().post(chat)
            }
            else -> {
                Log.e(TAG, "unsupported message: $msg")
            }
        }
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        if (Utils.DEBUG) {
            Log.d(TAG, "channelInactive")
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        Log.e(TAG, cause.message, cause)
        if (!handshakeFuture.isDone) {
            handshakeFuture.setFailure(cause)
        }
        closeOnFlush(ctx.channel())
    }

    companion object {
        private const val TAG = "ChatHandler"

        /**
         * Closes the specified channel after all queued write requests are flushed.
         */
        fun closeOnFlush(ch: Channel?) {
            if (ch != null && ch.isActive) {
                ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
            }
        }
    }
}