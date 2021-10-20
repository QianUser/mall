package com.example.mall.ware.dao;

import com.example.mall.ware.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品库存
 * 
 * @author QianUsers
 * @email QianUsers@gmail.com
 * @date 2022-09-10 19:25:44
 */
@Mapper
public interface WareSkuDao extends BaseMapper<WareSkuEntity> {
	
}
