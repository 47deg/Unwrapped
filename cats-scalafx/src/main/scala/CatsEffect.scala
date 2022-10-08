package fx
package cats

import _root_.cats.effect.*
import _root_.cats.effect.unsafe.implicits.*
import fx.run
import fx.Fiber

import java.util.concurrent.{CancellationException, CompletableFuture, Future}
import scala.concurrent.ExecutionException

def toIO[R, A](program: Control[R] ?=> A): IO[R | A] =
  IO(run(program))

def fromIO[A](program: IO[A]): Structured ?=> Fiber[A] =
  val fiber = CompletableFuture[A]()
  track(fiber)
  val future = program.unsafeToCompletableFuture()
  setupCancellation(fiber, future)
  future.whenComplete { (a, exception) =>
    if (a != null)
      fiber.complete(a)
    else fiber.completeExceptionally(exception)
  }
  fiber.asInstanceOf[Fiber[A]]

def setupCancellation[A](fiber: Future[A], future: CompletableFuture[A]) =
  future.whenComplete { (_, exception) =>
    if (exception != null && exception.isInstanceOf[CancellationException])
      fiber.cancel()
  }
