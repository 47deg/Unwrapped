package fx

import org.scalacheck.Properties
import org.scalacheck.Prop.forAll

object BindTests extends Properties("Bind Tests"):
  property("Binding two values of the same type") = forAll { (a: Int, b: Int) =>
    val effect: Int = Right(a).bind + Right(b).bind
    run(effect) == a + b
  }

  property("Binding two values of different types") = forAll { (a: Int, b: Int) =>
    val effect: Int = Right(a).bind + Some(b).bind
    run(effect) == a + b
  }

  property("Short-circuiting with Either.Left") = forAll { (n: Int, s: String) =>
    val effect: Control[String | None.type] ?=> Int =
      Left[String, Int](s).bind + Option(n).bind
    run(effect) == s
  }

  property("Short-circuiting with Option.None") = forAll { (n: Int) =>
    val effect: Control[None.type] ?=> Int =
      Right(n).bind + Option.empty[Int].bind
    run(effect) == None
  }
