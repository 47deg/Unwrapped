package fx

import com.sun.net.httpserver.*
import munit.fx.ScalaFXSuite

import java.net.InetSocketAddress
import java.net.URI
import java.util.concurrent.Executors
import scala.jdk.CollectionConverters.*
import java.net.http.HttpHeaders
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import scala.jdk.FunctionConverters.*

class HttpSuite extends ScalaFXSuite {

  httpServer(getHttpHandler(Option.empty))
    .testFX("requests should be returned in non-blocking fibers") { serverResource =>
      serverResource.use { baseServerAddress =>
        val pongResponse: Structured ?=> (
            String,
            String,
            String,
            String,
            String,
            String,
            String,
            String,
            String,
            String,
            String,
            String,
            String,
            String,
            String,
            String,
            String,
            String,
            String,
            String) =
          parallel(
            () => Http.GET[String](URI.create(s"$baseServerAddress/ping/1")).join.body,
            () => Http.GET[String](URI.create(s"$baseServerAddress/ping/2")).join.body,
            () => Http.GET[String](URI.create(s"$baseServerAddress/ping/3")).join.body,
            () => Http.GET[String](URI.create(s"$baseServerAddress/ping/4")).join.body,
            () => Http.GET[String](URI.create(s"$baseServerAddress/ping/5")).join.body,
            () => Http.GET[String](URI.create(s"$baseServerAddress/ping/6")).join.body,
            () => Http.GET[String](URI.create(s"$baseServerAddress/ping/7")).join.body,
            () => Http.GET[String](URI.create(s"$baseServerAddress/ping/8")).join.body,
            () => Http.GET[String](URI.create(s"$baseServerAddress/ping/9")).join.body,
            () => Http.GET[String](URI.create(s"$baseServerAddress/ping/0")).join.body,
            () => Http.GET[String](URI.create(s"$baseServerAddress/ping/11")).join.body,
            () => Http.GET[String](URI.create(s"$baseServerAddress/ping/12")).join.body,
            () => Http.GET[String](URI.create(s"$baseServerAddress/ping/13")).join.body,
            () => Http.GET[String](URI.create(s"$baseServerAddress/ping/14")).join.body,
            () => Http.GET[String](URI.create(s"$baseServerAddress/ping/15")).join.body,
            () => Http.GET[String](URI.create(s"$baseServerAddress/ping/16")).join.body,
            () => Http.GET[String](URI.create(s"$baseServerAddress/ping/17")).join.body,
            () => Http.GET[String](URI.create(s"$baseServerAddress/ping/18")).join.body,
            () => Http.GET[String](URI.create(s"$baseServerAddress/ping/19")).join.body,
            () => Http.GET[String](URI.create(s"$baseServerAddress/ping/20")).join.body
          )

        assertEqualsFX(
          structured(pongResponse),
          (
            "pong",
            "pong",
            "pong",
            "pong",
            "pong",
            "pong",
            "pong",
            "pong",
            "pong",
            "pong",
            "pong",
            "pong",
            "pong",
            "pong",
            "pong",
            "pong",
            "pong",
            "pong",
            "pong",
            "pong"
          )
        )
      }
    }

  httpServer(getHttpHandler(Option(Headers.of("X-Extended-Test-Header", "HttpSuite"))))
    .testFX("Expected headers are sent with requests, 404 means headers not sent") {
      (serverAddressResource: Resource[String]) =>
        serverAddressResource.use { baseServerAddress =>
          assertEqualsFX(
            structured(
              Http
                .GET[String](
                  URI.create(s"$baseServerAddress/ping/1"),
                  HttpHeader("X-Extended-Test-Header", "HttpSuite"))
                .join
                .body),
            "pong"
          )
        }
    }

  httpServer(getHttpFailureHandler(Option.empty, AtomicInteger(3)))
    .testFX("By default, failed requests should retry 3 times") { serverAddressResource =>
      serverAddressResource.use { baseServerAddress =>
        {
          assertEqualsFX(
            structured(
              Http.GET[String](URI.create(s"$baseServerAddress/ping/fail")).join.statusCode),
            200)
        }
      }
    }

  httpServer(getHttpFailureHandler(Option.empty, AtomicInteger(4)))
    .testFX("More than three request failures should return the server error by default") {
      serverAddressResource =>
        serverAddressResource.use { baseServerAddress =>
          {
            assertEqualsFX(
              structured(
                Http.GET[String](URI.create(s"$baseServerAddress/ping/fail")).join.statusCode),
              500)
          }
        }
    }

  httpServer(postHttpSuccessHandler).testFX(
    "requests with string post bodies are successuful") { serverAddressResource =>
    serverAddressResource.use { baseServeraddress =>
      assertEqualsFX(
        structured(Http.POST[String](URI.create(s"$baseServeraddress/ping"), "paddle"))
          .join
          .statusCode,
        201)
    }
  }

  httpServer(headHttpSuccessHandler).testFX("Head should send a HEAD request") {
    serverAddressResource =>
      serverAddressResource.use { baseServerAddress =>
        assertEqualsFX(
          structured(Http.HEAD(URI.create(s"$baseServerAddress/ping"))).join.statusCode,
          200
        )
      }
  }

  httpServer(putHttpSuccessHandler).testFX("PUT should send a PUT request") {
    serverAddressResource =>
      serverAddressResource.use { baseServerAddress =>
        assertEqualsFX(
          structured(Http.PUT[String, String](URI.create(s"$baseServerAddress/ping"), "paddle"))
            .join
            .statusCode,
          204
        )
      }
  }

  httpServer(deleteHttpSuccessHandler).testFX("DELETE should send a DELETE request") {
    serverAddressResource =>
      serverAddressResource.use { baseServerAddress =>
        assertEqualsFX(
          structured(Http.DELETE[String](URI.create(s"$baseServerAddress/ping")))
            .join
            .statusCode,
          204)
      }
  }

  httpServer(optionsHttpSuccessHandler).testFX("OPTIONS should send an Options request") {
    serverAddressResource =>
      serverAddressResource.use { baseServerAddress =>
        assertEqualsFX(
          structured(
            Http.OPTIONS[String](URI.create(s"$baseServerAddress/ping")).join.statusCode),
          200)
      }
  }

  httpServer(traceHttpSuccessHandler).testFX("TRACE should send a trace request") {
    serverAddressResource =>
      serverAddressResource.use { baseServerAddress =>
        assertEqualsFX(
          structured(
            Http.TRACE[String](URI.create(s"$baseServerAddress/ping")).join.statusCode),
          200)
      }
  }

  lazy val notFoundHeaders = Headers.of(
    "Content-Type",
    "text/plain; charset=UTF-8",
    "Last-Modified",
    "Thu, 10 Mar 2022 23:21:48 GMT",
    "Connection",
    "keep-alive",
    "Date",
    "Fri, 01 Jul 2022 04:22:42 GMT"
  )
  lazy val getSuccessHeaders = Headers.of(
    "Content-Type",
    "text/plain; charset=UTF-8",
    "Last-Modified",
    "Thu, 10 Mar 2022 23:21:48 GMT",
    "Connection",
    "keep-alive",
    "Date",
    "Fri, 01 Jul 2022 04:22:42 GMT"
  )
  lazy val serverProblemHeaders = Headers.of(
    "Content-Type",
    "text/plain; charset=UTF-8",
    "Connection",
    "close",
    "Date",
    "Fri, 01 Jul 2022 04:22:42 GMT"
  )

  lazy val traceHttpSuccessHandler = HttpHandlers.handleOrElse(
    request =>
      request
        .getRequestMethod() == "TRACE" && request.getRequestURI().getPath().contains("ping"),
    HttpHandlers.of(200, getSuccessHeaders, ""),
    fallbackHttpHandler
  )

  lazy val optionsHttpSuccessHandler = HttpHandlers.handleOrElse(
    request =>
      request
        .getRequestMethod() == "OPTIONS" && request.getRequestURI().getPath().contains("ping"),
    HttpHandlers.of(200, getSuccessHeaders, ""),
    fallbackHttpHandler
  )

  lazy val deleteHttpSuccessHandler = HttpHandlers.handleOrElse(
    request =>
      request
        .getRequestMethod() == "DELETE" && request.getRequestURI().getPath().contains("ping"),
    HttpHandlers.of(204, getSuccessHeaders, ""),
    fallbackHttpHandler
  )

  lazy val putHttpSuccessHandler = HttpHandlers.handleOrElse(
    request =>
      request.getRequestMethod() == "PUT" && request.getRequestURI().getPath().contains("ping"),
    HttpHandlers.of(204, getSuccessHeaders, ""),
    fallbackHttpHandler
  )

  lazy val headHttpSuccessHandler = HttpHandlers.handleOrElse(
    request =>
      request
        .getRequestMethod() == "HEAD" && request.getRequestURI().getPath().contains("ping"),
    HttpHandlers.of(200, getSuccessHeaders, "OK"),
    fallbackHttpHandler
  )

  lazy val postHttpSuccessHandler =
    HttpHandlers.handleOrElse(
      (request: Request) => {
        request
          .getRequestMethod() == "POST" && request.getRequestURI().getPath().contains("ping")
      },
      HttpHandlers.of(201, getSuccessHeaders, "Created"),
      fallbackHttpHandler
    )

  def httpServer(handler: HttpHandler) = FunFixture(
    setup = _ => {
      for {
        server <- Resource(
          HttpServer.create(InetSocketAddress(0), 0),
          (server, _) => server.stop(0))
        serverExecutor <- Resource(
          Executors.newVirtualThreadPerTaskExecutor,
          (executor, _) => executor.shutdown())
        _ = server.setExecutor(serverExecutor)
        httpContext = server.createContext("/root", handler)
        _ = server.start
      } yield s"http:/${server.getAddress()}/root"
    },
    teardown = server => {
      ()
    }
  )

  def getHttpFailureHandler(
      maybeExpectedHeaders: Option[Headers],
      numRequiredFailures: AtomicInteger): HttpHandler =
    val serverFail = HttpHandlers.of(500, serverProblemHeaders, "Server Error")
    val successHandler = getHttpHandler(maybeExpectedHeaders)
    HttpHandlers.handleOrElse(
      _ => {
        numRequiredFailures.getAndDecrement > 0
      },
      serverFail,
      successHandler
    )

  lazy val fallbackHttpHandler = HttpHandlers.of(404, notFoundHeaders, "Not Found")

  def getHttpHandler(maybeExpectedHeaders: Option[Headers]) =
    HttpHandlers.handleOrElse(
      (request: Request) => {
        val hasAllExpectedHeaders = maybeExpectedHeaders.map {
          _.entrySet.asScala.forall { entrySet =>
            val headerName = entrySet.getKey
            request.getRequestHeaders.containsKey(headerName) && request
              .getRequestHeaders
              .get(headerName)
              .containsAll(entrySet.getValue)
          }
        }
        hasAllExpectedHeaders.getOrElse(true) && request
          .getRequestMethod() == "GET" && request.getRequestURI().getPath().contains("ping")
      },
      HttpHandlers.of(200, getSuccessHeaders, "pong"),
      fallbackHttpHandler
    )

  lazy val virtualThreadExecutor = FunFixture(
    setup = _ => Executors.newVirtualThreadPerTaskExecutor,
    teardown = executor => {
      println("tearing down executor")
      executor.shutdown
    })
}
