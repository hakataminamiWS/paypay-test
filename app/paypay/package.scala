import java.util.UUID
import jp.ne.paypay.model.MoneyAmount
import play.api.data.Form
import play.api.data.format.Formats
import play.api.data.format.Formatter
import play.api.data.FormError
import play.api.data.Forms.mapping
import play.api.data.Forms.nonEmptyText
import play.api.data.Forms.number
import play.api.data.Forms.of
import play.api.data.Forms.single
import play.api.data.Mapping
import play.api.data.validation.Constraint
import play.api.data.validation.Invalid
import play.api.data.validation.Valid
import play.api.data.validation.ValidationError
import play.api.mvc.RequestHeader

package object paypay {

  sealed trait Item
  case object ItemOne extends Item
  case object ItemTwo extends Item

  private val regexItemOne = "^ItemOne$".r
  private val regexItemTwo = "^ItemTwo$".r

  def parseItem(str: String): Option[Item] = str match {
    case regexItemOne() => Some(ItemOne)
    case regexItemTwo() => Some(ItemTwo)
    case _              => None
  }

  private implicit val itemFormatter = new Formatter[Item] {
    def bind(
        key: String,
        data: Map[String, String]
    ): Either[Seq[FormError], Item] = {
      Formats.stringFormat.bind(key, data).flatMap { value =>
        parseItem(value).toRight(Seq(FormError(key, "error.parseItem", Nil)))
      }
    }
    def unbind(key: String, value: Item): Map[String, String] = value match {
      case ItemOne => Map(key -> ItemOne.toString())
      case ItemTwo => Map(key -> ItemTwo.toString())
    }
  }

  val itemForm = Form(
    single(
      "item" -> of[Item]
    )
  )

  def hashCodeInSessionConstraint[T](sessionKey: String)(implicit
      request: RequestHeader
  ): Constraint[T] = {
    Constraint("constraints.hashCodeInSession")({ form =>
      val value = request.session.get(sessionKey)
      value match {
        case Some(hash) if (hash == form.hashCode.toString) => Valid
        case Some(_) => Invalid(ValidationError("error.hashCodeInSession"))
        case None    => Invalid(ValidationError("error.hashCodeInSession"))
      }
    })
  }

  /** for use of to confirm form value and to post with no change. this has a validation
    * form.hashCode == session value get by sessionKey.
    *
    * @param mapping
    * @param sessionKey
    * @param request
    * @return
    */
  private def readOnlyForm[T](mapping: Mapping[T])(sessionKey: String)(implicit
      request: RequestHeader
  ) = Form[T](mapping.verifying(hashCodeInSessionConstraint[T](sessionKey)))

  case class Order(
      item: Item,
      price: Int,
      merchantPaymentId: String = UUID.randomUUID.toString
  )
  final val confirmOrderHash = "confirmOrderHash"
  def orderForm(implicit request: RequestHeader) = readOnlyForm[Order](
    mapping(
      "item"              -> of[Item],
      "price"             -> number(min = 0, max = Int.MaxValue),
      "merchantPaymentId" -> nonEmptyText
    )(Order.apply)(Order.unapply)
  )(confirmOrderHash)

  // for send order at redirect use session
  final val itemInSession              = "itemInSession"
  final val priceInSession             = "priceInSession"
  final val merchantPaymentIdInSession = "merchantPaymentIdInSession"

  case class RefundOrder(
      merchantRefundId: String = UUID.randomUUID.toString,
      paymentId: String,
      amount: Int,
      toMerchantPaymentId: String
  )
  final val confirmRefundOrderHash = "confirmRefundOrderHash"
  def refundOrderForm(implicit request: RequestHeader) = readOnlyForm[RefundOrder](
    mapping(
      "merchantRefundId"    -> nonEmptyText,
      "paymentId"           -> nonEmptyText,
      "amount"              -> number(min = 0, max = Int.MaxValue),
      "toMerchantPaymentId" -> nonEmptyText
    )(RefundOrder.apply)(RefundOrder.unapply)
  )(confirmRefundOrderHash)

  case class CancelOrder(merchantPaymentId: String)
  final val confirmCancelOrderHash = "confirmCancelOrderHash"
  def cancelOrderForm(implicit request: RequestHeader) = readOnlyForm[CancelOrder](
    mapping(
      "merchantPaymentId" -> nonEmptyText
    )(CancelOrder.apply)(CancelOrder.unapply)
  )(confirmCancelOrderHash)
}
