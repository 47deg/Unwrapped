package sttp
package fx

import _root_.fx.{given, _}
import org.apache.http.entity.ContentType
import sttp.capabilities.Effect
import sttp.capabilities.Streams
import sttp.client3.Identity
import sttp.client3.Request
import sttp.client3.Response
import sttp.client3.SttpBackend
import sttp.client3.WebSocketResponseAs
import sttp.client3.internal.BodyFromResponseAs
import sttp.client3.internal.SttpFile
import sttp.client3.ws.GotAWebSocketException
import sttp.client3.ws.NotAWebSocketException
import sttp.model.Header
import sttp.model.Method
import sttp.model.Methods
import sttp.model.RequestMetadata
import sttp.model.ResponseMetadata
import sttp.monad.MonadError
import sttp.{model => sm}

import java.net.URI
import java.net.{http => jnh}
import jnh.HttpHeaders
import jnh.HttpRequest
import jnh.HttpResponse
import java.nio.charset.Charset
import java.nio.file.Files
import java.util.Optional
import java.util.zip.Deflater
import java.util.zip.Inflater
import scala.jdk.CollectionConverters.*
import sttp.client3.RequestBody
import sttp.client3.BasicRequestBody
import sttp.client3.StringBody

class HttpScalaFXBackend(using client: HttpClient, control: Control[Throwable])
    extends SttpBackend[Http, Streams[Receive[Byte]] with Effect[Http]] {
  type ??? = Any

  given MonadError[Http] = responseMonad

  private def headersToHeaders(headers: HttpHeaders): List[Header] = headers
    .map()
    .asScala
    .flatMap { kv => kv._2.asScala.map { headerValue => Header(kv._1, headerValue) } }
    .toList

  val inflater = new Inflater(false)

  val emptyResponse: Receive[Byte] = streamOf(Array.emptyByteArray: _*)

  private def replaceBodyWithEmptyBody(
      r: jnh.HttpResponse[_]): jnh.HttpResponse[Receive[Byte]] =
    new jnh.HttpResponse[Receive[Byte]] {
      def statusCode(): Int = r.statusCode()
      def request(): HttpRequest = r.request()
      def previousResponse() = Optional.empty()
      def headers(): HttpHeaders = r.headers()
      def body(): Receive[Byte] = emptyResponse
      def sslSession() = r.sslSession()
      def uri(): URI = r.uri()
      def version(): jnh.HttpClient.Version = r.version()
    }

  private def asT[T](request: Request[T, _], r: jnh.HttpResponse[Receive[Byte]]): Http[T] =
    r.fmap { r =>
      val metadata = ResponseMetadata(
        sttp.model.StatusCode.unsafeApply(r.statusCode()),
        "",
        headersToHeaders(r.headers()))
      new BodyFromResponseAs[Http, HttpResponse[Receive[Byte]], Nothing, Receive[Byte]] {

        private def responseWithReplayableBody(
            response: HttpResponse[Receive[Byte]],
            newBody: Receive[Byte]) =
          new HttpResponse[Receive[Byte]] {
            def statusCode(): Int = response.statusCode();
            def request(): HttpRequest = response.request();
            def previousResponse() = Optional.of(response);
            def headers(): HttpHeaders = response.headers();
            def body(): Receive[Byte] = newBody
            def sslSession() = response.sslSession();
            def uri(): URI = response.uri()
            def version(): jnh.HttpClient.Version = response.version();
          }

        override protected def withReplayableBody(
            response: HttpResponse[Receive[Byte]],
            replayableBody: Either[Array[Byte], SttpFile]): Http[HttpResponse[Receive[Byte]]] =
          Http(
            replayableBody.fold(
              bytes => responseWithReplayableBody(response, streamOf(bytes: _*)),
              file =>
                responseWithReplayableBody(response, streamed(Files.readAllBytes(file.toPath)))
            ))

        override protected def cleanupWhenGotWebSocket(
            response: Nothing,
            e: GotAWebSocketException): Http[Unit] = e.shift

        override protected def regularAsStream(
            response: HttpResponse[Receive[Byte]]): Http[(Receive[Byte], () => Http[Unit])] =
          Http((response.body(), () => Http(())))

        private def getContentType(headers: HttpHeaders): Option[String] =
          (headers.allValues("Content-Type").asScala ++ headers
            .allValues("content-type")
            .asScala ++ headers.allValues("Content-type").asScala ++ headers
            .allValues("content-type")
            .asScala).filter(_.contains("charset")).headOption

        override protected def regularAsFile(
            response: HttpResponse[Receive[Byte]],
            file: SttpFile): Http[SttpFile] = Http {
          // all this has to be wrapped by resource handling since we have
          // it, it may make sense to use plain old
          // FileImageOutputStream instead, and avoid all the content-type
          // shenanigans
          val charset: Charset = ContentType
            .parse(
              getContentType(response.headers()).getOrElse(
                "application/octet-stream; charset=utf-8")
            )
            .getCharset()
          val writer = Files.newBufferedWriter(file.toPath, charset)
          response
            .body()
            .grouped(4096)
            .receive(bytes => writer.append(new String(bytes.toArray, charset)))
          writer.flush()
          writer.close()
          file
        }

        override protected def regularAsByteArray(
            response: HttpResponse[Receive[Byte]]): Http[Array[Byte]] =
          Http(response.body().toList.toArray)

        override protected def handleWS[T](
            responseAs: WebSocketResponseAs[T, ?],
            meta: ResponseMetadata,
            ws: Nothing): Http[T] = new NotAWebSocketException(sm.StatusCode.Ok).shift

        override protected def cleanupWhenNotAWebSocket(
            response: HttpResponse[Receive[Byte]],
            e: NotAWebSocketException): Http[Unit] = e.shift

        override protected def regularIgnore(
            response: HttpResponse[Receive[Byte]]): Http[Unit] =
          Http(response.body()).fmap(_ => ())

      }.apply(request.response, metadata, Left(r))
    }
  private def requestMetadata(r: Request[_, _]): RequestMetadata =
    RequestMetadata(r.method, r.uri, r.headers)

  private def basicRequestBody(body: BasicRequestBody) =
    body match
    case StringBody(b: String, encoding: String, _) =>

  private def requestBody[R :> Streams[Receive[Byte]] with Effect[Http]](r: Request[_, R], body: RequestBody[R]) =
  body match {
    case NoBody => emptyResponse
    case BasicRequestBody
  }

  def send[T, R >: Streams[Receive[Byte]] with Effect[Http]](
      request: Request[T, R]): Http[Response[T]] = {
    val method: Identity[Method] = request.method
    val body = request.body
    val headers: List[HttpHeader] = request
      .headers
      .groupBy(_.name)
      .map { p =>
        val values = p._2.map(_.value).toList
        HttpHeader(p._1, values.headOption.getOrElse(""), values.tail: _*)
      }
      .toList
    val uri = request.uri

    val metadata = requestMetadata(request)
    val asRequestResponse  = asT(request, _)
    method match {
      case Method.GET =>

        asRequestResponse(uri.toJavaUri.GET[Receive[Byte]](headers: _*)).fmap{ ???} // need
                                                                                   // to
                                                                                   // map
                                                                                   // to
                                                                                   // sttp
                                                                                   // Response

      case Method.HEAD =>
        asRequestResponse(uri.toJavaUri.HEAD(headers: _*).fmap(replaceBodyWithEmptyBody)
      case Method.CONNECT => ??? // need to implement this as well... yuck
      case Method.DELETE =>
        asRequestResponse(
          uri.toJavaUri.DELETE[Receive[Byte]](headers: _*).fmap(replaceBodyWithEmptyBody))
      case Method.OPTIONS =>
        asRequestResponse(uri.toJavaUri.OPTIONS[Receive[Byte]](headers: _*))
      case Method.PATCH =>
        asRequestResponse(uri.toJavaUri.PATCH[Receive[Byte], ???](???, headers: _*)) // need
      // body
      // encoders,
      // and
      // need
      // to
      // figure
      // out
      // what
      // the
      // B
      // type
      // should
      // be. I
      // may
      // actually
      // need
      // a
      // stupid
      // aux
      // pattern
      // to
      // get
      // that... yuck.
      case Method.POST =>
        asRequestResponse(uri.toJavaUri.POST[Receive[Byte], ???](???, headers: _*))
      case Method.PUT =>
        asRequestResponse(uri.toJavaUri.PUT[Receive[Byte], ???](???, headers: _*))
      case Method.TRACE => asRequestResponse(uri.toJavaUri.TRACE[Receive[Byte]](headers: _*))
    }
  }

  def close(): Http[Unit] = ???

  def responseMonad: MonadError[Http] = new MonadError[Http] {
    def ensure[T](f: Http[T], e: => Http[Unit]): Http[T] = {
      val x: Throwable | T = run(f)
      x match {
        case ex: Throwable =>
          val ignored = run(e)
          f
        case a => unit(a.asInstanceOf[T]) // the only other possible
        // value is T. Since we
        // cannot change the
        // signature, we cast
      }
    }
    def error[T](t: Throwable): Http[T] = t.shift
    def flatMap[T, T2](fa: Http[T])(f: T => Http[T2]): Http[T2] = fa.bindMap(f)
    protected def handleWrappedError[T](rt: Http[T])(
        h: PartialFunction[Throwable, Http[T]]): Http[T] = {
      val x: Throwable | T = run(rt)
      x match {
        case ex: Throwable if h.isDefinedAt(ex) => h(ex)
        case ex: Throwable => ex.shift[T]
        case a => unit(a.asInstanceOf[T]) // because the only other
        // possible value is T, and
        // we cannot change the
        // signature to include a
        // manifest, we cast here

      }
    }
    def map[T, T2](fa: Http[T])(f: T => T2): Http[T2] = fa.fmap(f)
    def unit[T](t: T): Http[T] = Http(t)
  }
}
