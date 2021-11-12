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
import java.util.UUID

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

  case class MoneyAmountForJPY(
      price: Int
  ) extends MoneyAmount {
    this.setAmount(price)
    this.setCurrency(MoneyAmount.CurrencyEnum.JPY)
  }

  case class Order(item: Item, price: Int, merchantPaymentId: String = UUID.randomUUID.toString)

  val orderForm = Form(
    mapping(
      "item"              -> of[Item],
      "price"             -> number(min = 1, max = Int.MaxValue),
      "merchantPaymentId" -> nonEmptyText
    )(Order.apply)(Order.unapply)
  )

  // for order in session
  final val itemInSession              = "itemInSession"
  final val priceInSession             = "priceInSession"
  final val merchantPaymentIdInSession = "merchantPaymentIdInSession"
  final val confirmOrderHash           = "confirmOrderHash"

}
