package unwrapped

import org.scalacheck.Prop.forAll
import org.scalacheck.Properties
import org.scalacheck.Test.Parameters

import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicReference
import scala.util.control.NonFatal

object StructuredTests extends Properties("Structured Concurrency Tests"):

  override def overrideParameters(p: Parameters) =
    p.withMinSuccessfulTests(10000)

  property("parallel runs in parallel") = forAll { (a: Int, b: Int) =>
    val r = AtomicReference("")
    val modifyGate = CompletableFuture[Int]()
    structured(
      parallel(
        () =>
          modifyGate.join
          r.updateAndGet { i => s"$i$a" }
        ,
        () =>
          r.set(s"$b")
          modifyGate.complete(0)
      )
    )
    r.get() == s"$b$a"
  }

  property("concurrent shift on fork join propagates") = forAll { (a: Int, b: Int) =>
    val x: Control[Int] ?=> Structured ?=> String =
      val fa = fork[String](() => a.shift)
      val fb = fork[String](() => b.shift)
      fa.join + fb.join

    val value: String | Int = run(structured(x))

    List(a, b).contains(value)
  }

  property("concurrent shift on fork that doesn't join does not propagate") = forAll {
    (a: Int, b: Int, c: String) =>
      val x: Control[Int] ?=> Structured ?=> String =
        val fa = fork[Nothing](() => a.shift)
        val fb = fork[Nothing](() => b.shift)
        c

      c == run(structured(x))
  }

end StructuredTests
