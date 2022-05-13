package com.example.mall.order.web;

import com.alipay.api.AlipayApiException;
import com.example.mall.order.config.AlipayTemplate;
import com.example.mall.order.service.OrderService;
import com.example.mall.order.vo.PayVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

@Slf4j
@Controller
public class PayWebController {

    @Autowired
    private AlipayTemplate alipayTemplate;

    @Autowired
    private OrderService orderService;

    /**
     * 用户下单：支付宝支付，让支付页让浏览器展示；支付成功以后，跳转到用户的订单列表页
     */
    @ResponseBody
    @GetMapping(value = "/aliPayOrder", produces = "text/html")
    public String aliPayOrder(@RequestParam("orderSn") String orderSn) throws AlipayApiException {
        PayVo payVo = orderService.getOrderPay(orderSn);
        return alipayTemplate.pay(payVo);
    }

}
