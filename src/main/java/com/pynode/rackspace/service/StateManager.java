package com.pynode.rackspace.service;

import java.util.concurrent.ConcurrentMap;

/**
 *
 * @author Christos Fragoulides
 */
public interface StateManager {
    
    ConcurrentMap<Object, Object> getState();
    
}
