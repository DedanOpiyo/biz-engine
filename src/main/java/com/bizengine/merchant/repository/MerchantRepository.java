package com.bizengine.merchant.repository;
import com.bizengine.merchant.model.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, Long>{
    // Automatically generates SQL to find all stores belonging to a specific Node user
    List<Merchant> findByOwnerUserId(Long ownerUserId);
}
