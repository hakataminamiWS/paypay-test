package controllers

import javax.inject._
import paypay._
import play.api.data.Form
import play.api.mvc._
import scala.util.Failure
import scala.util.Success
import services.cancel.CancelService
import services.item.ItemService
import services.refund.RefundService

/** This controller creates an `Action` to handle HTTP requests to the application's home page.
  */
@Singleton
class HomeController @Inject() (
    components: MessagesControllerComponents,
    paypayRepository: PayPayRepository
) extends MessagesAbstractController(components) {
  implicit val repo = paypayRepository

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
      // for show the order contents in confirm page, set the order in session.
      val order = Order(item, ItemService.priceForItem(item))
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
        Ok(views.html.confirmOrder(orderForm.fill(order), postUrl))
          .withSession(
            request.session
              - itemInSession
              - priceInSession
              - merchantPaymentIdInSession
          )
          // for validating that the post content (of hashCode) and session data is matched.
          // because the post content is change-able, even if the form is read-only.
          .withSession(
            request.session
              + (confirmOrderHash -> order.hashCode.toString)
          )
    }
  }

  def createQRCode = Action { implicit request =>
    val errorFunction = { formWithError: Form[Order] =>
      BadRequest(views.html.index("Bad Request for create-qr"))
    }

    // user agent test
    import play.api.http.HeaderNames
    val agent: String = request.headers.get(HeaderNames.USER_AGENT).getOrElse("unknown")
    //
    val successFunction = (order: Order) => {
      PayPayApiClient.qrCodeFromOrder(order)(agent) match {
        case Failure(exception) => BadRequest(views.html.index(s"exception occur ${exception}"))
        case Success(qrCodeDetails) => {
          repo.insertPayment(qrCodeDetails)
          println(qrCodeDetails.getData.getUrl)
          println(agent)
          println(qrCodeDetails.getData().getRedirectUrl())
          Redirect(qrCodeDetails.getData.getUrl)
        }
      }
    }

    val formValidationResult = orderForm.bindFromRequest()
    formValidationResult.fold(errorFunction, successFunction)
  }

  def orderStatus(merchantPaymentId: String) = Action { implicit request =>
    val result = PayPayApiClient.fetchPaymentDetails(merchantPaymentId)

    result match {
      case Failure(exception) => BadRequest(views.html.index(s"exception occur ${exception}"))
      case Success(value) => {
        repo.updatePaymentStatus(value)
        val cancelUrl =
          if (CancelService.paymentIsCancellable(merchantPaymentId))
            Some(routes.HomeController.cancelOrder(Some(merchantPaymentId)))
          else None

        val refundUrl =
          if (RefundService.paymentIsCompleted(merchantPaymentId))
            Some(routes.HomeController.refundOrder(Some(merchantPaymentId)))
          else None

        Ok(views.html.orderStatus(value.toString, refundUrl, cancelUrl))
      }
    }
  }

  def orderList = Action { implicit request =>
    val allPayment = repo.findAllPayment.getOrElse(Map.empty)
    Ok(views.html.orderList(allPayment))
  }

  def refundOrder(optId: Option[String]) = Action { implicit request =>
    val refundOrder = for {
      id <- optId
      if RefundService.paymentIsRefundable(id)
      pe  <- repo.findPayment(id)
      pId <- pe.paymentId
      amo = pe.amount
    } yield RefundOrder(paymentId = pId, amount = amo, toMerchantPaymentId = id)

    RefundService.calculateRefundAmount(refundOrder) match {
      case None => BadRequest(views.html.index("the order cannot be refund"))
      case Some(order) => {
        val filledForm = refundOrderForm.fill(order)
        val postUrl    = routes.HomeController.refundApi
        Ok(views.html.refundOrder(filledForm, postUrl))
          // for validating that the post content (of hashCode) and session data is matched.
          .withSession(
            request.session
              + (confirmRefundOrderHash -> order.hashCode.toString)
          )
      }
    }
  }

  def refundApi = Action { implicit request =>
    val errorFunction = { formWithError: Form[RefundOrder] =>
      BadRequest(views.html.index("Bad Request for refund"))
    }

    val successFunction = (refundOrder: RefundOrder) => {
      PayPayApiClient.refundForOrder(refundOrder) match {
        case Failure(exception) => BadRequest(views.html.index(s"exception occur ${exception}"))
        case Success(refundDetails) => {
          repo.insertRefund(refundDetails)
          Redirect(routes.HomeController.refundStatus(refundDetails.getData.getMerchantRefundId))
        }
      }
    }

    val formValidationResult = refundOrderForm.bindFromRequest()
    formValidationResult.fold(errorFunction, successFunction)
  }

  def refundStatus(merchantRefundId: String) = Action { implicit request =>
    val result = PayPayApiClient.fetchRefundDetails(merchantRefundId)

    result match {
      case Failure(exception) => BadRequest(views.html.index(s"exception occur ${exception}"))
      case Success(value) => {
        repo.updateRefundStatus(value)
        Ok(views.html.refundStatus(value.toString))
      }
    }
  }

  def refundList = Action { implicit request =>
    val allRefund = repo.findAllRefund.getOrElse(Map.empty)
    Ok(views.html.refundList(allRefund))
  }

  def cancelOrder(optId: Option[String]) = Action { implicit request =>
    val cancelOrder = for {
      id <- optId
      if CancelService.paymentIsCancellable(id)
    } yield CancelOrder(merchantPaymentId = id)

    cancelOrder match {
      case None => BadRequest(views.html.index("the order cannot be canceled"))
      case Some(order) => {
        val filledForm = cancelOrderForm.fill(order)
        val postUrl    = routes.HomeController.cancelApi
        Ok(views.html.cancelOrder(filledForm, postUrl))
          // for validating that the post content (of hashCode) and session data is matched.
          .withSession(
            request.session
              + (confirmCancelOrderHash -> order.hashCode.toString)
          )
      }
    }
  }

  def cancelApi = Action { implicit request =>
    val errorFunction = { formWithError: Form[CancelOrder] =>
      BadRequest(views.html.index("Bad Request for cancel"))
    }

    val successFunction = (cancelOrder: CancelOrder) => {
      PayPayApiClient.cancelForOrder(cancelOrder) match {
        case Failure(exception) => BadRequest(views.html.index(s"exception occur ${exception}"))
        case Success(notDataResponse) => {
          Redirect(
            routes.HomeController.orderStatus(cancelOrder.merchantPaymentId)
          )
        }
      }
    }

    val formValidationResult = cancelOrderForm.bindFromRequest()
    formValidationResult.fold(errorFunction, successFunction)
  }
}
