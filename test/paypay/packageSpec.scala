package paypay

import org.scalatestplus.play.PlaySpec
import play.api.test.FakeHeaders
import play.api.test.FakeRequest
import controllers.routes

class packageSpec extends PlaySpec {
  "parseItem" should {
    "return None for a non-parsable string." in {
      val result = parseItem("non parsable string to ItemOne")
      result mustBe (None)
    }

    val itemOneToString = ItemOne.toString()
    s"return Some(ItemOne) for the string ${itemOneToString}" in {
      val result = parseItem(itemOneToString)
      result mustBe (Some(ItemOne))
    }

    val itemTwoToString = ItemTwo.toString()
    s"return Some(ItemTwo) for the string ${itemTwoToString}" in {
      val result = parseItem(itemTwoToString)
      result mustBe (Some(ItemTwo))
    }
  }

  "itemForm" should {
    "requires item to be non-empty." in {
      val form = itemForm.bind(Map("item" -> ""))
      form.hasErrors mustBe (true)
      form.errors("item").head.message mustBe ("error.parseItem")
    }

    "does not accept strings non-parsable to Item." in {
      val form = itemForm.bind(Map("item" -> "non-parsable to Item"))
      form.hasErrors mustBe (true)
      form.errors("item").head.message mustBe ("error.parseItem")
    }

    "binds to Item." in {
      val form = itemForm.bind(Map("item" -> "ItemOne"))
      form.get mustBe (ItemOne)
    }
  }

  "orderFrom" should {
    "binds to Order." in {
      val expectedOrder = Order(ItemOne, 10, "test")
      implicit val t = FakeRequest(routes.HomeController.index())
        .withSession(confirmOrderHash -> expectedOrder.hashCode.toString)
      val form =
        orderForm.bind(Map("item" -> "ItemOne", "price" -> "10", "merchantPaymentId" -> "test"))
      form.get mustBe expectedOrder
    }
  }

  "refundOrderFrom" should {
    "binds to RefundOrder." in {
      val expectedRefundOrder = RefundOrder("refundId", "paymentId", 100, "merchantPaymentId")
      implicit val t = FakeRequest(routes.HomeController.index())
        .withSession(confirmRefundOrderHash -> expectedRefundOrder.hashCode.toString)
      val form =
        refundOrderForm.bind(
          Map(
            "merchantRefundId"    -> "refundId",
            "paymentId"           -> "paymentId",
            "amount"              -> "100",
            "toMerchantPaymentId" -> "merchantPaymentId"
          )
        )
      form.get mustBe expectedRefundOrder
    }
  }

}
