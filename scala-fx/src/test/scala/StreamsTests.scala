package fx

import org.scalacheck.Properties
import org.scalacheck.Prop.forAll

object StreamsTests extends Properties("Streams tests"):

  property("send/receive") = forAll { (n: Int) => streamOf(n).toList == List(n) }

  property("toList") = forAll { (list: List[Int]) => streamOf(list*).toList == list }

  property("zipWithIndex") = forAll { (list: List[Int]) =>
    streamOf(list*).zipWithIndex.toList == list.zipWithIndex
  }

  property("flatten: identity") = forAll { (n: Int) =>
    streamOf(n).map(i => streamOf(i)).flatten.toList == List(n)
  }

  property("flatten") = forAll { (list: List[Int]) =>
    streamOf(list*).map(i => streamOf(i, i + 1)).flatten.toList == list.flatMap(i =>
      List(i, i + 1))
  }

  property("fold") = forAll { (initial: Int, a: Int, b: Int) =>
    streamOf(a, b).fold(initial, _ + _).toList == List(initial, initial + a, initial + a + b)
  }

  property("map") = forAll { (n: Int) => streamOf(n).map(_ + 1).toList == List(n + 1) }

  property("flatMap") = forAll { (n: Int) =>
    streamOf(n).flatMap(n => streamOf(n + 1)).toList == List(n + 1)
  }

  property("comprehensions") = forAll { (n: Int) =>
    val r = for {
      a <- streamOf(n)
      b <- streamOf(n)
    } yield a + b
    r.toList == List(n + n)
  }

end StreamsTests
