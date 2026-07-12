package dev.deathlegion.dazki.net

import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Tiny localhost-only TCP server that speaks the dazki RPC wire
 * format. Listens on port 7654 by default. Each accepted connection
 * runs on a worker thread. Connections are expected to come from the
 * adb port forward tunnel, so we bind to 127.0.0.1 only.
 *
 * The server is intentionally minimal. Real production code would
 * use Netty or OkHttp WebSocket. For dazki's small request volume
 * (an AI assistant calling a few dozen RPCs per minute) plain
 * length-prefixed TCP is enough.
 */
class RpcServer(
    private val port: Int = 7654,
    private val dispatcher: RpcDispatcher,
) {
    private val running = AtomicBoolean(false)
    private val executor = Executors.newCachedThreadPool { r ->
        Thread(r, "dazki-rpc-worker").apply { isDaemon = true }
    }
    private var serverSocket: ServerSocket? = null

    fun start() {
        if (!running.compareAndSet(false, true)) return
        serverSocket = ServerSocket(port, 16, InetAddress.getByName("127.0.0.1"))
        Thread({
            while (running.get()) {
                try {
                    val client = serverSocket?.accept() ?: break
                    executor.submit { handle(client) }
                } catch (_: IOException) {
                    if (running.get()) continue else break
                }
            }
        }, "dazki-rpc-accept").start()
    }

    fun stop() {
        if (!running.compareAndSet(true, false)) return
        serverSocket?.close()
        executor.shutdownNow()
    }

    private fun handle(socket: Socket) {
        socket.use { s ->
            val input = s.getInputStream()
            val output = s.getOutputStream()
            while (running.get() && !s.isClosed) {
                val header = ByteArray(5)
                if (!readFully(input, header)) return
                if (header[0] != 0x00.toByte()) return
                val length = ((header[1].toInt() and 0xFF) shl 24) or
                    ((header[2].toInt() and 0xFF) shl 16) or
                    ((header[3].toInt() and 0xFF) shl 8) or
                    (header[4].toInt() and 0xFF)
                if (length <= 0 || length > 1_048_576) return
                val payload = ByteArray(length)
                if (!readFully(input, payload)) return
                val reply = dispatcher.dispatch(payload)
                val replyBytes = reply.encodeToByteArray()
                val frame = ByteArray(5 + replyBytes.size)
                frame[0] = 0x00
                frame[1] = ((replyBytes.size ushr 24) and 0xFF).toByte()
                frame[2] = ((replyBytes.size ushr 16) and 0xFF).toByte()
                frame[3] = ((replyBytes.size ushr 8) and 0xFF).toByte()
                frame[4] = (replyBytes.size and 0xFF).toByte()
                System.arraycopy(replyBytes, 0, frame, 5, replyBytes.size)
                output.write(frame)
                output.flush()
            }
        }
    }

    private fun readFully(input: java.io.InputStream, buf: ByteArray): Boolean {
        var read = 0
        while (read < buf.size) {
            val n = input.read(buf, read, buf.size - read)
            if (n < 0) return false
            read += n
        }
        return true
    }
}
