package com.pynode.rackspace.service.utils;

import com.pynode.rackspace.client.AccountBase;
import com.pynode.rackspace.service.StateManager;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 * @author Christos Fragoulides
 */
public final class DefaultStateManager implements StateManager {
    
    private static Map<Integer, StateManager> singletons = new ConcurrentHashMap<Integer, StateManager>();
    
    private ConcurrentMap<Object, Object> state = new ConcurrentHashMap<Object, Object>();
    
    private DefaultStateManager() {};
    
    public static StateManager getInstance(AccountBase accountBase, String userName, String apiKey) {
        
        int hash = accountBase.hashCode() + userName.hashCode() + apiKey.hashCode();
        StateManager manager = singletons.get(hash);
        if (manager == null) {
            manager = new DefaultStateManager();
            singletons.put(hash, manager);
        }
        
        return manager;
    }

    @Override
    public ConcurrentMap<Object, Object> getState() {
        return state;
    }
    
}
