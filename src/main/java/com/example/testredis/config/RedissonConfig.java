package com.example.testredis.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {
    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redisson() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + redisHost + ":" + redisPort)
                .setConnectionMinimumIdleSize(5)      // Duy trì tối thiểu 5 kết nối nhàn rỗi
                .setConnectionPoolSize(20)            // Sửa lại thành SET: Tối đa 20 kết nối đồng thời
                .setConnectTimeout(5000)              // Timeout khi tạo kết nối (5s)
                .setTimeout(3000);
        return Redisson.create(config);
    }


}
