package com.example.mall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.common.utils.PageUtils;
import com.example.mall.member.entity.MemberEntity;

import java.util.Map;

/**
 * 会员
 *
 * @author QianUsers
 * @email QianUsers@gmail.com
 * @date 2022-09-10 19:16:02
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

