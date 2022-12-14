package com.example.mall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.utils.PageUtils;
import com.example.common.utils.Query;
import com.example.mall.product.dao.CategoryDao;
import com.example.mall.product.entity.CategoryEntity;
import com.example.mall.product.service.CategoryBrandRelationService;
import com.example.mall.product.service.CategoryService;
import com.example.mall.product.vo.Catalog2Vo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    private CategoryBrandRelationService categoryBrandRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<>()
        );
        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        // 查询出所有分类
        List<CategoryEntity> entities = super.baseMapper.selectList(null);
        // 组装成父子的树形结构
        return entities.stream()
                .filter(e -> e.getParentCid() == 0)
                .peek((menu) -> menu.setChildren(getChildren(menu, entities)))
                .sorted(Comparator.comparingInt(menu -> (menu.getSort() == null ? 0 : menu.getSort())))
                .collect(Collectors.toList());
    }

    @Override
    public void removeMenuByIds(List<Long> asList) {
        // TODO 检查当前删除的菜单，是否被别的地方引用
        baseMapper.deleteBatchIds(asList);

    }

    @Override
    public Long[] findCatalogPath(Long catalogId) {
        List<Long> paths = new ArrayList<>();
        // 递归查询是否还有父节点
        List<Long> parentPath = findParentPath(catalogId, paths);
        // 进行一个逆序排列
        Collections.reverse(parentPath);
        return parentPath.toArray(new Long[0]);
    }

    /**
     * 级联更新所有关联的数据
     */
    @CacheEvict(value = "category", allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
    }

    // 递归查找所有菜单的子菜单
    private List<CategoryEntity> getChildren(CategoryEntity root, List<CategoryEntity> all) {
        return all.stream().filter(categoryEntity -> categoryEntity.getParentCid().equals(root.getCatId())).peek(categoryEntity -> {
            categoryEntity.setChildren(getChildren(categoryEntity, all));
        }).sorted(Comparator.comparingInt(menu -> (menu.getSort() == null ? 0 : menu.getSort()))).collect(Collectors.toList());

    }

    private List<Long> findParentPath(Long catalogId, List<Long> paths) {
        // 收集当前节点id
        paths.add(catalogId);
        // 根据当前分类id查询信息
        CategoryEntity byId = this.getById(catalogId);
        // 如果当前不是父分类
        if (byId.getParentCid() != 0) {
            findParentPath(byId.getParentCid(), paths);
        }
        return paths;
    }

    @Cacheable(value = {"category"}, key = "#root.method.name", sync = true)
    @Override
    public List<CategoryEntity> getLevel1Categories() {
        return this.baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
    }

    @Cacheable(value = "category",key = "#root.method.name")
    @Override
    public Map<String, List<Catalog2Vo>> getCatalogJson() {
        // 将数据库的多次查询变为一次
        List<CategoryEntity> selectList = this.baseMapper.selectList(null);

        // 查出所有分类
        // 查出所有一级分类
        List<CategoryEntity> level1Categories = getParent_cid(selectList, 0L);

        // 封装数据

        return level1Categories.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            // 每一个的一级分类，查到这个一级分类的二级分类
            List<CategoryEntity> categoryEntities = getParent_cid(selectList, v.getCatId());

            // 封装上面的结果
            List<Catalog2Vo> catalog2Vos = new ArrayList<>();
            if (categoryEntities != null) {
                catalog2Vos = categoryEntities.stream().map(l2 -> {
                    Catalog2Vo catalog2Vo = new Catalog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());

                    // 找当前二级分类的三级分类封装成VO
                    List<CategoryEntity> level3Catalog = getParent_cid(selectList, l2.getCatId());

                    if (level3Catalog != null) {
                        List<Catalog2Vo.Category3Vo> category3Vos = level3Catalog.stream().map(l3 -> {
                            // 封装成指定格式
                            return new Catalog2Vo.Category3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                        }).collect(Collectors.toList());
                        catalog2Vo.setCatalog3List(category3Vos);
                    }

                    return catalog2Vo;
                }).collect(Collectors.toList());
            }
            return catalog2Vos;
        }));
    }

    private List<CategoryEntity> getParent_cid(List<CategoryEntity> selectList, Long parentCid) {
        return selectList.stream().filter(item -> item.getParentCid().equals(parentCid)).collect(Collectors.toList());
    }


}