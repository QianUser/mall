package com.example.mall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.common.to.OrderTo;
import com.example.common.to.mq.StockLockedTo;
import com.example.common.utils.PageUtils;
import com.example.mall.ware.entity.WareSkuEntity;
import com.example.mall.ware.vo.SkuHasStockVo;
import com.example.mall.ware.vo.WareSkuLockVo;

import java.util.List;
import java.util.Map;

/**
 * εεεΊε­
 *
 * @author QianUsers
 * @email QianUsers@gmail.com
 * @date 2022-09-10 19:25:44
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void addStock(Long skuId, Long wareId, Integer skuNum);

    List<SkuHasStockVo> getSkuHasStock(List<Long> skuIds);

    boolean orderLockStock(WareSkuLockVo vo);

    void unlockStock(StockLockedTo to);

    void unlockStock(OrderTo to);
}

