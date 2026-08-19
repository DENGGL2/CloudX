package com.denggl2.masonremote.transport

import android.content.Context
import java.nio.ByteBuffer
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import kotlin.coroutines.resume

/**
 * Carries the existing Connector HTTP API over one reliable, ordered DataChannel.
 * The signaling endpoint is only used for SDP setup; business requests stay on the
 * encrypted WebRTC channel and retain their original method/path/body semantics.
 */
internal class WebRtcDataChannelTransport(
    context: Context,
    private val route: PairingRoutePayload,
    private val offerId: String,
) : AutoCloseable {
    private val appContext = context.applicationContext
    private val signalingEndpoint = requireNotNull(route.signalingEndpoint?.trim()?.toHttpUrl()) {
        "WebRTC 配对缺少 signaling endpoint"
    }.also(::requireSecureSignalingEndpoint)
    private val signalingClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .build()
    private val connectionMutex = Mutex()
    private var connection: Connection? = null

    suspend fun request(
        method: String,
        path: String,
        headers: Map<String, String> = emptyMap(),
        body: ByteArray? = null,
    ): WebRtcHttpResponse {
        val retryable = method.equals("GET", ignoreCase = true) ||
            method.equals("HEAD", ignoreCase = true) ||
            method.equals("OPTIONS", ignoreCase = true)
        var attempt = 0
        while (true) {
            var active: Connection? = null
            try {
                active = connectionMutex.withLock {
                    connection?.takeIf(Connection::isUsable)
                        ?: connect().also { replacement ->
                            connection?.close()
                            connection = replacement
                        }
                }
                return active.request(method, path, headers, body)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                active?.let { invalidate(it) }
                if (!retryable || attempt++ > 0) throw error
            }
        }
    }

    private suspend fun invalidate(stale: Connection) {
        connectionMutex.withLock {
            if (connection === stale) {
                connection = null
                stale.close()
            }
        }
    }

    /**
     * The transport is process-scoped so pairing, list, and detail clients use
     * the same authenticated DataChannel. The process owns its native WebRTC
     * resources and releases them when the Android process exits.
     */
    override fun close() = Unit

    private suspend fun connect(): Connection = withContext(Dispatchers.IO) {
        ensurePeerConnectionFactory(appContext)
        val factory = PeerConnectionFactory.builder().createPeerConnectionFactory()
        val iceServers = buildList {
            if (route.stunServers.isNotEmpty()) {
                add(PeerConnection.IceServer.builder(route.stunServers).createIceServer())
            }
            route.turnServers.forEach { server ->
                if (server.urls.isNotEmpty()) {
                    add(
                        PeerConnection.IceServer.builder(server.urls)
                            .setUsername(server.username.orEmpty())
                            .setPassword(server.credential.orEmpty())
                            .createIceServer(),
                    )
                }
            }
        }
        val gatheringComplete = CompletableDeferred<Unit>()
        val incomingChannel = CompletableDeferred<DataChannel>()
        val localCandidates = CopyOnWriteArrayList<IceCandidate>()
        val peer = factory.createPeerConnection(
            PeerConnection.RTCConfiguration(iceServers),
            object : PeerConnection.Observer {
                override fun onSignalingChange(newState: PeerConnection.SignalingState?) = Unit
                override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) = Unit
                override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
                override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {
                    if (newState == PeerConnection.IceGatheringState.COMPLETE) {
                        gatheringComplete.complete(Unit)
                    }
                }
                override fun onIceCandidate(candidate: IceCandidate?) {
                    candidate?.let(localCandidates::add)
                }
                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) = Unit
                override fun onAddStream(stream: MediaStream?) = Unit
                override fun onRemoveStream(stream: MediaStream?) = Unit
                override fun onDataChannel(dataChannel: DataChannel?) {
                    dataChannel?.let { incomingChannel.complete(it) }
                }
                override fun onRenegotiationNeeded() = Unit
                override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) = Unit
            },
        ) ?: error("无法创建 WebRTC PeerConnection")
        val pollScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val channel = if (route.signalingMode == SignalingMode.DESKTOP_OFFER) {
                val remoteOffer = requestDesktopOffer()
                setRemoteDescription(
                    peer,
                    SessionDescription(
                        SessionDescription.Type.fromCanonicalForm(remoteOffer.type),
                        remoteOffer.sdp,
                    ),
                )
                remoteOffer.candidates.forEach { candidate ->
                    peer.addIceCandidate(
                        IceCandidate(candidate.sdpMid, candidate.sdpMLineIndex, candidate.candidate),
                    )
                }
                val answer = createAnswer(peer)
                setLocalDescription(peer, answer)
                if (peer.iceGatheringState() == PeerConnection.IceGatheringState.COMPLETE) {
                    gatheringComplete.complete(Unit)
                }
                withTimeout(15_000) { gatheringComplete.await() }
                postDesktopOfferAnswer(peer.localDescription ?: answer)
                localCandidates.forEach { candidate -> postCandidate("mobile", candidate) }
                pollScope.launch { pollDesktopCandidates(peer) }
                withTimeout(20_000) { incomingChannel.await() }
            } else {
                val outgoing = peer.createDataChannel("mason-rpc", DataChannel.Init().apply {
                    ordered = true
                }) ?: error("无法创建 WebRTC DataChannel")
                val offer = createOffer(peer)
                setLocalDescription(peer, offer)
                if (peer.iceGatheringState() == PeerConnection.IceGatheringState.COMPLETE) {
                    gatheringComplete.complete(Unit)
                }
                withTimeout(15_000) { gatheringComplete.await() }
                val answer = requestAnswer(offer = peer.localDescription ?: offer)
                setRemoteDescription(peer, answer)
                outgoing
            }
            Connection(peer, factory, channel, offerId).also { it.awaitOpen() }
        } catch (error: Throwable) {
            peer.close()
            peer.dispose()
            factory.dispose()
            throw error
        } finally {
            pollScope.cancel()
        }
    }

    private suspend fun requestDesktopOffer(): SignalingOfferResponse = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(signalingEndpoint.resolve("v1/offers/$offerId") ?: error("WebRTC signaling endpoint 无效"))
            .get()
            .build()
        signalingClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("WebRTC offer 获取失败（${response.code}）: $body")
            RemoteProtocolJson.decode(body)
        }
    }

    private suspend fun postDesktopOfferAnswer(answer: SessionDescription) = withContext(Dispatchers.IO) {
        val payload = SignalingDescriptionUpload(
            offerId = offerId,
            type = answer.type.canonicalForm(),
            sdp = answer.description,
            expiresAt = System.currentTimeMillis() + 5 * 60_000,
        )
        val request = Request.Builder()
            .url(signalingEndpoint.resolve("v1/offers/$offerId") ?: error("WebRTC signaling endpoint 无效"))
            .header("Content-Type", "application/json")
            .post(RemoteProtocolJson.encode(payload).toRequestBody(JSON_MEDIA_TYPE))
            .build()
        signalingClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("WebRTC answer 登记失败（${response.code}）: $body")
        }
    }

    private suspend fun postCandidate(side: String, candidate: IceCandidate) = withContext(Dispatchers.IO) {
        val payload = SignalingCandidateUpload(
            side = side,
            candidate = candidate.sdp,
            sdpMid = candidate.sdpMid,
            sdpMLineIndex = candidate.sdpMLineIndex,
        )
        val request = Request.Builder()
            .url(signalingEndpoint.resolve("v1/offers/$offerId/candidates") ?: error("WebRTC signaling endpoint 无效"))
            .header("Content-Type", "application/json")
            .post(RemoteProtocolJson.encode(payload).toRequestBody(JSON_MEDIA_TYPE))
            .build()
        signalingClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("WebRTC ICE 登记失败（${response.code}）")
        }
    }

    private suspend fun pollDesktopCandidates(peer: PeerConnection) {
        val seen = HashSet<String>()
        withTimeout(20_000) {
            while (isActive && peer.iceConnectionState() !in setOf(
                    PeerConnection.IceConnectionState.CONNECTED,
                    PeerConnection.IceConnectionState.COMPLETED,
                )) {
                val request = Request.Builder()
                    .url(
                        signalingEndpoint.resolve("v1/offers/$offerId/candidates?side=mobile")
                            ?: error("WebRTC signaling endpoint 无效"),
                    )
                    .get()
                    .build()
                val candidates = withContext(Dispatchers.IO) {
                    signalingClient.newCall(request).execute().use { response ->
                        if (response.code == 404) return@use emptyList<SignalingCandidate>()
                        val body = response.body?.string().orEmpty()
                        if (!response.isSuccessful) error("WebRTC ICE 获取失败（${response.code}）: $body")
                        RemoteProtocolJson.decode<SignalingCandidatesResponse>(body).candidates
                    }
                }
                candidates.forEach { candidate ->
                    val key = "${candidate.sdpMid}:${candidate.sdpMLineIndex}:${candidate.candidate}"
                    if (seen.add(key)) {
                        peer.addIceCandidate(
                            IceCandidate(candidate.sdpMid, candidate.sdpMLineIndex, candidate.candidate),
                        )
                    }
                }
                delay(250)
            }
        }
    }

    private suspend fun requestAnswer(offer: SessionDescription): SessionDescription = withContext(Dispatchers.IO) {
        val payload = SignalingOfferPayload(
            type = offer.type.canonicalForm(),
            sdp = offer.description,
            offerId = offerId,
        )
        val request = Request.Builder()
            .url(signalingEndpoint.resolve("offer") ?: error("WebRTC signaling endpoint 无效"))
            .header("Content-Type", "application/json")
            .post(RemoteProtocolJson.encode(payload).toRequestBody(JSON_MEDIA_TYPE))
            .build()
        signalingClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("WebRTC 信令失败（${response.code}）: $body")
            val answer = RemoteProtocolJson.decode<SignalingAnswerPayload>(body).answer
            SessionDescription(SessionDescription.Type.fromCanonicalForm(answer.type), answer.sdp)
        }
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val factoryLock = Any()
        private var factoryInitialized = false

        fun ensurePeerConnectionFactory(context: Context) {
            synchronized(factoryLock) {
                if (factoryInitialized) return
                PeerConnectionFactory.initialize(
                    PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions(),
                )
                factoryInitialized = true
            }
        }
    }

    private class Connection(
        private val peer: PeerConnection,
        private val factory: PeerConnectionFactory,
        val channel: DataChannel,
        private val offerId: String,
    ) : AutoCloseable {
        private val open = CompletableDeferred<Unit>()
        private val pending = ConcurrentHashMap<String, CompletableDeferred<WebRtcHttpResponse>>()
        private val closed = AtomicBoolean(false)
        private val disposed = AtomicBoolean(false)

        fun isUsable(): Boolean = !closed.get() && channel.state() == DataChannel.State.OPEN

        init {
            channel.registerObserver(object : DataChannel.Observer {
                override fun onBufferedAmountChange(previousAmount: Long) = Unit

                override fun onStateChange() {
                    when (channel.state()) {
                        DataChannel.State.OPEN -> open.complete(Unit)
                        DataChannel.State.CLOSING,
                        DataChannel.State.CLOSED,
                        -> markClosed(IllegalStateException("WebRTC DataChannel 已关闭"))
                        else -> Unit
                    }
                }

                override fun onMessage(buffer: DataChannel.Buffer) {
                    val bytes = ByteArray(buffer.data.remaining()).also(buffer.data::get)
                    val raw = runCatching {
                        RemoteProtocolJson.decode<SignalingRpcResponse>(String(bytes, Charsets.UTF_8))
                    }.getOrNull() ?: return
                    pending.remove(raw.requestId)?.complete(
                        WebRtcHttpResponse(
                            statusCode = raw.status,
                            headers = raw.headers,
                            body = raw.bodyBase64?.let(java.util.Base64.getDecoder()::decode) ?: ByteArray(0),
                        ),
                    )
                }
            })
            if (channel.state() == DataChannel.State.OPEN) open.complete(Unit)
        }

        suspend fun awaitOpen() = withTimeout(20_000) { open.await() }

        suspend fun request(
            method: String,
            path: String,
            headers: Map<String, String>,
            body: ByteArray?,
        ): WebRtcHttpResponse {
            awaitOpen()
            val requestId = UUID.randomUUID().toString()
            val response = CompletableDeferred<WebRtcHttpResponse>()
            pending[requestId] = response
            val payload = SignalingRpcRequest(
                requestId = requestId,
                offerId = offerId,
                method = method,
                path = path,
                headers = headers,
                bodyBase64 = body?.let(java.util.Base64.getEncoder()::encodeToString),
            )
            val sent = channel.send(
                DataChannel.Buffer(
                    ByteBuffer.wrap(RemoteProtocolJson.encode(payload).toByteArray(Charsets.UTF_8)),
                    false,
                ),
            )
            if (!sent) {
                pending.remove(requestId)
                val failure = IllegalStateException("WebRTC DataChannel 发送失败")
                markClosed(failure)
                throw failure
            }
            return try {
                withTimeout(60_000) { response.await() }
            } finally {
                pending.remove(requestId)
            }
        }

        override fun close() {
            if (!disposed.compareAndSet(false, true)) return
            closed.set(true)
            val failure = IllegalStateException("WebRTC DataChannel 已关闭")
            open.completeExceptionally(failure)
            failAll(failure)
            channel.close()
            channel.dispose()
            peer.close()
            peer.dispose()
            factory.dispose()
        }

        private fun markClosed(error: Throwable) {
            if (!closed.compareAndSet(false, true)) return
            open.completeExceptionally(error)
            failAll(error)
        }

        private fun failAll(error: Throwable) {
            pending.values.forEach { it.completeExceptionally(error) }
            pending.clear()
        }
    }
}

internal data class WebRtcHttpResponse(
    val statusCode: Int,
    val headers: Map<String, String>,
    val body: ByteArray,
)

@Serializable
private data class SignalingOfferPayload(
    val type: String,
    val sdp: String,
    val offerId: String,
)

@Serializable
private data class SignalingAnswerPayload(
    val answer: SignalingSessionDescription,
)

@Serializable
private data class SignalingOfferResponse(
    val offerId: String = "",
    val type: String,
    val sdp: String,
    val expiresAt: Long = 0,
    val revision: Long = 0,
    val candidates: List<SignalingCandidate> = emptyList(),
)

@Serializable
private data class SignalingDescriptionUpload(
    val offerId: String,
    val type: String,
    val sdp: String,
    val expiresAt: Long,
)

@Serializable
private data class SignalingCandidateUpload(
    val side: String,
    val candidate: String,
    val sdpMid: String? = null,
    val sdpMLineIndex: Int,
)

@Serializable
private data class SignalingCandidate(
    val candidate: String,
    val sdpMid: String? = null,
    val sdpMLineIndex: Int,
)

@Serializable
private data class SignalingCandidatesResponse(
    val offerId: String = "",
    val revision: Long = 0,
    val candidates: List<SignalingCandidate> = emptyList(),
)

@Serializable
private data class SignalingSessionDescription(
    val type: String,
    val sdp: String,
    val revision: Long = 0,
)

@Serializable
private data class SignalingRpcRequest(
    val type: String = "http.request",
    val requestId: String,
    val offerId: String,
    val method: String,
    val path: String,
    val headers: Map<String, String> = emptyMap(),
    val bodyBase64: String? = null,
)

@Serializable
private data class SignalingRpcResponse(
    val type: String = "http.response",
    val requestId: String,
    val status: Int,
    val headers: Map<String, String> = emptyMap(),
    val bodyBase64: String? = null,
)

private suspend fun createOffer(peer: PeerConnection): SessionDescription =
    suspendCreateSdp { observer -> peer.createOffer(observer, MediaConstraints()) }

private suspend fun createAnswer(peer: PeerConnection): SessionDescription =
    suspendCreateSdp { observer -> peer.createAnswer(observer, MediaConstraints()) }

private suspend fun setLocalDescription(peer: PeerConnection, description: SessionDescription) =
    suspendSetDescription { observer -> peer.setLocalDescription(observer, description) }

private suspend fun setRemoteDescription(peer: PeerConnection, description: SessionDescription) =
    suspendSetDescription { observer -> peer.setRemoteDescription(observer, description) }

private suspend fun suspendCreateSdp(action: (SdpObserver) -> Unit): SessionDescription =
    kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        action(object : SdpObserver {
            override fun onCreateSuccess(description: SessionDescription?) {
                if (description != null) continuation.resume(description) {}
                else continuation.resumeWith(Result.failure(IllegalStateException("WebRTC SDP 为空")))
            }
            override fun onSetSuccess() = Unit
            override fun onCreateFailure(error: String?) {
                continuation.resumeWith(Result.failure(IllegalStateException(error ?: "WebRTC SDP 创建失败")))
            }
            override fun onSetFailure(error: String?) {
                continuation.resumeWith(Result.failure(IllegalStateException(error ?: "WebRTC SDP 设置失败")))
            }
        })
    }

private suspend fun suspendSetDescription(action: (SdpObserver) -> Unit) =
    kotlinx.coroutines.suspendCancellableCoroutine<Unit> { continuation ->
        action(object : SdpObserver {
            override fun onCreateSuccess(description: SessionDescription?) = Unit
            override fun onSetSuccess() {
                continuation.resume(Unit) {}
            }
            override fun onCreateFailure(error: String?) = Unit
            override fun onSetFailure(error: String?) {
                continuation.resumeWith(Result.failure(IllegalStateException(error ?: "WebRTC SDP 设置失败")))
            }
        })
    }

internal object WebRtcTransportRegistry {
    private val transports = ConcurrentHashMap<String, WebRtcDataChannelTransport>()

    fun obtain(
        context: Context,
        route: PairingRoutePayload,
        offerId: String,
    ): WebRtcDataChannelTransport {
        val key = "${route.signalingEndpoint}|$offerId"
        return transports.computeIfAbsent(key) {
            WebRtcDataChannelTransport(
                context = context,
                route = route,
                offerId = offerId,
            )
        }
    }
}
