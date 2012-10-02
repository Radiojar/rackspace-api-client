package com.pynode.rackspace.service;

import com.pynode.rackspace.client.AccountBase;
import com.pynode.rackspace.service.impl.CloudServersServiceImpl;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Creates {@linkplain CloudServersService} instances.
 * @author Christos Fragoulides
 */
public final class CloudServersServiceFactory {

    private CloudServersServiceFactory() { }   
    
    /**
     * This is used to avoid creating new service instances when one exists for the same
     * credentials and settings.
     */
    private static final Map<Integer, CloudServersService> SERVICE_STORE = 
            new HashMap<Integer, CloudServersService>();
    
    public static CloudServersService getService(AccountBase accountBase, String username, String apiKey) {
        return getService(accountBase, username, apiKey, Collections.EMPTY_MAP);
    }
    
    public static CloudServersService getService(AccountBase accountBase, String username, String apiKey,
            Map<String, Object>  settings) {
        
        int hash = username.hashCode() + apiKey.hashCode() + settings.hashCode();
        CloudServersService service = SERVICE_STORE.get(hash);
        if (service != null) return service;
        
        // Service not yet created for the specified settings, create a new one.
        service = new CloudServersServiceImpl(accountBase, username, apiKey, settings);
        
        SERVICE_STORE.put(hash, service);
        return service;
    }
    
    public static enum ServiceSetting {
        CLIENT_GAE_COMPATIBLE,
        CLIENT_RESPONSE_CACHING,
        CLIENT_CACHE_TTL,
        STATE_MANAGER;      
    }
    
}
