package continuations

import dotty.tools.dotc.report
import dotty.tools.dotc.ast.Trees.*
import dotty.tools.dotc.ast.{tpd, Trees}
import dotty.tools.dotc.core.Constants.Constant
import dotty.tools.dotc.core.Contexts.{atPhase, Context}
import dotty.tools.dotc.core.Decorators.*
import dotty.tools.dotc.core.StdNames.*
import dotty.tools.dotc.core.Symbols
import dotty.tools.dotc.core.Types.{AppliedType, Type}
import dotty.tools.dotc.plugins.{PluginPhase, StandardPlugin}
import dotty.tools.dotc.semanticdb.TypeMessage.SealedValue.TypeRef
import dotty.tools.dotc.transform.{PickleQuotes, Staging}
import dotty.tools.dotc.core.{Flags, NameKinds}

import scala.annotation.tailrec

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

  override def transformDefDef(tree: tpd.DefDef)(using ctx: Context): Tree =
    tree match
      case t if returnsContextFunctionWithSuspendType(t) =>
        report.logWith("transformDefDef - new tree")(tree.show)
        report.logWith("transformDefDef - new tree")(tree)

      case _ =>
        report.logWith("transformDefDef - original tree")(tree.show)
        report.logWith("transformDefDef - original tree")(tree)

  override def transformBlock(tree: Block)(using ctx: Context): Tree =
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
    tree

  /*
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
   */

  def isSuspendType(tpe: Type)(using ctx: Context): Boolean =
    tpe.classSymbol.showFullName == "continuations.Suspend"

  def returnsContextFunctionWithSuspendType(tree: DefDef)(using ctx: Context): Boolean =
    if (hasSuspendParam(tree)) tree.rhs.existsSubTree(hasInnerSuspensionPoint)
    else false

  def hasSuspendParam(tree: tpd.DefDef)(using ctx: Context): Boolean =
    tree.paramss.reverse match {
      case ValDefs(vparams @ (vparam :: _)) :: _
          if vparam.mods.isOneOf(Flags.GivenOrImplicit) =>
        vparams.exists(vp => isSuspendType(vp.tpe))
      case _ => false
    }

  def hasInnerSuspensionPoint(subTree: Tree)(using ctx: Context): Boolean =
    subTree match
      case Inlined(Apply(fun, _), _, _) => isCallToSuspend(fun)
      case Apply(fun, _) => isCallToSuspend(fun)
      case _ => false

  def isCallToSuspend(tree: Tree)(using ctx: Context): Boolean =
    tree.symbol.name.show == "suspendContinuationOrReturn"

end ContinuationsPhase
