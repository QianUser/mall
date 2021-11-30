package com.example.mall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.mall.product.dao.BrandDao;
import com.example.mall.product.dao.CategoryDao;
import com.example.mall.product.entity.BrandEntity;
import com.example.mall.product.entity.CategoryEntity;
import com.example.mall.product.service.BrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.utils.PageUtils;
import com.example.common.utils.Query;

import com.example.mall.product.dao.CategoryBrandRelationDao;
import com.example.mall.product.entity.CategoryBrandRelationEntity;
import com.example.mall.product.service.CategoryBrandRelationService;

import javax.annotation.Resource;


@Service("categoryBrandRelationService")
public class CategoryBrandRelationServiceImpl extends ServiceImpl<CategoryBrandRelationDao, CategoryBrandRelationEntity> implements CategoryBrandRelationService {

    @Autowired
    private BrandDao brandDao;

    @Autowired
    private CategoryDao categoryDao;

    @Autowired
    private CategoryBrandRelationDao relationDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryBrandRelationEntity> page = this.page(
                new Query<CategoryBrandRelationEntity>().getPage(params),
                new QueryWrapper<CategoryBrandRelationEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveDetail(CategoryBrandRelationEntity categoryBrandRelation) {
        Long brandId = categoryBrandRelation.getBrandId();
        Long catalogId = categoryBrandRelation.getcatalogId();

        // 查询品牌详细信息
        BrandEntity brandEntity = brandDao.selectById(brandId);
        // 查询分类详细信息
        CategoryEntity categoryEntity = categoryDao.selectById(catalogId);

        // 将信息保存到categoryBrandRelation中
        categoryBrandRelation.setBrandName(brandEntity.getName());
        categoryBrandRelation.setcatalogName(categoryEntity.getName());

        // 保存到数据库中
        this.baseMapper.insert(categoryBrandRelation);
    }

    @Override
    public void updateBrand(Long brandId, String name) {
        CategoryBrandRelationEntity relationEntity = new CategoryBrandRelationEntity();
        relationEntity.setBrandId(brandId);
        relationEntity.setBrandName(name);
        this.update(relationEntity,new UpdateWrapper<CategoryBrandRelationEntity>().eq("brand_id", brandId));
    }

    @Override
    public void updateCategory(Long catId, String name) {
        this.baseMapper.updateCategory(catId, name);
    }

    @Override
    public List<BrandEntity> getBrandsByCatId(Long catId) {

        List<CategoryBrandRelationEntity> catalogId = relationDao.selectList(new QueryWrapper<CategoryBrandRelationEntity>().eq("catalog_id", catId));

        return catalogId.stream().map(item -> {
            Long brandId = item.getBrandId();
            // 查询品牌的详情
            return brandDao.selectById(brandId);
        }).collect(Collectors.toList());
    }

}