package com.example.testredis.config;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BloomFilterConfig {
    public static final String PRODUCT_BLOOM_FILTER = "product-bloom-filter";

    @Bean
    public RBloomFilter<Long> productBloomFilter(RedissonClient redissonClient) {
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(PRODUCT_BLOOM_FILTER);
        // Khoi tao neu chua ton tai
        bloomFilter.tryInit(1_000_000L, 0.01);
        return bloomFilter;
    }


}
