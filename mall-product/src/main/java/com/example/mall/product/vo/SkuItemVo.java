package com.example.mall.product.vo;

import com.example.mall.product.entity.SkuImagesEntity;
import com.example.mall.product.entity.SkuInfoEntity;
import com.example.mall.product.entity.SpuInfoDescEntity;
import lombok.Data;
import lombok.ToString;

import java.util.List;


@ToString
@Data
public class SkuItemVo {

    // sku基本信息的获取
    private SkuInfoEntity info;

    private boolean hasStock = true;

    // sku的图片信息
    private List<SkuImagesEntity> images;

    // spu的销售属性组合
    private List<SkuItemSaleAttrVo> saleAttr;

    // spu的介绍
    private SpuInfoDescEntity desc;

    // spu的规格参数信息
    private List<SpuItemAttrGroupVo> groupAttrs;

    // 秒杀商品的优惠信息
    private SeckillSkuVo seckillSkuVo;

}
