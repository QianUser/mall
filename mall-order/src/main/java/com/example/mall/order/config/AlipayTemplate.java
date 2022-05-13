package com.example.mall.order.config;

import com.alipay.api.*;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.example.mall.order.vo.PayVo;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "alipay")
@Component
@Data
public class AlipayTemplate {

    public String app_id;

    public String merchant_private_key;

    public String alipay_public_key;

    public String notify_url;

    public String return_url;

    private  String sign_type;

    private  String charset;

    private String timeout = "1m";

    public String gatewayUrl;

    public  String pay(PayVo vo) throws AlipayApiException {
        AlipayClient alipayClient = new DefaultAlipayClient(gatewayUrl,
                app_id, merchant_private_key, "json",
                charset, alipay_public_key, sign_type);
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(return_url);
        alipayRequest.setNotifyUrl(notify_url);
        // 商户订单号，商户网站订单系统中唯一订单号，必填
        String out_trade_no = vo.getOut_trade_no();
        // 付款金额，必填
        String total_amount = vo.getTotal_amount();
        // 订单名称，必填
        String subject = vo.getSubject();
        // 商品描述，可空
        String body = vo.getBody();
        alipayRequest.setBizContent("{\"out_trade_no\":\""+ out_trade_no +"\","
                + "\"total_amount\":\""+ total_amount +"\","
                + "\"subject\":\""+ subject +"\","
                + "\"body\":\""+ body +"\","
                + "\"timeout_express\":\""+timeout+"\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");
        return alipayClient.pageExecute(alipayRequest).getBody();

    }
}
