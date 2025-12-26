package com.sellerautomation.seller_ai_automation.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.sellerautomation.seller_ai_automation.model.DistributionCenter;
public interface DistributionCenterRepository extends MongoRepository<DistributionCenter, String> {}
