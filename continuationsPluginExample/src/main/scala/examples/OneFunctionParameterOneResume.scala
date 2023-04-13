package examples

import continuations.Suspend

@main def OneFunctionParameterOneResume =
  def foo(f: Int => Int)(using s: Suspend): Int =
    shift(_.resume(f(1)))
  println(foo(_ + 1))
