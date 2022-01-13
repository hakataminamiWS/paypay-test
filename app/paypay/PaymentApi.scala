package paypay

import com.typesafe.config.ConfigFactory
import jp.ne.paypay.api.PaymentApi
import jp.ne.paypay.ApiClient
import jp.ne.paypay.Configuration
import jp.ne.paypay.model.MoneyAmount
import jp.ne.paypay.model.NotDataResponse
import jp.ne.paypay.model.PaymentDetails
import jp.ne.paypay.model.QRCode
import jp.ne.paypay.model.QRCodeDetails
import jp.ne.paypay.model.Refund
import jp.ne.paypay.model.RefundDetails
import scala.util.Try

object PayPayApiClient {

  val apiClient: ApiClient = new Configuration().getDefaultApiClient

  private val config = ConfigFactory.load()

  private val productionMode = config.getBoolean("paypay.productionMode")
  apiClient.setProductionMode(productionMode)

  private val apiKey = config.getString("paypay.secret.apiKey")
  apiClient.setApiKey(apiKey)

  private val apiSecretKey = config.getString("paypay.secret.apiSecretKey")
  apiClient.setApiSecretKey(apiSecretKey)

  private val apiAssumeMerchant = config.getString("paypay.secret.apiAssumeMerchant")
  apiClient.setAssumeMerchant(apiAssumeMerchant)

  def qrCodeFromOrder(order: Order)(agent: String): Try[QRCodeDetails] = {
    val qrCode = new QRCode()
    qrCode.setMerchantPaymentId(order.merchantPaymentId)
    qrCode.setAmount(new MoneyAmount().amount(order.price).currency(MoneyAmount.CurrencyEnum.JPY))
    qrCode.setCodeType("ORDER_QR")

    val redirectToThx = config.getString("paypay.thanksPage")
    qrCode.setRedirectUrl(redirectToThx)
    qrCode.setRedirectType(QRCode.RedirectTypeEnum.WEB_LINK)
    qrCode.setUserAgent(agent)
    val paymentApi = new PaymentApi(apiClient)
    Try(paymentApi.createQRCode(qrCode))
  }

  def fetchPaymentDetails(
      merchantPaymentId: String
  ): Try[PaymentDetails] = {
    val paymentApi = new PaymentApi(apiClient)
    Try(paymentApi.getCodesPaymentDetails(merchantPaymentId))
  }

  def refundForOrder(order: RefundOrder): Try[RefundDetails] = {
    val refund = new Refund
    refund.setAmount(new MoneyAmount().amount(order.amount).currency(MoneyAmount.CurrencyEnum.JPY))
    refund.setMerchantRefundId(order.merchantRefundId)
    refund.setPaymentId(order.paymentId)

    val paymentApi = new PaymentApi(apiClient)
    Try(paymentApi.refundPayment(refund))
  }

  def cancelForOrder(order: CancelOrder): Try[NotDataResponse] = {
    val paymentApi = new PaymentApi(apiClient)
    Try(paymentApi.cancelPayment(order.merchantPaymentId))
  }

  def fetchRefundDetails(
      merchantRefundId: String
  ): Try[RefundDetails] = {
    val paymentApi = new PaymentApi(PayPayApiClient.apiClient)
    Try(paymentApi.getRefundDetails(merchantRefundId))
  }

}
