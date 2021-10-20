package com.example.mall.order.dao;

import com.example.mall.order.entity.OrderSettingEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单配置信息
 * 
 * @author QianUsers
 * @email QianUsers@gmail.com
 * @date 2022-09-10 19:21:45
 */
@Mapper
public interface OrderSettingDao extends BaseMapper<OrderSettingEntity> {
	
}
