package services.item

import paypay.Item
import paypay.ItemOne
import paypay.ItemTwo

object ItemService {
  def priceForItem(item: Item): Int = item match {
    case ItemOne => 10
    case ItemTwo => 20
  }
}
