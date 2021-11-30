package com.example.mall.coupon.service.impl;

import com.example.common.to.MemberPrice;
import com.example.common.to.SkuReductionTo;
import com.example.mall.coupon.entity.MemberPriceEntity;
import com.example.mall.coupon.entity.SkuLadderEntity;
import com.example.mall.coupon.service.MemberPriceService;
import com.example.mall.coupon.service.SkuLadderService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.utils.PageUtils;
import com.example.common.utils.Query;

import com.example.mall.coupon.dao.SkuFullReductionDao;
import com.example.mall.coupon.entity.SkuFullReductionEntity;
import com.example.mall.coupon.service.SkuFullReductionService;


@Service("skuFullReductionService")
public class SkuFullReductionServiceImpl extends ServiceImpl<SkuFullReductionDao, SkuFullReductionEntity> implements SkuFullReductionService {

    @Autowired
    private SkuLadderService skuLadderService;

    @Autowired
    private MemberPriceService memberPriceService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuFullReductionEntity> page = this.page(
                new Query<SkuFullReductionEntity>().getPage(params),
                new QueryWrapper<SkuFullReductionEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveSkuReduction(SkuReductionTo skuReductionTo) {

        // 保存满减打折、会员价
        // sku的优惠、满减等信息：mall_sms数据库的sms_sku_ladder、sms_sku_full_reduction、sms_member_price表
        SkuLadderEntity skuLadderEntity = new SkuLadderEntity();
        BeanUtils.copyProperties(skuReductionTo, skuLadderEntity);
        skuLadderEntity.setAddOther(skuReductionTo.getCountStatus());

        if (skuReductionTo.getFullCount() > 0) {
            skuLadderService.save(skuLadderEntity);
        }

        // sms_sku_full_reduction表
        SkuFullReductionEntity skuFullReductionEntity = new SkuFullReductionEntity();
        BeanUtils.copyProperties(skuReductionTo,skuFullReductionEntity);
        if (skuFullReductionEntity.getFullPrice().compareTo(BigDecimal.ZERO) > 0) {
            this.save(skuFullReductionEntity);
        }


        // sms_member_price表
        List<MemberPrice> memberPrice = skuReductionTo.getMemberPrice();

        List<MemberPriceEntity> collect = memberPrice.stream().map(mem -> {
            MemberPriceEntity memberPriceEntity = new MemberPriceEntity();
            memberPriceEntity.setSkuId(skuReductionTo.getSkuId());
            memberPriceEntity.setMemberLevelId(mem.getId());
            memberPriceEntity.setMemberLevelName(mem.getName());
            memberPriceEntity.setMemberPrice(mem.getPrice());
            memberPriceEntity.setAddOther(1);
            return memberPriceEntity;
        }).filter(item -> item.getMemberPrice().compareTo(BigDecimal.ZERO) > 0).collect(Collectors.toList());

        memberPriceService.saveBatch(collect);
    }

}