package com.bizengine.businessuser.dto;

public class CreateBusinessUserRequest {
    private String externalUserId;
    private String email;
    private String password;
    private String companyName;

    // Getters and Setters
    public String getExternalUserId() { return externalUserId; }
    public void setExternalUserId(String externalUserId) { this.externalUserId = externalUserId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
}
