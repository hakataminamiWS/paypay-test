package services.refund

import jp.ne.paypay.model.PaymentState
import paypay.PayPayRepository
import paypay.RefundOrder

object RefundService {
  def paymentIsRefundable(
      merchantPaymentId: String
  )(implicit repo: PayPayRepository): Boolean = {
    paymentIsCompleted(merchantPaymentId)
    // TODO add refund check if needed
  }
  // check status of payment is COMPLETED
  // https://paypay.ne.jp/developers-faq/refund/apidelete-a-codecancel-a-payment-revert-a-payment-authorizationrefund-a-payment/
  def paymentIsCompleted(
      merchantPaymentId: String
  )(implicit repo: PayPayRepository): Boolean = {
    repo
      .findPayment(merchantPaymentId)
      .filter(_.status == (PaymentState.StatusEnum.COMPLETED))
      .map(_ => true)
      .getOrElse(false)
  }

  /** return refundOrder with the calculated refund amount
    *
    * @param refundOrder
    * @return
    */
  def calculateRefundAmount(
      refundOrder: Option[RefundOrder]
  ): Option[RefundOrder] = {
    refundOrder
      .map(order => {
        order.copy(amount = refundAmount(order.toMerchantPaymentId))
      })
  }
  def refundAmount(merchantPaymentId: String): Int =
    // TODO calculate refund amount
    10
}
