package controllers

import javax.inject._
import paypay._
import play.api.data.Form
import play.api.mvc._
import scala.util.Failure
import scala.util.Success

/** This controller creates an `Action` to handle HTTP requests to the application's home page.
  */
@Singleton
class HomeController @Inject() (
    components: MessagesControllerComponents,
    repo: PayPayRepository
) extends MessagesAbstractController(components) {

  /** Create an Action to render an HTML page.
    *
    * The configuration in the `routes` file means that this method will be called when the
    * application receives a `GET` request with a path of `/`.
    */
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index("Welcome to Play!"))
  }

  def order = Action { implicit request: MessagesRequest[AnyContent] =>
    val postUrl = routes.HomeController.checkOrder
    Ok(views.html.order(itemForm, postUrl))
  }

  def checkOrder = Action { implicit request: MessagesRequest[AnyContent] =>
    val postUrl = routes.HomeController.checkOrder

    val errorFunction = { formWithError: Form[Item] =>
      BadRequest(views.html.order(formWithError, postUrl))
    }

    val successFunction = { item: Item =>
      def priceForItem(item: Item): Int = item match {
        case ItemOne => 10
        case ItemTwo => 20
      }

      // for show the order contents in confirm page, set the order in session.
      val order = Order(item, priceForItem(item))
      Redirect(routes.HomeController.confirmOrder)
        .withSession(
          request.session +
            (itemInSession              -> order.item.toString) +
            (priceInSession             -> order.price.toString) +
            (merchantPaymentIdInSession -> order.merchantPaymentId)
        )
    }

    val formValidationResult = itemForm.bindFromRequest()
    formValidationResult.fold(errorFunction, successFunction)
  }

  def confirmOrder = Action { implicit request: MessagesRequest[AnyContent] =>
    val optOrderInSession = for {
      item      <- request.session.get(itemInSession).flatMap(parseItem(_))
      price     <- request.session.get(priceInSession).flatMap(_.toIntOption)
      paymentId <- request.session.get(merchantPaymentIdInSession)
    } yield Order(item, price, paymentId)

    val postUrl = routes.HomeController.createQRCode

    optOrderInSession match {
      case None => BadRequest(views.html.index("Bad Request for confirm-order"))

      case Some(order) =>
        // for validating that the post content (of hashCode) and session data is matched.
        // because the post content is change-able, even if the form is read-only.
        Ok(views.html.confirmOrder(orderForm.fill(order), postUrl))
          .withSession(
            request.session
              - itemInSession
              - priceInSession
              - merchantPaymentIdInSession
          )
          .withSession(
            request.session
              + (confirmOrderHash -> order.hashCode.toString)
          )
    }
  }

  def createQRCode = Action { implicit request =>
    println(request.session.data)

    val errorFunction = { formWithError: Form[Order] =>
      BadRequest(views.html.index("Bad Request for create-qr"))
    }

    val successFunction = (order: Order) => {
      val confirmedOrder =
        request.session
          .get(confirmOrderHash)
          .map(hash => if (hash == order.hashCode.toString) Some(order) else None)
          .flatten

      confirmedOrder match {
        case None =>
          BadRequest(
            views.html.index(s"the order.hashCode in order and confirm-order does not match.")
          )
        case Some(o) => {
          PayPayApiClient.qrCodeFromOrder(o) match {
            case Failure(exception) => BadRequest(views.html.index(s"exception occur ${exception}"))
            case Success(qrCodeDetails) => {
              repo.insertPayment(qrCodeDetails)
              Redirect(qrCodeDetails.getData.getUrl())
            }
          }
        }
      }
    }

    val formValidationResult = orderForm.bindFromRequest()

    formValidationResult.fold(errorFunction, successFunction)
  }

  def orderStatus(merchantPaymentId: String) = Action { implicit request =>
    val result = PayPayApiClient.fetchPaymentDetails(
      merchantPaymentId
    )

    result match {
      case Failure(exception) => Ok(exception.toString())
      case Success(value) => {
        repo.updatePaymentStatus(value)
        Ok(views.html.index(value.toString))
      }
    }
  }

  def orderList = Action { implicit request =>
    Ok(views.html.orderList(repo.findAllPayment))
  }

  def refund = ???

}
