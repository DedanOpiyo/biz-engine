package com.bizengine.merchant.dto;

public class UpdateMerchantRequest {

//    @NotBlank
    private String storeName;
//
//    @NotBlank
    private String category;

//    @NotBlank
    private String status;

    // getters/setters
    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}


//
//merchant/
//        ├── controller/             # HTTP/API layer
//│   └── MerchantController.java
//│
//        ├── dto/                    # API request/response shapes
//│   ├── CreateMerchantRequest.java
//│   ├── UpdateMerchantRequest.java
//│   ├── PatchMerchantRequest.java
//│   └── MerchantResponse.java
//│
//        ├── model/                  # Domain/JPA entities
//│   └── Merchant.java
//│
//        ├── repository/             # Database access
//│   └── MerchantRepository.java
//│
//        └── service/                # Business logic
//    ├── MerchantService.java
//    └── MerchantServiceImpl.java
//
