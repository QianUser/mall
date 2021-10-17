package com.example.mall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.common.utils.PageUtils;
import com.example.mall.product.entity.UndoLogEntity;

import java.util.Map;

/**
 * 
 *
 * @author QianUsers
 * @email QianUsers@gmail.com
 * @date 2022-09-09 22:48:41
 */
public interface UndoLogService extends IService<UndoLogEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

