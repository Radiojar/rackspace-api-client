package com.pynode.rackspace.service.utils;

import com.pynode.rackspace.service.StateManagementException;
import com.pynode.rackspace.service.StateManager;
import java.util.AbstractMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link StateManager} implementation using GAE's datastore and memcache services for state management,
 * providing a means for distributed state management. Datastore is the primary state store
 * and memcache is used for faster reads.<br />
 * It is implemented in such a way that in case of memcache
 * failures it will continue to serve the state as long as the datastore is available. In case of
 * datastore unavailability, the implementation's methods will throw a {@link StateManagementException}.
 * @author Christos Fragoulides
 */
public class GAEStateManager extends AbstractMap<Object, Object> implements StateManager,
        ConcurrentMap<Object, Object> {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(GAEStateManager.class);
    
    private static final String NAMESPACE = GAEStateManager.class.getName();
    private static final String NULL_NOT_SUPPORTED = "This map does not support null values, nor keys.";
    
    private ConcurrentMap<Object, Object> memcache;
    private ConcurrentMap<Object, Object> datastore;

    public GAEStateManager() {
        // Bind to a namespace specific to this class to avoid data collision.
        memcache = new MemcacheConcurrentMap<Object, Object>(NAMESPACE);
        datastore = new DatastoreConcurrentMap<Object, Object>(NAMESPACE);
    }

    @Override
    public ConcurrentMap<Object, Object> getState() {
        return this;
    }

    @Override
    public Set<Entry<Object, Object>> entrySet() {
        throw new UnsupportedOperationException("Only ConcurrentMap methods supported, in addition to get()");
    }
    
    
    /* ----------  Create Operation --------- */
    @Override
    public Object putIfAbsent(Object key, Object value) {
        
        if (key == null || value == null) throw new IllegalArgumentException(NULL_NOT_SUPPORTED);
        
        Object result;
        // Try to put in datastore.
        try {
            result = datastore.putIfAbsent(key, value);
        } catch (RuntimeException e) {
            throw new StateManagementException("putIfAbsent() failed due to a datastore error.", e);
        }
        
        // Try (once) to put in memcache, or update if value is out of date.
        try {
            Object memcached = memcache.putIfAbsent(key, value);
            if (result == null && memcached != null) {
                memcache.remove(key, memcached);
            } else if (result != null && !result.equals(memcached)) {
                if (memcached == null) {
                    memcache.putIfAbsent(key, result);
                } else {
                    memcache.replace(key, memcached, result);
                }
            }
        } catch (RuntimeException e) {
            // Report Memcache error.
            LOGGER.warn("Failed to update memcache during putIfAbsent().", e);
        }
        
        return result;
    }
    
    /* ----------   Read Operation  --------- */
    @Override
    public Object get(Object key) {
        
        if (key == null) throw new IllegalArgumentException(NULL_NOT_SUPPORTED);
        
        // Try to get from memcache.
        Object result;
        try {
            result = memcache.get(key);
        } catch (RuntimeException e) {
            // On failure log and retrieve form datastore.
            LOGGER.warn("Failed to retrive value from memcache in get()", e);
            result = null;
        }
        
        if (result == null) {
            // Value absent, get from datastore to ensure its absense.
            try {
                result = datastore.get(key);
                // Try (once) to udpate memcache if value is out of date
                if (result != null) {
                    try {
                        memcache.putIfAbsent(key, result);
                    } catch (RuntimeException e) {
                        LOGGER.warn("Failed to update memcache during get().", e);
                    }
                }
            } catch (RuntimeException dse) {
                throw new StateManagementException("get() failed due to a datastore error.", dse);
            }
        }
        
        return result;
    }

    /* ---------- Update Operations --------- */
    @Override
    public boolean replace(Object key, Object oldValue, Object newValue) {
        
        if (key == null || oldValue == null || newValue == null) 
            throw new IllegalArgumentException(NULL_NOT_SUPPORTED);
        
        // Try to replace in datastore.
        boolean result;
        try {
            result = datastore.replace(key, oldValue, newValue);
        } catch (RuntimeException e) {
            throw new StateManagementException("replace(key, oldValue, newValue) failed due to "
                    + "a datastore error.", e);
        }
        
        // Try to replace in memcache.
        try {
            // Try (once) to update memcache if necessary.
            if (!memcache.replace(key, oldValue, newValue) && result) { 
                // Replaced in datastore, not replaced in memcache.
                Object memcached = memcache.putIfAbsent(key, newValue);
                if (memcached != null) {
                    memcache.replace(key, memcached, newValue);
                }                
            }
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to update memcache during replace(key, oldValue, newValue).", e);
        }
        
        return result;
    }

    @Override
    public Object replace(Object key, Object value) {
        
        if (key == null || value == null) throw new IllegalArgumentException(NULL_NOT_SUPPORTED);
        
        // Try to replace in datastore.
        Object result;
        try {
            result = datastore.replace(key, value);
        } catch (RuntimeException e) {
            throw new StateManagementException("replace(key, value) failed due to a datastore error.", e);
        }
        
        // Try to replace in memcache.
        try {
            Object memcached = memcache.replace(key, value);
            // Try (once) to udpate in memcache if necessary.
            if (result != null && memcached == null) {
                memcache.putIfAbsent(key, value);
            }
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to update memcache during replace(key, value).", e);
        }
        
        return result;
    }
    
    /* ---------- Delete Operation  --------- */
    @Override
    public boolean remove(Object key, Object value) {
        
        if (key == null || value == null) throw new IllegalArgumentException(NULL_NOT_SUPPORTED);
        
        // Try to remove from datastore.
        boolean result;
        try {
            result = datastore.remove(key, value);
        } catch (RuntimeException e) {
            throw new StateManagementException("remove(key, value) failed due to a datastore error.", e);
        }
        
        // Try to remove from memcache.
        try {
            if (!memcache.remove(key, value) && result) {
                // Try (once) to update memcache if necessary.
                Object memcached = memcache.get(key);
                if (memcached != null) memcache.remove(key, memcached);
            }
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to update memcache during replace(key, value).", e);
        }
        
        return result;
    }

    
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GAEStateManager other = (GAEStateManager) obj;
        if (this.memcache != other.memcache 
                && (this.memcache == null || !this.memcache.equals(other.memcache))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 19 * hash + (this.memcache != null ? this.memcache.hashCode() : 0);
        return hash;
    }
    
}
