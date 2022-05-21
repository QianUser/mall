package com.example.mall.seckill.service;

import com.example.mall.seckill.to.SeckillSkuRedisTo;

import java.util.List;

public interface SeckillService {

    /**
     * 上架三天需要秒杀的商品
     */
    void uploadSeckillSkuLatest3Days();

    List<SeckillSkuRedisTo> getCurrentSeckillSkus();

    SeckillSkuRedisTo getSkuSeckilInfo(Long skuId);
}
