package com.example.mall.order.dao;

import com.example.mall.order.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author QianUsers
 * @email QianUsers@gmail.com
 * @date 2022-09-10 19:21:46
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {
	
}
