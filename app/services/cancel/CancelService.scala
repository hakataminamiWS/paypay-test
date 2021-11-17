package services.cancel

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.time.ZonedDateTime
import java.time.ZoneId
import jp.ne.paypay.model.PaymentState
import paypay.PaymentEntity
import paypay.PayPayRepository
import play.api.Logging

object CancelService extends Logging {

  /** PayPay developer's Note: So you want to cancel a Payment. In most cases this should not be
    * needed for payment happening in this flow, however following can be a case when this might be
    * needed.
    * --Polling for Get Payment Details timeout, and you are uncertain of the status
    * https://developer.paypay.ne.jp/products/docs/webpayment#cancel-payment
    *
    * @param merchantPaymentId
    * @param repo
    * @return
    */
  def paymentIsCancellable(
      merchantPaymentId: String
  )(implicit repo: PayPayRepository): Boolean = {

    val result: Boolean = (for {
      entity <- f(merchantPaymentId)
      now = Instant.now.getEpochSecond()
      t <- g(entity.requestedAt, now)
    } yield t) match {
      case Left(_)  => false
      case Right(_) => true
    }

    result
  }

  // check payment status is created or completed
  def f(
      merchantPaymentId: String
  )(implicit repo: PayPayRepository): Either[Boolean, PaymentEntity] = repo
    .findPayment(merchantPaymentId)
    .filter(e =>
      (e.status == (PaymentState.StatusEnum.CREATED)) ||
        (e.status == (PaymentState.StatusEnum.COMPLETED))
    ) match {
    case None    => Left(false)
    case Some(v) => Right(v)
  }

  /** return Right(true) if executionTime is before "cancelable time", Left(false) or not.
    *
    * cancelable time: 00:15:00 AM the day after requestedAt (in JST).
    *
    * PayPay developer's Note: The Cancel API can be used until 00:14:59 AM the day after the
    * Payment has happened. For 00:15 AM or later, please call the refund API to refund the payment.
    * https://github.com/paypay/paypayopa-sdk-java/blob/068161da68d8dc575f93a84632be711eca61f502/docs/PaymentApi.md#cancelpayment
    *
    * @param requestedAt
    * @param executionTime
    * @return
    */
  def g(requestedAt: Long, executionTime: Long): Either[Boolean, Boolean] = {
    val instant   = Instant.ofEpochSecond(requestedAt)
    val zonedDate = ZonedDateTime.ofInstant(instant, ZoneId.of("JST", ZoneId.SHORT_IDS))
    val deadLine = zonedDate
      .plusDays(1)
      .withHour(0)
      .withMinute(15)
      .truncatedTo(ChronoUnit.MINUTES)
    val deadLineLong = deadLine.toEpochSecond()

    logger.debug(s"cancel deadLine is ${deadLine}")
    if (executionTime < deadLineLong) Right(true) else Left(false)
  }
}
