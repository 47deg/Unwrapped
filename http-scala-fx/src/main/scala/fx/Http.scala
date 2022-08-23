package fx

import java.net.URI
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers
import java.time.Duration
import java.util.Random
import scala.annotation.tailrec
import scala.concurrent.duration.{Duration => ScalaDuration}

/**
 * Models the http scheme
 */
type Http[A] = (
    HttpRetryPolicy[A],
    Control[HttpExecutionException],
    HttpClient
) ?=> A

extension [A](http: Http[A])
  def httpValue: Control[HttpExecutionException] ?=> A = http
  def fmap[B](f: A => B): Http[B] =
    f(http)
  def bindMap[B](f: A => Http[B]): Http[B] =
    f(http)

extension (uri: URI)
  private def addHeadersToRequestBuilder(
      builder: HttpRequest.Builder,
      headers: HttpHeader*): HttpRequest.Builder =
    headers.foldLeft(builder) { (builder: HttpRequest.Builder, header: HttpHeader) =>
      header.values.foldLeft[HttpRequest.Builder](builder) {
        (builder: HttpRequest.Builder, headerValue: String) =>
          builder.header(header.name, headerValue)
      }
    }

  def GET[A](): HttpResponseMapper[A] ?=> Http[HttpResponse[A]] =
    GET(0, None, List.empty: _*)

  def GET[A](headers: HttpHeader*): HttpResponseMapper[A] ?=> Http[HttpResponse[A]] =
    GET(0, None, headers: _*)

  def GET[A](
      timeout: ScalaDuration,
      headers: HttpHeader*): HttpResponseMapper[A] ?=> Http[HttpResponse[A]] =
    GET(0, Option(timeout), headers: _*)

  @tailrec
  private def GET[A](
      retryCount: Int,
      maybeTimeout: Option[ScalaDuration],
      headers: HttpHeader*): HttpResponseMapper[A] ?=> Http[HttpResponse[A]] =
    val mapper = summon[HttpResponseMapper[A]]
    val config = summon[HttpClientConfig]
    val retries = config.maximumRetries.getOrElse(summon[HttpRetries])
    val requestBuilder =
      addHeadersToRequestBuilder(HttpRequest.newBuilder().GET().uri(uri), headers: _*)

    val response: HttpResponse[A] =
      handle(
        summon[HttpClient]
          .client
          .send(
            maybeTimeout
              .fold(requestBuilder) { fd =>
                requestBuilder.timeout(Duration.ofMillis(fd.toMillis))
              }
              .build(),
            mapper.bodyHandler)) { ex => HttpExecutionException(ex).shift }

    if (response.shouldRetry(retryCount)) {
      GET(retryCount + 1, maybeTimeout, headers: _*)
    } else {
      response
    }

  def POST[A, B](body: B, headers: HttpHeader*)
      : (HttpResponseMapper[A], HttpBodyMapper[B]) ?=> Http[HttpResponse[A]] =
    POST(body, None, 0, headers: _*)

  def POST[A, B](body: B, timeout: ScalaDuration, headers: HttpHeader*)
      : (HttpResponseMapper[A], HttpBodyMapper[B]) ?=> Http[HttpResponse[A]] =
    POST(body, Option(timeout), 0, headers: _*)

  @tailrec
  private def POST[A, B](
      body: B,
      maybeTimeout: Option[ScalaDuration],
      retryCount: Int = 0,
      headers: HttpHeader*)
      : (HttpResponseMapper[A], HttpBodyMapper[B]) ?=> Http[HttpResponse[A]] =
    val mapper = summon[HttpResponseMapper[A]]
    val bodyMapper = summon[HttpBodyMapper[B]]
    val config = summon[HttpClientConfig]
    val retries = config.maximumRetries.getOrElse(summon[HttpRetries])
    val requestBuilder = addHeadersToRequestBuilder(
      HttpRequest.newBuilder().POST(bodyMapper.bodyPublisher(body)).uri(uri),
      headers: _*)

    val response =
      handle(
        summon[HttpClient]
          .client
          .send(
            maybeTimeout
              .fold(requestBuilder) { fd =>
                requestBuilder.timeout(Duration.ofMillis(fd.toMillis))
              }
              .build(),
            mapper.bodyHandler)) { ex => HttpExecutionException(ex).shift }
    if (response.shouldRetry(retryCount)) {
      POST(body, maybeTimeout, retryCount + 1, headers: _*)
    } else {
      response
    }

  def HEAD(headers: HttpHeader*): Http[HttpResponse[Void]] = HEAD(None, 0, headers: _*)
  def HEAD(timeout: ScalaDuration, headers: HttpHeader*): Http[HttpResponse[Void]] =
    HEAD(Option(timeout), 0, headers: _*)

  @tailrec
  private def HEAD(
      maybeTimeout: Option[ScalaDuration],
      retryCount: Int = 0,
      headers: HttpHeader*): Http[HttpResponse[Void]] =
    val config = summon[HttpClientConfig]
    val retries = config.maximumRetries.getOrElse(summon[HttpRetries])
    val requestBuilder =
      addHeadersToRequestBuilder(HttpRequest.newBuilder().HEAD().uri(uri), headers: _*)
    val response =
      handle(
        summon[HttpClient]
          .client
          .send(
            maybeTimeout
              .fold(requestBuilder) { fd =>
                requestBuilder.timeout(Duration.ofMillis(fd.toMillis))
              }
              .build(),
            BodyHandlers.discarding)) { ex => HttpExecutionException(ex).shift }
    if (response.shouldRetry(retryCount)) {
      HEAD(maybeTimeout, retryCount + 1, headers: _*)
    } else response

  def PUT[A, B](body: B, headers: HttpHeader*)
      : (HttpResponseMapper[A], HttpBodyMapper[B]) ?=> Http[HttpResponse[A]] =
    PUT[A, B](body, None, 0, headers: _*)

  def PUT[A, B](body: B, timeout: ScalaDuration, headers: HttpHeader*)
      : (HttpResponseMapper[A], HttpBodyMapper[B]) ?=> Http[HttpResponse[A]] =
    PUT[A, B](body, Option(timeout), 0, headers: _*)

  @tailrec
  private def PUT[A, B](
      body: B,
      maybeTimeout: Option[ScalaDuration],
      retryCount: Int = 0,
      headers: HttpHeader*)
      : (HttpResponseMapper[A], HttpBodyMapper[B]) ?=> Http[HttpResponse[A]] =
    val mapper = summon[HttpResponseMapper[A]]
    val bodyMapper = summon[HttpBodyMapper[B]]
    val config = summon[HttpClientConfig]
    val retries = config.maximumRetries.getOrElse(summon[HttpRetries])
    val requestBuilder = addHeadersToRequestBuilder(
      HttpRequest.newBuilder().PUT(bodyMapper.bodyPublisher(body)).uri(uri),
      headers: _*)
    val response =
      handle(
        summon[HttpClient]
          .client
          .send(
            maybeTimeout
              .fold(requestBuilder) { fd =>
                requestBuilder.timeout(Duration.ofMillis(fd.toMillis))
              }
              .build(),
            mapper.bodyHandler)) { ex => HttpExecutionException(ex).shift }
    if (response.shouldRetry(retryCount)) {
      PUT(body, maybeTimeout, retryCount + 1, headers: _*)
    } else response

  def DELETE[A](headers: HttpHeader*): (HttpResponseMapper[A]) ?=> Http[HttpResponse[A]] =
    DELETE[A](None, 0, headers: _*)
  def DELETE[A](
      timeout: ScalaDuration,
      headers: HttpHeader*): (HttpResponseMapper[A]) ?=> Http[HttpResponse[A]] =
    DELETE[A](Option(timeout), 0, headers: _*)

  @tailrec
  private def DELETE[A](
      maybeTimeout: Option[ScalaDuration],
      retryCount: Int = 0,
      headers: HttpHeader*): (HttpResponseMapper[A]) ?=> Http[HttpResponse[A]] =
    val mapper = summon[HttpResponseMapper[A]]
    val config = summon[HttpClientConfig]
    val retries = config.maximumRetries.getOrElse(summon[HttpRetries])
    val requestBuilder =
      addHeadersToRequestBuilder(HttpRequest.newBuilder().DELETE().uri(uri), headers: _*)
    val response =
      handle(
        summon[HttpClient]
          .client
          .send(
            maybeTimeout
              .fold(requestBuilder) { fd =>
                requestBuilder.timeout(Duration.ofMillis(fd.toMillis))
              }
              .build(),
            mapper.bodyHandler)) { ex => HttpExecutionException(ex).shift }
    if (response.shouldRetry(retryCount)) {
      DELETE(maybeTimeout, retryCount + 1, headers: _*)
    } else {
      response
    }

  def OPTIONS[A](headers: HttpHeader*): (HttpResponseMapper[A]) ?=> Http[HttpResponse[A]] =
    OPTIONS(None, 0, headers: _*)

  def OPTIONS[A](
      timeout: ScalaDuration,
      headers: HttpHeader*): (HttpResponseMapper[A]) ?=> Http[HttpResponse[A]] =
    OPTIONS(Option(timeout), 0, headers: _*)

  @tailrec
  private def OPTIONS[A](
      maybeTimeout: Option[ScalaDuration],
      retryCount: Int = 0,
      headers: HttpHeader*): (HttpResponseMapper[A]) ?=> Http[HttpResponse[A]] =
    val mapper = summon[HttpResponseMapper[A]]
    val config = summon[HttpClientConfig]
    val retries = config.maximumRetries.getOrElse(summon[HttpRetries])
    val requestBuilder = addHeadersToRequestBuilder(
      HttpRequest.newBuilder().method("OPTIONS", BodyPublishers.noBody).uri(uri))
    val response =
      handle(
        summon[HttpClient]
          .client
          .send(
            maybeTimeout
              .fold(requestBuilder) { fd =>
                requestBuilder.timeout(Duration.ofMillis(fd.toMillis))
              }
              .build(),
            mapper.bodyHandler)) { ex => HttpExecutionException(ex).shift }
    if (response.shouldRetry(retryCount)) {
      OPTIONS(maybeTimeout, retryCount + 1, headers: _*)
    } else response

  def TRACE[A](headers: HttpHeader*): HttpResponseMapper[A] ?=> Http[HttpResponse[A]] =
    TRACE[A](None, 0, headers: _*)

  def TRACE[A](
      timeout: ScalaDuration,
      headers: HttpHeader*): HttpResponseMapper[A] ?=> Http[HttpResponse[A]] =
    TRACE[A](Option(timeout), 0, headers: _*)

  @tailrec
  private def TRACE[A](
      maybeTimeout: Option[ScalaDuration],
      retryCount: Int = 0,
      headers: HttpHeader*): HttpResponseMapper[A] ?=> Http[HttpResponse[A]] =
    val mapper = summon[HttpResponseMapper[A]]
    val config = summon[HttpClientConfig]
    val retries = config.maximumRetries.getOrElse(summon[HttpRetries])
    val requestBuilder = addHeadersToRequestBuilder(
      HttpRequest.newBuilder().method("TRACE", BodyPublishers.noBody).uri(uri),
      headers: _*)
    val response =
      handle(
        summon[HttpClient]
          .client
          .send(
            maybeTimeout
              .fold(requestBuilder) { fd =>
                requestBuilder.timeout(Duration.ofMillis(fd.toMillis))
              }
              .build(),
            mapper.bodyHandler)) { ex => HttpExecutionException(ex).shift }
    if (response.shouldRetry(retryCount)) {
      TRACE(maybeTimeout, retryCount + 1, headers: _*)
    } else response

  def PATCH[A, B](body: B, headers: HttpHeader*)
      : (HttpResponseMapper[A], HttpBodyMapper[B]) ?=> Http[HttpResponse[A]] =
    PATCH(body, None, 0, headers: _*)
  def PATCH[A, B](body: B, timeout: ScalaDuration, headers: HttpHeader*)
      : (HttpResponseMapper[A], HttpBodyMapper[B]) ?=> Http[HttpResponse[A]] =
    PATCH(body, Option(timeout), 0, headers: _*)

  @tailrec
  private def PATCH[A, B](
      body: B,
      maybeTimeout: Option[ScalaDuration],
      retryCount: Int = 0,
      headers: HttpHeader*)
      : (HttpResponseMapper[A], HttpBodyMapper[B]) ?=> Http[HttpResponse[A]] =
    val mapper = summon[HttpResponseMapper[A]]
    val config = summon[HttpClientConfig]
    val retries = config.maximumRetries.getOrElse(summon[HttpRetries])
    val requestBuilder = addHeadersToRequestBuilder(
      HttpRequest
        .newBuilder()
        .method("PATCH", summon[HttpBodyMapper[B]].bodyPublisher(body))
        .uri(uri),
      headers: _*)
    val response =
      handle(
        summon[HttpClient]
          .client
          .send(
            maybeTimeout
              .fold(requestBuilder) { fd =>
                requestBuilder.timeout(Duration.ofMillis(fd.toMillis))
              }
              .build(),
            mapper.bodyHandler
          )) { ex => HttpExecutionException(ex).shift }
    if (response.shouldRetry(retryCount)) {
      PATCH(body, maybeTimeout, retryCount + 1, headers: _*)
    } else response

object Http:

  def apply[A](a: A): Http[A] =
    a
