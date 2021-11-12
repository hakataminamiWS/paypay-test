package paypay

import org.scalatestplus.play.PlaySpec

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
      val form =
        orderForm.bind(Map("item" -> "ItemOne", "price" -> "10", "merchantPaymentId" -> "test"))
      form.get mustBe (Order(ItemOne, 10, "test"))
    }

  }

  "test" should {
    val order1 = Order(ItemOne, 10)
    val order2 = Order(ItemOne, 10)
    val order3 = Order(ItemOne, 10)

    println(order1.merchantPaymentId)
    println(order2.merchantPaymentId)
    println(order3.merchantPaymentId)
  }
}
