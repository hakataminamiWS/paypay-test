package paypay

import com.typesafe.config.ConfigFactory
import jp.ne.paypay.api.PaymentApi
import jp.ne.paypay.ApiClient
import jp.ne.paypay.Configuration
import jp.ne.paypay.model.MoneyAmount
import jp.ne.paypay.model.PaymentDetails
import jp.ne.paypay.model.QRCode
import jp.ne.paypay.model.QRCodeDetails
import play.api.Logging
import scala.util.Try

object PayPayApiClient extends Logging {

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

  def qrCodeFromOrder(order: Order): Try[QRCodeDetails] = {
    val qrCode = new QRCode()
    qrCode.setMerchantPaymentId(order.merchantPaymentId)
    qrCode.setAmount(new MoneyAmount().amount(order.price).currency(MoneyAmount.CurrencyEnum.JPY))
    qrCode.setCodeType("ORDER_QR")

    val redirectUrl = config.getString("paypay.redirectUrl")
    qrCode.setRedirectUrl(redirectUrl + order.merchantPaymentId)
    qrCode.setRedirectType(QRCode.RedirectTypeEnum.WEB_LINK)

    val paymentApi = new PaymentApi(PayPayApiClient.apiClient)
    Try { paymentApi.createQRCode(qrCode) }
  }

  def fetchPaymentDetails(
      merchantPaymentId: String
  ): Try[PaymentDetails] = Try {
    val paymentApi = new PaymentApi(PayPayApiClient.apiClient)
    paymentApi.getCodesPaymentDetails(merchantPaymentId)
  }

}
