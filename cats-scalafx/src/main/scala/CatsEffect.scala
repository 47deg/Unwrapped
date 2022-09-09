package fx
package cats

import _root_.cats.*
import _root_.cats.implicits.*
import _root_.cats.effect.*
import _root_.cats.effect.unsafe.*
import fx.run
import fx.Fiber

import java.util.concurrent.{CancellationException, CompletableFuture, Executors, Future}
import scala.annotation.unchecked.uncheckedVariance
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, ExecutionException}
import scala.util.{Failure, Success}

def toEffect[F[*]: [g[*]] =>> ApplicativeError[g, R], R: Manifest, A: Manifest](
    program: Control[R] ?=> A): F[A] =
  val x = run(program)
  x match {
    case x: A => x.pure
    case err: R => ApplicativeError[F, R].raiseError[A](err)
    case _ => throw RuntimeException("impossible!") // exhaustivity checker is wrong
  }

def fromIO[A](program: IO[A])(using runtime: IORuntime): Structured ?=> Fiber[A] =
  val fiber = CompletableFuture[A]()
  track(fiber)
  val (future, close) = program.unsafeToFutureCancelable()
  setupCancellation(fiber, close)
  given ExecutionContext = runtime.compute
  future.onComplete { trying =>
    trying match
      case s: Success[a] => fiber.complete(s.get)
      case f: Failure[a] => fiber.completeExceptionally(f.exception)
  }
  fiber.asInstanceOf[Fiber[A]]

def setupCancellation[A](fiber: CompletableFuture[A], close: () => scala.concurrent.Future[Unit]) =
  fiber.whenComplete { (_, exception) =>
    if (exception != null && exception.isInstanceOf[CancellationException])
      Await.result(close(), Duration.Inf)
  }
