package com.bizengine.merchant.controller;

import com.bizengine.businessuser.model.BusinessUser;
import com.bizengine.merchant.dto.UpdateMerchantRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.bizengine.merchant.model.Merchant;
import com.bizengine.merchant.service.MerchantService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/v1/merchants")
public class MerchantController {
    private static final Logger log =
            LoggerFactory.getLogger(MerchantController.class);
    private final MerchantService merchantService;

    // @Autowired
    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

//    @GetMapping("/")
//    public String testRequest() {
//        log.info("Trial route!");
//        return "Trial route!";
//    }

    // 🔒 SECURE: Reads stores using the verified user ID from the active thread context
    @GetMapping("/my-stores")
    public List<Merchant> getStores(@AuthenticationPrincipal BusinessUser user) {
        log.info("Merchants retrieved for verified business user ID: {}", user.getId());
        return merchantService.getStoresByOwner(user.getId());
    }

    @PutMapping("/update")
    public Merchant updateStore(
            @AuthenticationPrincipal BusinessUser user,
            /* @Valid */ @RequestBody UpdateMerchantRequest request) {
        log.info("Merchant updated for business user ID: {}", user.getId());
        return merchantService.updateStoreByOwner(user.getId(), request);
    }

    @PatchMapping("/patch")
    public Merchant patchStore(
            @AuthenticationPrincipal BusinessUser user,
            @RequestBody Map<String, Object> updates) {
        log.info("Merchant records patched for business user ID: {}", user.getId());
        return merchantService.patchStoreByOwner(user.getId(), updates);
    }

    @DeleteMapping("/delete/{merchantId}")
    public ResponseEntity<Merchant> deleteStore(
            @AuthenticationPrincipal BusinessUser user,
            @PathVariable Long merchantId) {
        log.info("Merchant deletion requested by business user ID: {}", user.getId());
        return merchantService.deleteStoreByOwner(user.getId(), merchantId);
    }

    @PostMapping("/register")
    Merchant createStore(
            @AuthenticationPrincipal BusinessUser user,
            @RequestBody Merchant merchant) {
        log.info("Merchant created!");
        return merchantService.registerStore(user.getId(), merchant);
    }
}
