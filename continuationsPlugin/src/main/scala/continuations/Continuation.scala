package continuations

import continuations.jvm.internal.BaseContinuationImpl

trait Continuation[-A]:
  type Ctx <: Tuple
  def context: Ctx
  def resume(value: Either[Throwable, A]): Unit
  def contextService[T](): T | Null =
    context.toList.find(_.isInstanceOf[T]).map(_.asInstanceOf[T]).orNull

object Continuation:
  enum State:
    case Suspended, Undecided, Resumed

  type Result = Either[Throwable, Any | Null | State.Suspended.type]

  def checkResult(result: Result): Unit =
    result match {
      case null => ()
      case Left(ex) => throw ex
      case Right(_) => ()
    }

  inline def Continuation[T, C <: Tuple](
      ctx: C,
      res: Either[Throwable, T] => Unit): Continuation[T] =
    new Continuation[T]:
      override type Ctx = C

      override def context: Ctx = ctx

      override def resume(value: Either[Throwable, T]): Unit = res(value)

end Continuation

abstract class RestrictedContinuation(
    completion: Continuation[Any | Null] | Null
) extends BaseContinuationImpl(completion):

  if (completion != null)
    require(completion.context == EmptyTuple)

  override type Ctx = EmptyTuple
  override val context: EmptyTuple = EmptyTuple
