package com.example.testredis.common;

import com.example.testredis.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;


@Component
@RequiredArgsConstructor
@Slf4j
public class BloomFilterInitializer implements ApplicationRunner {
    private final RBloomFilter<Long> productBloomFilter;
    private final ProductRepository productRepository;

    public static final int BATCH_SIZE = 5_000;

    @Override
    public void run(ApplicationArguments args) {
        // chi nap khi bloom filter chua co du lieu ( uoc luong so luong phan tu hien tai)
        if (productBloomFilter.count() > 0) {
            log.info("BloomFilter da chua du lieu, bo qua buoc nap ban dau. ");
            return;
        }
        log.info("Bat dau nap du lieu cho Bloom filter theo batch");

        Long lastId = 0L;
        long totalLoaded = 0;

        while (true) {
            // Chỉ cần cố định lấy batchSize phần tử đầu tiên thỏa điều kiện id > lastId
            Pageable pageable = PageRequest.of(0, BATCH_SIZE);
            List<Long> idBatch = productRepository.findNextIds(lastId, pageable);

            if (idBatch.isEmpty()) {
                break;
            }

            // dung Rbatch de gui pipeline len den redis
            List<CompletableFuture<Boolean>> futures = new ArrayList<>(idBatch.size());
            for (Long id : idBatch) {
                futures.add(productBloomFilter.addAsync(id).toCompletableFuture());
            }

            // 2. Cho toan bo batch hoan thanh truoc khi doc tiep DB
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            lastId = idBatch.getLast();
            totalLoaded += idBatch.size();
        }
        log.info("Da nap thanh cong {} IDs vao Bloom Filter.", totalLoaded);
    }
}
