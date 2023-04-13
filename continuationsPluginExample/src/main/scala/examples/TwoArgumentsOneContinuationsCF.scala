package examples

import continuations.Suspend

@main def TwoArgumentsOneContinuationsCF =
  def twoArgumentsOneContinuationsCF(x: Int, y: Int): Suspend ?=> Int =
    shift[Int](_.resume(x + y + 1))
  println(twoArgumentsOneContinuationsCF(1, 1))
