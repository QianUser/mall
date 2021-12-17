package com.example.mall.product;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.mall.product.entity.BrandEntity;
import com.example.mall.product.service.BrandService;
import com.example.mall.product.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

@Slf4j
@SpringBootTest
class MallProductApplicationTests {

    @Autowired
    BrandService brandService;

    @Autowired
    private CategoryService categoryService;

    @Test
    public void testInsert() {
        BrandEntity brandEntity = new BrandEntity();
        brandEntity.setDescript("");
        brandEntity.setName("华为");
        brandService.save(brandEntity);
        System.out.println("保存成功...");
    }

    @Test
    public void testUpdate() {
        BrandEntity brandEntity = new BrandEntity();
        brandEntity.setBrandId(1L);
        brandEntity.setDescript("华为");
        brandService.updateById(brandEntity);
    }

    @Test
    public void testSelect() {
        List<BrandEntity> list = brandService.list(new QueryWrapper<BrandEntity>().eq("brand_id", 1L));
        list.forEach(System.out::println);
    }

    @Test
    public void testFindPath() {
        Long[] catalogPath = categoryService.findcatalogPath(225l);

        log.info("完整路径catalogPath={}", Arrays.asList(catalogPath));
    }

}
