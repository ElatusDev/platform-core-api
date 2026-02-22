/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a CN-bound one-time enrollment token for service certificate bootstrapping.
 *
 * <p>Each token is cryptographically random (256 bits), valid for a single use,
 * and bound to exactly one Common Name. Once a service enrolls, the token is
 * permanently invalidated.
 */
public class BootstrapToken {

    @JsonProperty("token")
    private String token;

    @JsonProperty("boundCN")
    private String boundCN;

    @JsonProperty("issued")
    private String issued;

    @JsonProperty("used")
    private boolean used;

    public BootstrapToken() {
    }

    public BootstrapToken(String token, String boundCN, String issued, boolean used) {
        this.token = token;
        this.boundCN = boundCN;
        this.issued = issued;
        this.used = used;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getBoundCN() {
        return boundCN;
    }

    public void setBoundCN(String boundCN) {
        this.boundCN = boundCN;
    }

    public String getIssued() {
        return issued;
    }

    public void setIssued(String issued) {
        this.issued = issued;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }
}
