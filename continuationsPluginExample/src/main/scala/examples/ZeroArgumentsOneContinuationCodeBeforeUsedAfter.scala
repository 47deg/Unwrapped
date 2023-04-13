package examples

import continuations.Suspend

@main def ZeroArgumentsOneContinuationCodeBeforeUsedAfter =
  def zeroArgumentsOneContinuationCodeBeforeUsedAfter()(using s: Suspend): Int =
    val x = 1
    shift[Unit](_.resume(println("Hello")))
    val y = 1
    x + y
  println(zeroArgumentsOneContinuationCodeBeforeUsedAfter())
