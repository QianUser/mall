package com.example.mall.order.dao;

import com.example.mall.order.entity.OrderItemEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单项信息
 * 
 * @author QianUsers
 * @email QianUsers@gmail.com
 * @date 2022-09-10 19:21:46
 */
@Mapper
public interface OrderItemDao extends BaseMapper<OrderItemEntity> {
	
}
