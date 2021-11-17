package services.cancel

import org.scalatestplus.play.PlaySpec
import paypay.MemoryRepository
import paypay.PaymentEntity
import java.time.Instant
import java.time.ZonedDateTime

class CancelServiceSpec extends PlaySpec {
  implicit val repo = new MemoryRepository

  "g" should {
    val paymentHappen    = ZonedDateTime.parse("2021-01-02T01:02:03+09:00")
    val notCancelableDay = ZonedDateTime.parse("2021-01-03T00:15:00+09:00")
    val cancelableDay    = ZonedDateTime.parse("2021-01-03T00:14:59+09:00")
    "return Left(false), if execution time is after 00:15:00 AM the day after the Payment has happened." in {
      val result = CancelService.g(paymentHappen.toEpochSecond(), notCancelableDay.toEpochSecond())
      result mustBe (Left(false))
    }
    "return Right(true), if execution time is before 00:15:00 AM the day after the Payment has happened." in {
      val result = CancelService.g(paymentHappen.toEpochSecond(), cancelableDay.toEpochSecond())
      result mustBe (Right(true))
    }
  }
}
