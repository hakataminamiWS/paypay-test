# paypay-test
[the Web Payment](https://developer.paypay.ne.jp/products/docs/webpayment#demo-heading) test for paypay by PlayFramework 2.8.x and [paypayopa-sdk-java 1.0.5](https://github.com/paypay/paypayopa-sdk-java/tree/1.0.5)

# Function
* [決済(Create a QR Code)](https://developer.paypay.ne.jp/products/docs/webpayment#dynamic-qr-codeid)
* [Get Payment Details](https://developer.paypay.ne.jp/products/docs/webpayment#fetch-qr-code)
* [キャンセル(Cancel a payment)](https://developer.paypay.ne.jp/products/docs/webpayment#cancel-payment)
* [返品(Refund payment)](https://developer.paypay.ne.jp/products/docs/webpayment#refund-payment)
* [Fetch refund status and details](https://developer.paypay.ne.jp/products/docs/webpayment#fetch-refund-payment)
* [突合ファイル通知Webhook](https://developer.paypay.ne.jp/products/docs/webpayment#recon-file) -> [paypay-webhook-test](https://github.com/hakataminamiWS/paypay-webhook-test/tree/ae95d81b808e4ebd866ce0beb126f5db3808551c)
* [決済トランザクション通知Webhook](https://www.paypay.ne.jp/opa/doc/jp/v1.0/webcashier#tag/%E3%83%88%E3%83%A9%E3%83%B3%E3%82%B6%E3%82%AF%E3%82%B7%E3%83%A7%E3%83%B3%E3%82%A4%E3%83%99%E3%83%B3%E3%83%88) -> [paypay-webhook-test](https://github.com/hakataminamiWS/paypay-webhook-test/tree/ae95d81b808e4ebd866ce0beb126f5db3808551c)
* [Polling of Get Payment/Refund Details](https://developer.paypay.ne.jp/products/docs/webpayment#fetch-qr-code) -> [polling-test](https://github.com/hakataminamiWS/polling-test/tree/0c50138a3a84c4d314963f691e9b0c382c157450)

# Not yet
* [Delete a QR Code](https://developer.paypay.ne.jp/products/docs/webpayment#delete-qr-codeid)
* [Capture a payment authorization](https://developer.paypay.ne.jp/products/docs/webpayment#capture-payment)
* [Revert a payment authorization](https://developer.paypay.ne.jp/products/docs/webpayment#revert-payment)
* ~~[カスタマーイベント通知Webhook](https://www.paypay.ne.jp/opa/doc/jp/v1.0/account_link.html?_ga=2.173823210.835974427.1637208233-1402170323.1637208233#tag/%E3%82%AB%E3%82%B9%E3%82%BF%E3%83%9E%E3%83%BC%E3%82%A4%E3%83%99%E3%83%B3%E3%83%88)~~ (Web payment では必要ないっぽい？)
