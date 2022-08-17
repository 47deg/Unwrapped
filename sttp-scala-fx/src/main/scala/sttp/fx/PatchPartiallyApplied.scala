package sttp.fx

import _root_.fx.{given, *}
import java.net.URI
import java.net.http.HttpResponse
import scala.reflect.TypeTest

/**
 * Sttp's BodyFromResponseAs can only really work with responses bound to a statically known
 * response body type, which must be some kind of byte array or convertible to a byte array. To
 * maintain the capability of Http of serving any type of response with body type inference, we
 * are using [Rob Norris's Kinda-Curried Type
 * Parameters](https://tpolecat.github.io/2015/07/30/infer.html), accessed
 * 2022/08/11T23:01:00.000-5:00.
 */
private [fx] class PatchPartiallyApplied[A](private val uri: URI) extends Equals {
  def apply[B](body: B, headers: HttpHeader*)
        : (HttpResponseMapper[A], HttpBodyMapper[B]) ?=> Http[HttpResponse[A]] =
          uri.PATCH[A, B](body, headers: _*)

  override def canEqual(that: Any): Boolean = that.isInstanceOf[PatchPartiallyApplied[A]]
  override def equals(x: Any): Boolean = canEqual(x) && x.asInstanceOf[PatchPartiallyApplied[A]].uri == uri
}
