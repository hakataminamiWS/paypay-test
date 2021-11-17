package paypay

import com.google.inject.ImplementedBy
import com.google.inject.Singleton
import jp.ne.paypay.model.PaymentDetails
import jp.ne.paypay.model.PaymentState
import jp.ne.paypay.model.QRCodeDetails
import jp.ne.paypay.model.RefundDetails
import jp.ne.paypay.model.RefundState

@ImplementedBy(classOf[MemoryRepository])
trait PayPayRepository {
  type MerchantPaymentId = String
  type MerchantRefundId  = String
  type PaymentTable      = Map[MerchantPaymentId, PaymentEntity]
  type RefundTable       = Map[MerchantRefundId, RefundEntity]

  def insertPayment(qrCodeDetails: QRCodeDetails): Unit
  def findAllPayment: Option[PaymentTable]
  def updatePaymentStatus(paymentDetails: PaymentDetails): Unit
  def findPayment(merchantPaymentId: MerchantPaymentId): Option[PaymentEntity]
  def insertRefund(refundDetails: RefundDetails): Unit
  def updateRefundStatus(refundDetails: RefundDetails): Unit
  def findAllRefund: Option[RefundTable]
}

case class PaymentEntity(
    status: PaymentState.StatusEnum,
    requestedAt: Long,
    paymentId: Option[String] = None,
    amount: Int = 0
)
case class RefundEntity(
    status: RefundState.StatusEnum,
    requestedAt: Long,
    paymentId: String,
    amount: Int
)

@Singleton
class MemoryRepository extends PayPayRepository {

  private var paymentTable: PaymentTable = Map(
    "1c83f93d-ff4b-4e2f-bc40-45e7d32c195b" -> PaymentEntity(
      PaymentState.StatusEnum.CREATED,
      1637038435L,
      Some("03761470811072651264"),
      10
    ),
    "856e81e3-84ce-4835-8c83-0f3599cd841a" -> PaymentEntity(
      PaymentState.StatusEnum.COMPLETED,
      1636960956L,
      Some("03760805271530397696"),
      20
    ),
    "103c6992-abab-42d2-9440-c1a23efadbae" -> PaymentEntity(
      PaymentState.StatusEnum.REFUNDED,
      1636691631L,
      Some("03758491787396349952"),
      20
    )
  )

  private var refundTable: RefundTable = Map(
    "e699a362-a434-47e7-aec9-aa82848f3b1a" -> RefundEntity(
      RefundState.StatusEnum.REFUNDED,
      1636943286L,
      "03758491787396349952",
      20
    )
  )

  override def insertPayment(qr: QRCodeDetails) = {
    val qeRes = qr.getData()
    paymentTable += (qeRes.getMerchantPaymentId -> PaymentEntity(
      PaymentState.StatusEnum.CREATED,
      qeRes.getRequestedAt
    ))
  }

  override def findAllPayment = {
    if (paymentTable.nonEmpty) Some(paymentTable) else None
  }

  override def updatePaymentStatus(detail: PaymentDetails) = {
    val payment = Option(detail.getData)
    val result = for {
      id     <- payment.flatMap(p => Option(p.getMerchantPaymentId))
      status <- payment.flatMap(p => Option(p.getStatus))
      req    <- payment.flatMap(p => Option(p.getRequestedAt))
      pId    <- payment.flatMap(p => Option(p.getPaymentId))
      amo    <- payment.flatMap(p => Option(p.getAmount)).flatMap(m => Option(m.getAmount))
    } yield (id -> PaymentEntity(
      status = status,
      requestedAt = req,
      paymentId = Some(pId),
      amount = amo
    ))

    result.foreach {
      case (k, v) if (!paymentTable.contains(k)) =>
        paymentTable += (k -> v)
      case (k, v) if (paymentTable.get(k).get.hashCode != v.hashCode) =>
        paymentTable += (k -> v)
      case (_, _) =>
    }
  }

  override def findPayment(id: MerchantPaymentId) = {
    paymentTable.get(id)
  }

  override def insertRefund(rd: RefundDetails) = {
    val refund = rd.getData()
    refundTable += (refund.getMerchantRefundId -> RefundEntity(
      refund.getStatus(),
      refund.getRequestedAt(),
      refund.getPaymentId(),
      refund.getAmount.getAmount()
    ))
  }

  override def updateRefundStatus(detail: RefundDetails) = {
    val refund = Option(detail.getData)
    val result = for {
      id     <- refund.flatMap(p => Option(p.getMerchantRefundId))
      status <- refund.flatMap(p => Option(p.getStatus))
      req    <- refund.flatMap(p => Option(p.getRequestedAt))
      pId    <- refund.flatMap(p => Option(p.getPaymentId))
      amo    <- refund.flatMap(p => Option(p.getAmount)).flatMap(m => Option(m.getAmount))
    } yield (
      id ->
        RefundEntity(
          status = status,
          requestedAt = req,
          paymentId = pId,
          amount = amo
        )
    )

    result.foreach {
      case (k, v) if (!refundTable.contains(k)) =>
        refundTable += (k -> v)
      case (k, v) if (refundTable.get(k).get.hashCode != v.hashCode) =>
        refundTable += (k -> v)
      case (_, _) =>
    }
  }

  override def findAllRefund = {
    if (refundTable.nonEmpty) Some(refundTable) else None
  }
}
