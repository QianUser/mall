package com.example.mall.seckill.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.example.common.utils.R;
import com.example.mall.seckill.feign.CouponFeignService;
import com.example.mall.seckill.feign.ProductFeignService;
import com.example.mall.seckill.service.SeckillService;
import com.example.mall.seckill.to.SeckillSkuRedisTo;
import com.example.mall.seckill.vo.SeckillSessionWithSkusVo;
import com.example.mall.seckill.vo.SkuInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private CouponFeignService couponFeignService;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private RedissonClient redissonClient;

    private final String SESSION_CACHE_PREFIX = "seckill:sessions:";

    private final String SECKILL_CHARE_PREFIX = "seckill:skus";

    private final String SKU_STOCK_SEMAPHORE = "seckill:stock:";    //+商品随机码

    @Override
    public void uploadSeckillSkuLatest3Days() {
        R lates3DaySession = couponFeignService.getLates3DaySession();
        if (lates3DaySession.getCode() == 0) {
            List<SeckillSessionWithSkusVo> sessionData = lates3DaySession.getData("data", new TypeReference<List<SeckillSessionWithSkusVo>>() {});
            saveSessionInfos(sessionData);
            saveSessionSkuInfo(sessionData);
        }
    }

    /**
     * 缓存秒杀活动信息
     */
    private void saveSessionInfos(List<SeckillSessionWithSkusVo> sessions) {
        sessions.forEach(session -> {
            // 获取当前活动的开始和结束时间的时间戳
            long startTime = session.getStartTime().getTime();
            long endTime = session.getEndTime().getTime();
            String key = SESSION_CACHE_PREFIX + startTime + "_" + endTime;
            // 判断Redis中是否有该信息，如果没有才进行添加
            Boolean hasKey = redisTemplate.hasKey(key);
            // 缓存活动信息
            if (hasKey == null || !hasKey) {
                // 获取到活动中所有商品的skuId
                List<String> skuIds = session.getRelationSkus().stream()
                        .map(item -> item.getPromotionSessionId() + "-" + item.getSkuId().toString()).collect(Collectors.toList());
                redisTemplate.opsForList().leftPushAll(key, skuIds);
            }
        });

    }

    /**
     * 缓存秒杀活动所关联的商品信息
     */
    private void saveSessionSkuInfo(List<SeckillSessionWithSkusVo> sessions) {

        sessions.forEach(session -> {
            // 准备hash操作，绑定hash
            BoundHashOperations<String, Object, Object> operations = redisTemplate.boundHashOps(SECKILL_CHARE_PREFIX);
            session.getRelationSkus().forEach(seckillSkuVo -> {
                // 生成随机码
                String token = UUID.randomUUID().toString().replace("-", "");
                String redisKey = seckillSkuVo.getPromotionSessionId().toString() + "-" + seckillSkuVo.getSkuId().toString();
                if (!operations.hasKey(redisKey)) {
                    // 缓存我们商品信息
                    SeckillSkuRedisTo redisTo = new SeckillSkuRedisTo();
                    Long skuId = seckillSkuVo.getSkuId();
                    // 先查询sku的基本信息，调用远程服务
                    R info = productFeignService.getSkuInfo(skuId);
                    if (info.getCode() == 0) {
                        SkuInfoVo skuInfo = info.getData("skuInfo",new TypeReference<SkuInfoVo>(){});
                        redisTo.setSkuInfo(skuInfo);
                    }
                    // sku的秒杀信息
                    BeanUtils.copyProperties(seckillSkuVo,redisTo);
                    // 设置当前商品的秒杀时间信息
                    redisTo.setStartTime(session.getStartTime().getTime());
                    redisTo.setEndTime(session.getEndTime().getTime());
                    // 设置商品的随机码（防止恶意攻击）
                    redisTo.setRandomCode(token);
                    // 序列化json格式存入Redis中
                    String seckillValue = JSON.toJSONString(redisTo);
                    operations.put(seckillSkuVo.getPromotionSessionId().toString() + "-" + seckillSkuVo.getSkuId().toString(),seckillValue);
                    // 如果当前这个场次的商品库存信息已经上架就不需要上架
                    // 使用库存作为分布式Redisson信号量（限流）
                    // 使用库存作为分布式信号量
                    RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + token);
                    // 商品可以秒杀的数量作为信号量
                    semaphore.trySetPermits(seckillSkuVo.getSeckillCount());
                }
            });
        });
    }


    /**
     * 获取到当前可以参加秒杀商品的信息
     */
    @Override
    public List<SeckillSkuRedisTo> getCurrentSeckillSkus() {
        // 确定当前属于哪个秒杀场次
        long currentTime = System.currentTimeMillis();
        // 从Redis中查询到所有key以seckill:sessions开头的所有数据
        Set<String> keys = redisTemplate.keys(SESSION_CACHE_PREFIX + "*");
        if (keys != null) {
            for (String key : keys) {
                String replace = key.replace(SESSION_CACHE_PREFIX, "");
                String[] s = replace.split("_");
                // 获取存入Redis商品的开始时间
                long startTime = Long.parseLong(s[0]);
                // 获取存入Redis商品的结束时间
                long endTime = Long.parseLong(s[1]);
                // 判断是否是当前秒杀场次
                if (currentTime >= startTime && currentTime <= endTime) {
                    // 获取这个秒杀场次需要的所有商品信息
                    List<String> range = redisTemplate.opsForList().range(key, -100, 100);
                    BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SECKILL_CHARE_PREFIX);
                    assert range != null;
                    List<String> listValue = hashOps.multiGet(range);
                    if (listValue != null) {
                        return listValue.stream().map(item -> JSON.parseObject(item, SeckillSkuRedisTo.class)).collect(Collectors.toList());
                    }
                    break;
                }
            }
        }
        return null;
    }

    /**
     * 根据skuId查询商品是否参加秒杀活动
     */
    @Override
    public SeckillSkuRedisTo getSkuSeckilInfo(Long skuId) {
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SECKILL_CHARE_PREFIX);
        Set<String> keys = hashOps.keys();
        if (keys != null && keys.size() > 0) {
            // 正则表达式进行匹配
            String reg = "\\d-" + skuId;
            for (String key : keys) {
                // 如果匹配上了
                if (Pattern.matches(reg, key)) {
                    // 从Redis中取出数据来
                    String redisValue = hashOps.get(key);
                    // 进行序列化
                    SeckillSkuRedisTo redisTo = JSON.parseObject(redisValue, SeckillSkuRedisTo.class);
                    // 随机码
                    long currentTime = System.currentTimeMillis();
                    Long startTime = redisTo.getStartTime();
                    Long endTime = redisTo.getEndTime();
                    // 如果当前时间大于等于秒杀活动开始时间并且要小于活动结束时间
                    if (currentTime >= startTime && currentTime <= endTime) {
                        return redisTo;
                    }
                    redisTo.setRandomCode(null);
                    return redisTo;
                }
            }
        }
        return null;
    }

}