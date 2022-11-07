package continuations

import dotty.tools.dotc.ast.Trees.*
import dotty.tools.dotc.ast.tpd
import dotty.tools.dotc.core.Constants.Constant
import dotty.tools.dotc.core.Contexts.{ctx, Context}
import dotty.tools.dotc.core.Decorators.*
import dotty.tools.dotc.core.Flags
import dotty.tools.dotc.core.Names.termName
import dotty.tools.dotc.core.StdNames.*
import dotty.tools.dotc.core.Symbols.*
import dotty.tools.dotc.core.Types.{AppliedType, Type}
import dotty.tools.dotc.plugins.{PluginPhase, StandardPlugin}
import dotty.tools.dotc.report
import dotty.tools.dotc.transform.{PickleQuotes, Staging}

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer

class ContinuationsPlugin extends StandardPlugin:
  val name: String = "continuations"
  override val description: String = "CPS transformations"

  def init(options: List[String]): List[PluginPhase] =
    (new ContinuationsPhase) :: Nil

class ContinuationsPhase extends PluginPhase:
  import tpd.*

  val phaseName = "continuations"

  override val runsAfter = Set(Staging.name)
  override val runsBefore = Set(PickleQuotes.name)

  // override def transformBlock(tree: Block)(using ctx: Context): Tree =
//    //val state = suspensionState(tree)
//    report.error("DEEP FOLD")
//    tree.deepFold(())((acc, tree) => {
//      report.error(tree.show)
//      acc
//    })
//    report.error("SHALLOW FOLD")
//    tree.shallowFold(())((acc, tree) => {
//      report.error(tree.show)
//      acc
//    })
    // tree

    // report.logWith("transformBlock")(tree)
    // report.logWith("transformBlock")(tree.show)
    // tree

  var defDefTransforms: DefDefTransforms = _

  override def prepareForDefDef(tree: DefDef)(using Context): Context =
    defDefTransforms = new DefDefTransforms(
      requiredClass("continuations.Continuation"),
      requiredClass("continuations.SafeContinuation"),
      requiredPackage("continuations.intrinsics").requiredMethod("intercepted")
    )

    ctx

  override def transformDefDef(tree: DefDef)(using Context): Tree =
    defDefTransforms.transformSuspendContinuation(tree)

  @tailrec final def transformStatements(
      block: Block,
      statements: List[Tree],
      previous: List[Tree])(using ctx: Context): Block =
    statements match
      case Nil => block
      case current :: remaining =>
        if (hasInnerSuspensionPoint(current))
          val newBlock = Block(???, ???)
          transformStatements(newBlock, remaining, Nil)
        // TODO nest previous under suspension point. Look at trees dif with example function
        else transformStatements(block, remaining, previous :+ current) // this may be wrong

  def isSuspendType(tpe: Type)(using ctx: Context): Boolean =
    tpe.classSymbol.showFullName == "continuations.Suspend"

  def returnsContextFunctionWithSuspendType(tree: Tree)(using ctx: Context): Boolean =
    ctx.definitions.isContextFunctionType(tree.tpe) && tree.tpe.argTypes.exists(isSuspendType)

  def hasInnerSuspensionPoint(statement: Tree)(using ctx: Context): Boolean =
    statement.find(isCallToSuspend).isDefined

  def isCallToSuspend(tree: Tree)(using ctx: Context): Boolean =
    tree match
      case Apply(_, _) => returnsContextFunctionWithSuspendType(tree)
      case _ => false
