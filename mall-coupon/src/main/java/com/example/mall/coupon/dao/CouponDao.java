package com.example.mall.coupon.dao;

import com.example.mall.coupon.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author QianUsers
 * @email QianUsers@gmail.com
 * @date 2022-09-10 19:11:59
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
