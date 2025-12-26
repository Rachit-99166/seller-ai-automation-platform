package com.sellerautomation.seller_ai_automation.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.sellerautomation.seller_ai_automation.model.Product;

public interface ProductRepository extends MongoRepository<Product, String> {
    List<Product> findByStockQuantityLessThan(int stockLimit);
}
