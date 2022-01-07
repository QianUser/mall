package com.example.mall.product.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyRedissonConfig {

    /**
     * 所有对Redisson的使用都是通过RedissonClient
     */
    @Bean(destroyMethod="shutdown")
    public RedissonClient redisson() {
        // 创建配置
        Config config = new Config();
        config.useSingleServer().setAddress("redis://192.168.227.131:6379");
        // 根据Config创建出RedissonClient实例
        return Redisson.create(config);
    }

}
