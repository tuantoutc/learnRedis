package com.example.testredis.repository;

import com.example.testredis.domain.entity.ProductEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    @Query("SELECT p.id FROM ProductEntity p WHERE p.id > :lastId ORDER BY p.id ASC")
    List<Long> findNextIds(@Param("lastId") Long lastId, Pageable pageable);
}
