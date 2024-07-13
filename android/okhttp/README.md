## Request

* An HTTP request.

  ```kotlin
  class Request internal constructor(
    @get:JvmName("url") val url: HttpUrl,
    @get:JvmName("method") val method: String,
    @get:JvmName("headers") val headers: Headers,
    @get:JvmName("body") val body: RequestBody?,
    internal val tags: Map<Class<*>, Any>
  )
  ```

## OkHttpClient

* what is? -> Factory for calls, which can be used to send HTTP requests and read their responses.

* features

  1. OkHttpClients Should Be Shared. 
  2. each client holds its own connection pool and thread pools. Reusing connections and threads reduces latency and saves memory.

* code

  ```kotlin
  open class OkHttpClient internal constructor(
    builder: Builder
  ) {
    // specifies the thread of execution
    val dispatcher: Dispatcher
    // HTTP requests that share the same Address may share a Connection.
    val connectionPool: ConnectionPool
    // observe the full span of each call
    val interceptors: List<Interceptor>
    // Listener for metrics events. Extend this class to monitor the quantity, size, and duration of your application's HTTP calls.
    val eventListenerFactory: EventListener.Factory
    // Caches HTTP and HTTPS responses to the filesystem so they may be reused, saving time and bandwidth.
    val cache: Cache?
    // This class represents a proxy setting, typically a type (http, socks) and a socket address. 
    val proxy: Proxy?
  }
  ```

## Dispatcher

* comment -> Policy on when async requests are executed. Each dispatcher uses an ExecutorService to run calls internally.

* code

  ```kotlin
  class Dispatcher constructor() {
    val executorService: ExecutorService
      get() {
        if (executorServiceOrNull == null) {
          // 创建线程池
          executorServiceOrNull = ThreadPoolExecutor(0, Int.MAX_VALUE, 60, TimeUnit.SECONDS,
              SynchronousQueue(), threadFactory("$okHttpName Dispatcher", false))
        }
        return executorServiceOrNull!!
      }
  }
  ```

## RealCall

* Call

  ```kotlin
  package okhttp3
  
  interface Call : Cloneable {
    fun request(): Request
    fun execute(): Response
    fun enqueue(responseCallback: Callback)
  }
  ```

* RealCall

  ```kotlin
  class RealCall(
    val client: OkHttpClient,
    val originalRequest: Request,
    val forWebSocket: Boolean
  ) : Call {
    private val executed = AtomicBoolean()
    
    override fun enqueue(responseCallback: Callback) {
      // atomic operation
      check(executed.compareAndSet(false, true))
      // call dispatch.enqueue(AsyncCall)
      client.dispatcher.enqueue(AsyncCall(responseCallback))
    }
    
    internal inner class AsyncCall(
      private val responseCallback: Callback
    ) : Runnable {
      // Dispather.enqueue() will call this
      fun executeOn(executorService: ExecutorService) {
        executorService.execute(this)
      }
      // implements Runnable
      override fun run() {
        // try get response
        val response = getResponseWithInterceptorChain()
        // call callback
        responseCallback.onResponse(this@RealCall, response)
      }
    }
    
    internal fun getResponseWithInterceptorChain(): Response {
      // add all kinds of Interceptors
      // use chain of responsibility pattern to get response
      val interceptors = mutableListOf<Interceptor>()
      interceptors += client.interceptors
      interceptors += RetryAndFollowUpInterceptor(client)
      interceptors += BridgeInterceptor(client.cookieJar)
      interceptors += CacheInterceptor(client.cache)
      interceptors += ConnectInterceptor
      if (!forWebSocket) {
        interceptors += client.networkInterceptors
      }
      interceptors += CallServerInterceptor(forWebSocket)
      
      val chain = RealInterceptorChain(interceptors = interceptors)
      return chain.proceed(originalRequest)
    }
  }
  ```

## Intercepter

* RetryAndFollowUpInterceptor

  ```kotlin
  class RetryAndFollowUpInterceptor(private val client: OkHttpClient) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
      while (true) {
        try {
          response = realChain.proceed(request) // try get response
          newExchangeFinder = true
        } catch (e: RouteException) {
          // try connect via a route failed, want to retry
          if (!recover(e.lastConnectException, call, request, requestSendStarted = false)) {
            // not meet retry condition, fail
            throw e.firstConnectException.withSuppressed(recoveredFailures)
          } else {
            continue // start to retry
          }
        } catch (e: IOException) {
          // An attempt to communicate with a server failed. want to retry
          if (!recover(e, call, request, requestSendStarted = e !is ConnectionShutdownException)) {
            // not meet retry condition, fail
            throw e.withSuppressed(recoveredFailures)
          } else {
            continue // start to retry
          }
        }
        // this method will check if status_code indicates you need to redirect
        val followUp = followUpRequest(response, exchange)
        if (++followUpCount > MAX_FOLLOW_UPS) {
          // when redirect counts overlimit, throw exception
          throw ProtocolException("Too many follow-up requests: $followUpCount")
        }
        // redirect, rerun while true
        request = followUp
        priorResponse = response
      }
    }
    
    private fun recover(call: RealCall, userRequest: Request, requestSendStarted: Boolean): Boolean {
      // The application layer has forbidden retries.
      if (!client.retryOnConnectionFailure) return false
      // We can't send the request body again.
      if (requestSendStarted && requestIsOneShot(e, userRequest)) return false
      // This exception is fatal.
      if (!isRecoverable(e, requestSendStarted)) return false
      // No more routes to attempt.
      if (!call.retryAfterFailure()) return false
      // For failure recovery, use the same route selector with a new connection.
      return true
    }
    
    // get redirect url
    private fun buildRedirectRequest(userResponse: Response, method: String): Request? {
      val location = userResponse.header("Location") ?: return null
      val url = userResponse.request.url.resolve(location) ?: return null
      return requestBuilder.url(url).build()
    }
  }
  ```

* BridgeInterceptor

  ```kotlin
  class BridgeInterceptor(private val cookieJar: CookieJar) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
      val userRequest = chain.request()
      val requestBuilder = userRequest.newBuilder()
      requestBuilder.header("Content-Type", contentType.toString())
      requestBuilder.header("Content-Length", contentLength.toString())
      requestBuilder.header("Connection", "Keep-Alive")
      requestBuilder.header("Accept-Encoding", "gzip") // use gzip algorithm to reduce response size
      
      val responseBuilder = networkResponse.newBuilder()
          .request(userRequest)
      // when you use gzip to compress response, it's your duty to decompress it
      if ("gzip".equals(networkResponse.header("Content-Encoding")) {
        val gzipSource = GzipSource(responseBody.source())
        responseBuilder.body(RealResponseBody(contentType, -1L, gzipSource.buffer()))
      }
      return responseBuilder.build()
    }
  }
  ```

* CacheInterceptor

  ```kotlin
  class CacheInterceptor(internal val cache: Cache?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
      // If we don't need the network, we're done.
      if (networkRequest == null) {
        return cacheResponse
      }
      // do real request
      networkResponse = chain.proceed(networkRequest)
      // when not modified, use cache
      if (networkResponse?.code == HTTP_NOT_MODIFIED) {
        val response = cacheResponse
        cache.update(cacheResponse, response)
        return response
      }
      // not hit cache
      val response = networkResponse
      cache.put(response) // put into cache
      return response
    }
  }
  ```

* ConnectionInterceptor

  ```kotlin
  object ConnectInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
      val realChain = chain as RealInterceptorChain
      // code are very little, but inner it is complicated
      val exchange = realChain.call.initExchange(chain)
      val connectedChain = realChain.copy(exchange = exchange)
      return connectedChain.proceed(realChain.request)
    }
  }
  ```

  ```kotlin
  class ExchangeFinder(
    private val connectionPool: RealConnectionPool, // pool
    internal val address: Address, // address
    private val call: RealCall) {
    
    private fun findConnection(connectTimeout: Int, readTimeout: Int, writeTimeout: Int, pingIntervalMillis: Int, onnectionRetryEnabled: Boolean ): RealConnection {
      // Attempt to reuse the connection from the call.
      // If the call's connection wasn't released, reuse it. We don't call connectionAcquired() here
      // because we already acquired it.
      if (call.connection != null) { return callConnection }
      
      // Attempt to get a connection from the pool.
      if (connectionPool.callAcquirePooledConnection(address, call)) {
        return call.connection!!
      }
      
      // Nothing in the pool. Figure out what route we'll try next.
      val routes: List<Route>?
      val route: Route
      
      // Connect. Tell the call about the connecting call so async cancels work.
      newConnection.connect()
      return newConnection
    }
  }
  ```

  ```kotlin
  class RealConnection(
    val connectionPool: RealConnectionPool,
    private val route: Route
  ) : Http2Connection.Listener(), Connection {
    fun connect() {
      if (route.requiresTunnel()) {
        connectTunnel(connectTimeout)
      } else {
        connectSocket(connectTimeout)
      }
      establishProtocol()
    }
    
    private fun establishProtocol() {
      if (route.address.sslSocketFactory == null) { //https
        if (Protocol.H2_PRIOR_KNOWLEDGE in route.address.protocols) {
          protocol = Protocol.H2_PRIOR_KNOWLEDGE
          startHttp2(pingIntervalMillis) //http2
          return
        }
        socket = rawSocket
        protocol = Protocol.HTTP_1_1
        return
      }
      if (protocol === Protocol.HTTP_2) {
        startHttp2(pingIntervalMillis) // http2
      }
    }
  }
  ```

* CallServerInterceptor

  ```kotlin
  class CallServerInterceptor(private val forWebSocket: Boolean) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
      // write request header to server 
      exchange.writeRequestHeaders(request)
      // write request body to server
      requestBody.writeTo(bufferedRequestBody)
      // read response header from server
      responseBuilder = exchange.readResponseHeaders(expectContinue = true)
      // read response body from server
      exchange.openResponseBody(response)
      return response
    }
  }
  ```

***

## 