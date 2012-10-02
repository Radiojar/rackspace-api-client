package com.pynode.rackspace.client;

/**
 * Enumeration defining the possible Rackspace Cloud Servers account bases.
 * Depending on the account base, a different authentication URL is used
 * by {@code RackspaceCloudClient}.
 * @author Christos Fragoulides
 */
public enum AccountBase {
    US("https://auth.api.rackspacecloud.com/v1.0"), UK("https://lon.auth.api.rackspacecloud.com/v1.0");
    private String authUrl;

    private AccountBase(String authUrl) {
        this.authUrl = authUrl;
    }

    public String getAuthUrl() {
        return authUrl;
    }
    
}
