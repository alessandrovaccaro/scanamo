package org.scanamo

import com.amazonaws.services.dynamodbv2.model.AttributeValue
import java.nio.ByteBuffer
import org.scanamo.error.DynamoReadError

/**
  * A `DynamoValue` is a pure representation of an `AttributeValue` from the AWS SDK.
  */
sealed abstract class DynamoValue extends Product with Serializable { self =>
  import DynamoValue._

  /**
    * Produces the `AttributeValue` isomorphic to this `DynamoValue`
    */
  def toAttributeValue: AttributeValue = self match {
    case DynNull        => Null
    case DynBool(true)  => True
    case DynBool(false) => False
    case DynNum(n)      => new AttributeValue().withN(n.toString)
    case DynString(s)   => new AttributeValue().withS(s)
    case DynByte(b)     => new AttributeValue().withB(b)
    case DynObject(as)  => as.toAttributeValue
    case DynArray(as)   => as.toAttributeValue
  }

  /**
    * Checks whether this object represents the null object
    */
  final def isNull: Boolean = self match {
    case DynNull => true
    case _       => false
  }

  /**
    * Checks whether this object represents a boolean
    */
  final def isBoolean: Boolean = self match {
    case _: DynBool => true
    case _          => false
  }

  /**
    * Checks whether this object represents a number
    */
  final def isNumber: Boolean = self match {
    case _: DynNum => true
    case _         => false
  }

  /**
    * Checks whether this object represents a string
    */
  final def isString: Boolean = self match {
    case _: DynString => true
    case _            => false
  }

  /**
    * Checks whether this object represents a byte buffer
    */
  final def isByteBuffer: Boolean = self match {
    case _: DynByte => true
    case _          => false
  }

  /**
    * Checks whether this object rerpresents a composite object
    */
  final def isObject: Boolean = self match {
    case _: DynObject => true
    case _            => false
  }

  /**
    * Checks whether this object rerpresents an array
    */
  final def isArray: Boolean = self match {
    case _: DynArray => true
    case _           => false
  }

  /**
    * Produces `()` is this object is null
    */
  final def asNull: Option[Unit] = self match {
    case DynNull => Some(())
    case _       => None
  }

  /**
    * Produces the underlying boolean, if applies
    */
  final def asBoolean: Option[Boolean] = self match {
    case DynBool(b) => Some(b)
    case _          => None
  }

  /**
    * Produces the underlying string, if applies
    */
  final def asString: Option[String] = self match {
    case DynString(s) => Some(s)
    case _            => None
  }

  /**
    * Produces the underlying number, if applies
    */
  final def asNumber: Option[String] = self match {
    case DynNum(n) => Some(n)
    case _         => None
  }

  /**
    * Produces the underlying byte buffer, if applies
    */
  final def asByteBuffer: Option[ByteBuffer] = self match {
    case DynByte(b) => Some(b)
    case _          => None
  }

  /**
    * Produces the underlying array, if applies
    */
  final def asArray: Option[DynamoArray] = self match {
    case DynArray(as) => Some(as)
    case _            => None
  }

  /**
    * Produces the underlying object, if applies
    */
  final def asObject: Option[DynamoObject] = self match {
    case DynObject(as) => Some(as)
    case _             => None
  }

  /**
    * Transforms into a value of type `A` for which there is a codec, if applies
    */
  final def as[A](implicit A: DynamoFormat[A]): Either[DynamoReadError, A] = A.read(self)

  /**
    * Returns this value if it isn't null, the provided one otherwise (which may be null too),
    * this allows expressions like: v1 orElse v2 orElse v3 ... or even vs.foldLeft(nil)(_ orElse _)
    */
  final def orElse(that: DynamoValue): DynamoValue = self match {
    case DynNull => that
    case _       => self
  }

  /**
    * Transforms into a new value if this one is null
    */
  final def withNull(f: => DynamoValue): DynamoValue = self match {
    case DynNull => f
    case _       => self
  }

  /**
    * Transforms into a new value if this one is a boolean
    */
  final def withBoolean(f: Boolean => DynamoValue): DynamoValue = self match {
    case DynBool(b) => f(b)
    case _          => self
  }

  /**
    * Transforms into a new value if this one is a string
    */
  final def withString(f: String => DynamoValue): DynamoValue = self match {
    case DynString(s) => f(s)
    case _            => self
  }

  /**
    * Transforms into a new value if this one is a number
    */
  final def withNumber(f: String => DynamoValue): DynamoValue = self match {
    case DynNum(n) => f(n)
    case _         => self
  }

  /**
    * Transforms into a new value if this one is a byte buffer
    */
  final def withByteBuffer(f: ByteBuffer => DynamoValue): DynamoValue = self match {
    case DynByte(b) => f(b)
    case _          => self
  }

  /**
    * Transforms into a new value if this one is an array
    */
  final def withArray(f: DynamoArray => DynamoValue): DynamoValue = self match {
    case DynArray(as) => f(as)
    case _            => self
  }

  /**
    * Transforms into a new value if this one is a map
    */
  final def withObject(f: DynamoObject => DynamoValue): DynamoValue = self match {
    case DynObject(as) => f(as)
    case _             => self
  }
}

object DynamoValue {
  private[scanamo] val Null: AttributeValue = new AttributeValue().withNULL(true)
  private[scanamo] val True: AttributeValue = new AttributeValue().withBOOL(true)
  private[scanamo] val False: AttributeValue = new AttributeValue().withBOOL(false)
  private[scanamo] val EmptyList: AttributeValue = new AttributeValue().withL()

  private[DynamoValue] case object DynNull extends DynamoValue
  final private[DynamoValue] case class DynBool(b: Boolean) extends DynamoValue
  final private[DynamoValue] case class DynNum(n: String) extends DynamoValue
  final private[DynamoValue] case class DynString(s: String) extends DynamoValue
  final private[DynamoValue] case class DynByte(b: ByteBuffer) extends DynamoValue
  final private[DynamoValue] case class DynArray(as: DynamoArray) extends DynamoValue
  final private[DynamoValue] case class DynObject(as: DynamoObject) extends DynamoValue

  /**
    * The `null` value
    */
  val nil: DynamoValue = DynNull

  /**
    * Creates a boolean value
    */
  def fromBoolean(b: Boolean): DynamoValue = DynBool(b)

  /**
    * Creates a numeric value
    */
  def fromNumber[N: Numeric](n: N): DynamoValue = DynNum(n.toString)

  /**
    * Creates a string value
    */
  def fromString(s: String): DynamoValue = DynString(s)

  /**
    * Creates a byte buffer value
    */
  def fromByteBuffer(b: ByteBuffer): DynamoValue = DynByte(b)

  /**
    * Creates an array of values
    */
  def fromValues(as: Iterable[DynamoValue]): DynamoValue = DynArray(DynamoArray(as))

  /**
    * Creates aa map of values
    */
  def fromFields(as: (String, DynamoValue)*): DynamoValue = DynObject(DynamoObject(as.toMap))

  /**
    * Builds a map of values from an actual `Map`
    */
  def fromMap(as: Map[String, DynamoValue]): DynamoValue = DynObject(DynamoObject(as))

  /**
    * Creates an array of numbers
    */
  def fromNumbers[N: Numeric](ns: Iterable[N]): DynamoValue = DynArray(DynamoArray.numbers(ns))

  /**
    * Creates an array of strings
    */
  def fromStrings(ss: Iterable[String]): DynamoValue = DynArray(DynamoArray.strings(ss))

  /**
    * Creates an array of byte buffers
    */
  def fromByteBuffers(bs: Iterable[ByteBuffer]): DynamoValue = DynArray(DynamoArray.byteBuffers(bs))

  /**
    * Creats a pure value from an `AttributeValue`
    */
  final def fromAttributeValue(av: AttributeValue): DynamoValue =
    if (!(av.isNULL eq null) && av.isNULL)
      DynNull
    else if (av.getBOOL ne null)
      DynBool(av.getBOOL)
    else if (av.getN ne null)
      DynNum(av.getN)
    else if (av.getS ne null)
      DynString(av.getS)
    else if (av.getB ne null)
      DynByte(av.getB)
    else if (av.getNS ne null)
      DynArray(DynamoArray.unsafeNumbers(av.getNS))
    else if (av.getBS ne null)
      DynArray(DynamoArray.byteBuffers(av.getBS))
    else if (av.getSS ne null)
      DynArray(DynamoArray.strings(av.getSS))
    else if (av.getL ne null)
      DynArray(DynamoArray(av.getL))
    else
      DynObject(DynamoObject(av.getM))

  /**
    * Creates a map from a [[DynamoObject]]
    */
  def fromDynamoObject(xs: DynamoObject): DynamoValue = DynObject(xs)

  /**
    * Creates an array from a [[DynamoArray]]
    */
  def fromDynamoArray(xs: DynamoArray): DynamoValue = DynArray(xs)

  private[scanamo] def unsafeFromNumber(n: String): DynamoValue = DynNum(n)
}
