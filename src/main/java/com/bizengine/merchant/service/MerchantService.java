package com.bizengine.merchant.service;

import com.bizengine.merchant.dto.UpdateMerchantRequest;
import com.bizengine.merchant.model.Merchant;
import com.bizengine.merchant.repository.MerchantRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class MerchantService {
    private final Set<String> PATCHABLE_FIELDS = Set.of("storeName", "category");

    private final MerchantRepository merchantRepository;

    public MerchantService(MerchantRepository merchantRepository) {
        this.merchantRepository = merchantRepository;
    }

    public Merchant registerStore(Long businessUserId, Merchant merchant) {
        return merchantRepository.save(merchant);
    }

    public Merchant updateStoreByOwner(Long ownerUserId, UpdateMerchantRequest request) {
        Merchant merchant = merchantRepository.findById(ownerUserId)
                .orElseThrow(() -> new RuntimeException("Merchant not found"));

        merchant.setStoreName(request.getStoreName());
        merchant.setCategory(request.getCategory());
        merchant.setStatus(request.getStatus());

        merchantRepository.save(merchant);

        return merchant;
    }

    public Merchant patchStoreByOwner(Long ownerUserId, Map<String, Object> data) {
        Merchant merchant = merchantRepository.findById(ownerUserId)
                .orElseThrow(() -> new RuntimeException("Merchant not found"));

        data.forEach((key, value) -> {
            if (!PATCHABLE_FIELDS.contains(key)) {
                throw new IllegalArgumentException(
                        "Field cannot be updated" + key
                );
            };

            switch (key) {
                case "category" ->
                        merchant.setCategory((String) value);
                case "storeName" ->
                        merchant.setStoreName((String) value);
            }
        });

        merchantRepository.save(merchant);

        return merchant;
    }

    public ResponseEntity<Merchant> deleteStoreByOwner(Long ownerUserId, Long merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new RuntimeException("Merchant not found"));

        if (!merchant.getOwnerUserId().equals(ownerUserId)) {
            throw new AccessDeniedException("You are not the owner of this merchant");
        }

        merchantRepository.delete(merchant);

        return ResponseEntity.ok(merchant);
    }

    public List<Merchant> getStoresByOwner(Long ownerUserId) {
        return merchantRepository.findByOwnerUserId(ownerUserId);
    }
}
