package com.debduttapanda.grpctutorial

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class MainViewModel: ViewModel() {
    private var channel: ManagedChannel? = null
    fun onIpChange(value: String) {
        _ip.value = value
    }

    fun onPortChange(value: String) {
        _port.value = value
    }

    private val _ip = mutableStateOf("")
    val ip: State<String> = _ip

    private val _port = mutableStateOf("")
    val port:State<String> = _port

    private val _result = mutableStateOf("")
    val result:State<String> = _result

    private val _hostEnabled = mutableStateOf(true)
    val hostEnabled:State<Boolean> = _hostEnabled

    private val _portEnabled = mutableStateOf(true)
    val portEnabled:State<Boolean> = _portEnabled

    private val _startRouteEnabled = mutableStateOf(true)
    val startRouteEnabled:State<Boolean> = _startRouteEnabled

    private val _endRouteEnabled = mutableStateOf(false)
    val endRouteEnabled:State<Boolean> = _endRouteEnabled

    private val _buttonsEnabled = mutableStateOf(false)
    val buttonsEnabled:State<Boolean> = _buttonsEnabled



    /////////////////////////
    fun startRouteGuide() {
        try {
            val host: String = _ip.value
            val portStr: String = _port.value
            val port = if (portStr.isEmpty()) 0 else Integer.valueOf(portStr)
            channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
            _endRouteEnabled.value = true
            _hostEnabled.value = false
            _portEnabled.value = false
            _startRouteEnabled.value = false
            _buttonsEnabled.value = true
        } catch (e: Exception) {
            _result.value = (System.currentTimeMillis().toString()  ) + "\n" + (e.message?: "")
        }
    }

    fun exitRouteGuide() {
        channel?.shutdown()
        _endRouteEnabled.value = false
        _hostEnabled.value = true
        _portEnabled.value = true
        _startRouteEnabled.value = true
        _buttonsEnabled.value = false
    }

    fun getFeature() {
        viewModelScope.launch(context = Dispatchers.IO) {
            val request = Point.newBuilder().setLatitude(409146138).setLongitude(-746188906).build()
            val feature: Feature
            val blockingStub = RouteGuideGrpc.newBlockingStub(channel)
            try {
                feature = blockingStub.getFeature(request)
                updateResult(feature.toString())
            } catch (e: Exception) {
                updateResult(e.message?:"")
            }
        }
    }

    fun listFeatures() {
        viewModelScope.launch(Dispatchers.IO) {
            val request = Rectangle.newBuilder()
                .setLo(Point.newBuilder().setLatitude(400000000).setLongitude(-750000000).build())
                .setHi(Point.newBuilder().setLatitude(420000000).setLongitude(-730000000).build())
                .build()
            val blockingStub = RouteGuideGrpc.newBlockingStub(channel)
            try {
                val features = blockingStub.listFeatures(request).asSequence().toList()
                updateResult(features.toString())
            } catch (e: Exception) {
                updateResult(e.message?:"")
            }
        }
    }

    fun recordRoute() {
        viewModelScope.launch(Dispatchers.IO) {
            var failed: Throwable? = null
            val points: MutableList<Point> = ArrayList()
            points.add(Point.newBuilder().setLatitude(407838351).setLongitude(-746143763).build())
            points.add(Point.newBuilder().setLatitude(408122808).setLongitude(-743999179).build())
            points.add(Point.newBuilder().setLatitude(413628156).setLongitude(-749015468).build())
            val numPoints = points.size
            val finishLatch = CountDownLatch(1)

            var routeSummary = mutableListOf<RouteSummary>()

            val responseObserver: StreamObserver<RouteSummary> = object : StreamObserver<RouteSummary> {
                override fun onNext(summary: RouteSummary) {
                    routeSummary.add(summary)
                }

                override fun onError(t: Throwable) {
                    failed = t
                    finishLatch.countDown()
                }

                override fun onCompleted() {
                    finishLatch.countDown()
                }
            }
            val asyncStub = RouteGuideGrpc.newStub(channel)
            val requestObserver: StreamObserver<Point> = asyncStub.recordRoute(responseObserver)
            try {
                // Send numPoints points randomly selected from the points list.
                val rand = Random()
                for (i in 0 until numPoints) {
                    val index = rand.nextInt(points.size)
                    val point = points[index]
                    requestObserver.onNext(point)
                    // Sleep for a bit before sending the next one.
                    delay((rand.nextInt(1000) + 500).toLong())
                    if (finishLatch.count == 0L) {
                        // RPC completed or errored before we finished sending.
                        // Sending further requests won't error, but they will just be thrown away.
                        break
                    }
                }
            } catch (e: RuntimeException) {
                // Cancel RPC
                requestObserver.onError(e)
                throw e
            }
            // Mark the end of requests
            requestObserver.onCompleted()


            // Receiving happens asynchronously
            if (!finishLatch.await(1, TimeUnit.MINUTES)) {
                updateResult("Timeout error")
                return@launch
            }

            if (failed != null) {
                updateResult(failed?.message?:"")
                return@launch
            }
            updateResult(routeSummary.toString())
        }
    }

    fun routeChat() {
        viewModelScope.launch(Dispatchers.IO) {
            var failed: Throwable? = null
            val finishLatch = CountDownLatch(1)
            val asyncStub = RouteGuideGrpc.newStub(channel)
            val routeNotes = mutableListOf<RouteNote>()
            val requestObserver: StreamObserver<RouteNote> = asyncStub.routeChat(
                object : StreamObserver<RouteNote> {
                    override fun onNext(note: RouteNote) {
                        routeNotes.add(note)
                    }

                    override fun onError(t: Throwable) {
                        failed = t
                        finishLatch.countDown()
                    }

                    override fun onCompleted() {
                        finishLatch.countDown()
                    }
                })

            try {
                val requests = arrayOf<RouteNote>(
                    newNote("First message", 0, 0),
                    newNote("Second message", 0, 1),
                    newNote("Third message", 1, 0),
                    newNote("Fourth message", 1, 1)
                )
                for (request in requests) {
                    requestObserver.onNext(request)
                }
            } catch (e: java.lang.RuntimeException) {
                // Cancel RPC
                requestObserver.onError(e)
                updateResult(e.message?:"")
                return@launch
            }
            // Mark the end of requests
            // Mark the end of requests
            requestObserver.onCompleted()

            // Receiving happens asynchronously

            // Receiving happens asynchronously
            if (!finishLatch.await(1, TimeUnit.MINUTES)) {
                updateResult("Timeout error")
                return@launch
            }

            if (failed != null) {
                updateResult(failed?.message?:"")
                return@launch
            }
            updateResult(routeNotes.toString())

        }
    }

    private fun newNote(message: String, lat: Int, lon: Int): RouteNote {
        return RouteNote.newBuilder()
            .setMessage(message)
            .setLocation(Point.newBuilder().setLatitude(lat).setLongitude(lon).build())
            .build()
    }

    private suspend fun updateResult(message: String){
        withContext(Dispatchers.Main){
            _result.value = System.currentTimeMillis().toString() +"\n"+ message
        }
    }
}