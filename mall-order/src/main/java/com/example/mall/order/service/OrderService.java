package com.example.mall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.common.utils.PageUtils;
import com.example.mall.order.entity.OrderEntity;

import java.util.Map;

/**
 * 订单
 *
 * @author QianUsers
 * @email QianUsers@gmail.com
 * @date 2022-09-10 19:21:46
 */
public interface OrderService extends IService<OrderEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

