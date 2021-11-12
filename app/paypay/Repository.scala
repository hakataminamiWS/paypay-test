package paypay

import com.google.inject.ImplementedBy
import com.google.inject.Singleton
import jp.ne.paypay.model.PaymentDetails
import jp.ne.paypay.model.PaymentState.StatusEnum
import jp.ne.paypay.model.QRCodeDetails

@ImplementedBy(classOf[MemoryRepository])
trait PayPayRepository {
  type DBCol = Map[String, Entity]
  def insertPayment(qrCodeDetails: QRCodeDetails): Unit
  def findAllPayment: DBCol
  def updatePaymentStatus(paymentDetails: PaymentDetails): Unit
  // def

}

case class Entity(status: StatusEnum, requestedAt: Long)

@Singleton
class MemoryRepository extends PayPayRepository {

  private var paymentTable: DBCol = Map(
    // "f758f027-e0b3-41ac-9e5f-7971f647cd22" -> Entity(StatusEnum.CREATED, 1636605366L),
    // "0d9fb175-d705-4976-8297-6373e291b62c" -> Entity(StatusEnum.CREATED, 1636605371L),
    // "1e9177d2-3dc2-402c-8d11-a0902ad8d60f" -> Entity(StatusEnum.COMPLETED, 1636613901L)
  )

  override def insertPayment(qr: QRCodeDetails) = {
    val qeRes = qr.getData()
    paymentTable += (qeRes.getMerchantPaymentId -> Entity(
      StatusEnum.CREATED,
      qeRes.getRequestedAt
    ))
  }

  override def findAllPayment = {
    paymentTable
  }

  override def updatePaymentStatus(detail: PaymentDetails) = {
    val payment           = Option(detail.getData)
    val merchantPaymentId = payment.map(_.getMerchantPaymentId)
    val payStatus         = payment.map(_.getStatus)
    val requestedAt       = payment.map(_.getRequestedAt)

    for {
      id     <- merchantPaymentId
      entity <- paymentTable.get(id)
      status <- payStatus
      req    <- requestedAt
      if status != entity.status
    } {
      paymentTable += (id -> Entity(status, req))
    }
  }
}
