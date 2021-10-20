package com.example.mall.coupon.dao;

import com.example.mall.coupon.entity.MemberPriceEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品会员价格
 * 
 * @author QianUsers
 * @email QianUsers@gmail.com
 * @date 2022-09-10 19:11:59
 */
@Mapper
public interface MemberPriceDao extends BaseMapper<MemberPriceEntity> {
	
}
