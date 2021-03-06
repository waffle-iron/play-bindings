package com.wellfactored.playbindings

import play.api.libs.json.{Reads, Writes}
import play.api.mvc.{PathBindable, QueryStringBindable}
import shapeless.{::, Generic, HNil}

import scala.annotation.implicitNotFound

trait PlayBindings[V, W] {
  implicit def wraps: ValueWrapper[V, W]

  implicit def reads(implicit vr: Reads[V]): Reads[W]

  implicit def writes(implicit vw: Writes[V]): Writes[W]

  implicit def pathBindable(implicit pb: PathBindable[V]): PathBindable[W]

  implicit def queryStringBinder(implicit qb: QueryStringBindable[V]): QueryStringBindable[W]
}

object PlayBindings {

  private def noValidate[V](v: V): Either[String, V] = Right(v)

  @implicitNotFound(msg = "Cannot wrap a value of type ${V} with a class of type ${W}")
  def apply[V, W](validate: V => Either[String, V] = noValidate[V](_: V))(implicit gen: Generic.Aux[W, (V :: HNil)]): PlayBindings[V, W] = {

    new PlayBindings[V, W] {
      override implicit val wraps = new ValueWrapper[V, W] {
        override def wrap(v: V): Either[String, W] = validate(v).right.map(v => gen.from(v :: HNil))

        override def unwrap(w: W): V = gen.to(w).head
      }

      implicit def reads(implicit vr: Reads[V]): Reads[W] = PlayValueWrapper.reads[V, W]

      implicit def writes(implicit vw: Writes[V]): Writes[W] = PlayValueWrapper.writes[V, W]

      implicit def pathBindable(implicit pb: PathBindable[V]): PathBindable[W] = PlayValueWrapper.pathBindable[V, W]

      implicit def queryStringBinder(implicit qb: QueryStringBindable[V]): QueryStringBindable[W] =
        PlayValueWrapper.queryStringBindable[V, W]
    }
  }
}