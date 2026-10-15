package com.bizengine.businessuser.dto;

public class EnlistPartnerRequest {
    private String issuer;
    private String jwkSetUri;
    private String partnerName;

    // Getters and Setters
    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }

    public String getJwkSetUri() { return jwkSetUri; }
    public void setJwkSetUri(String jwkSetUri) { this.jwkSetUri = jwkSetUri; }

    public String getPartnerName() { return partnerName; }
    public void setPartnerName(String partnerName) { this.partnerName = partnerName; }
}

// Instead of listing
//# External Partner configuration (Add as many external partners as needed). # Now enlisted in externalissuer db
//external.provider.issuer=https://external-auth-service.com
//external.provider.jwk-set-uri=https://external-auth-service.com
//
//in application.properties,
//this allows admins to 'whitelist / enlist' external API's