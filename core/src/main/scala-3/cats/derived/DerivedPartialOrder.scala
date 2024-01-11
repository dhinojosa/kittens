package cats.derived

import cats.{Order, PartialOrder}
import shapeless3.deriving.Complete
import shapeless3.deriving.K0.*

import scala.annotation.*
import scala.compiletime.*

@implicitNotFound("""Could not derive an instance of PartialOrder[A] where A = ${A}.
Make sure that A satisfies one of the following conditions:
  * it is a case class where all fields have a PartialOrder instance
  * it is a sealed trait where all subclasses have a PartialOrder instance""")
type DerivedPartialOrder[A] = Derived[PartialOrder[A]]
object DerivedPartialOrder:
  type Or[A] = Derived.Or[PartialOrder[A]]

  @nowarn("msg=unused import")
  inline def apply[A]: PartialOrder[A] =
    import DerivedPartialOrder.given
    summonInline[DerivedPartialOrder[A]].instance

  @nowarn("msg=unused import")
  inline def strict[A]: PartialOrder[A] =
    import Strict.given
    summonInline[DerivedPartialOrder[A]].instance

  given singleton[A <: Singleton: ValueOf]: DerivedPartialOrder[A] =
    Order.allEqual

  given product[A](using inst: => ProductInstances[Or, A]): DerivedPartialOrder[A] =
    Strict.product(using inst.unify)

  given coproduct[A](using inst: => CoproductInstances[Or, A]): DerivedPartialOrder[A] =
    given CoproductInstances[PartialOrder, A] = inst.unify
    new Coproduct[PartialOrder, A] {}

  trait Product[T[x] <: PartialOrder[x], A](using inst: ProductInstances[T, A]) extends PartialOrder[A]:
    def partialCompare(x: A, y: A): Double =
      inst.foldLeft2(x, y)(0: Double):
        [t] =>
          (acc: Double, ord: T[t], t0: t, t1: t) =>
            val cmp = ord.partialCompare(t0, t1)
            Complete(cmp != 0)(cmp)(acc)

  trait Coproduct[T[x] <: PartialOrder[x], A](using inst: CoproductInstances[T, A]) extends PartialOrder[A]:
    def partialCompare(x: A, y: A): Double =
      inst.fold2(x, y)(Double.NaN: Double):
        [t] => (ord: T[t], t0: t, t1: t) => ord.partialCompare(t0, t1)

  object Strict:
    export DerivedPartialOrder.coproduct
    given product[A: ProductInstancesOf[PartialOrder]]: DerivedPartialOrder[A] =
      new Product[PartialOrder, A] {}
