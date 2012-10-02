package com.pynode.rackspace.service.utils;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheService.IdentifiableValue;
import com.google.appengine.api.memcache.MemcacheService.SetPolicy;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.memcache.StrictErrorHandler;
import java.util.AbstractMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 *
 * @author Christos Fragoulides
 */
public class MemcacheConcurrentMap<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V> {
    
    private static final String NULL_NOT_SUPPORTED = "This map does not support null values, nor keys.";

    private String namespace;
    private MemcacheService memcache;

    public MemcacheConcurrentMap(String namespace) {
        memcache = MemcacheServiceFactory.getMemcacheService(namespace);        
        memcache.setErrorHandler(new StrictErrorHandler());
    }
    
    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException("Only ConcurrentMap methods supported, in addition to get()");
    }

    /* ----------  Create Operation --------- */
    
    @Override
    public V putIfAbsent(K key, V value) {
        
        if (key == null || value == null) throw new IllegalArgumentException(NULL_NOT_SUPPORTED);
        
        // Try to atomically insert the key and value if the key has not been set yet.
        while (!memcache.contains(key)) {
            if (memcache.put(key, value, null, SetPolicy.ADD_ONLY_IF_NOT_PRESENT)) return null;
        }
        
        // Key exist, but may be null (which means it does not exist for this implementation).
        IdentifiableValue oldValue;
        do {
            oldValue = memcache.getIdentifiable(key);
            // Proceed only if there was no value previously. Else return the old value.
            if (oldValue != null && oldValue.getValue() != null) return (V) oldValue.getValue();
        } while (!memcache.putIfUntouched(key, oldValue, value));
        // No value was associated with the key (and the given value was put).
        return null;
    }

    /* ----------   Read Operation  --------- */
    
    @Override
    public V get(Object key) {
        
        if (key == null) throw new IllegalArgumentException(NULL_NOT_SUPPORTED);
        
        return (V) memcache.get(key);
    }
    
    /* ---------- Update Operations --------- */
    

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        
        if (key == null || oldValue == null || newValue == null) 
            throw new IllegalArgumentException(NULL_NOT_SUPPORTED);
        
        if (!memcache.contains(key)) return false;
        
        IdentifiableValue existingValue;
        do {
            existingValue = memcache.getIdentifiable(key);
            if (existingValue.getValue() == null || !existingValue.getValue().equals(oldValue)) {
                // Key does not exist or is not equal to old value.
                return false;
            }
        } while (!memcache.putIfUntouched(key, existingValue, newValue));
        // Key was associated with oldValue and is now associated with newValue.
        return true;
    }
    
    @Override
    public V replace(K key, V value) {
        
        if (key == null || value == null) throw new IllegalArgumentException(NULL_NOT_SUPPORTED);
        
        if (!memcache.contains(key)) return null;
        
        // Inverse operation of putIfAbsent()
        IdentifiableValue oldValue;
        do {
            oldValue = memcache.getIdentifiable(key);
            // Proceed only if there was a value previously. Else return null.
            if (oldValue.getValue() == null) return null;
        } while (!memcache.putIfUntouched(key, oldValue, value));
        // A value was associated with the key (and the given value was put).
        return (V) oldValue.getValue();
    }

    /* ---------- Delete Operation  --------- */
    
    @Override
    public boolean remove(Object key, Object value) {
        
        if (key == null || value == null) throw new IllegalArgumentException(NULL_NOT_SUPPORTED);
        
        if (!memcache.contains(key)) return false;
        
        IdentifiableValue existingValue;
        do {
            existingValue = memcache.getIdentifiable(key);
            if (existingValue.getValue() != null && !existingValue.getValue().equals(value)) {
                // Key exist but is not equal to old value.
                return false;
            }
        } while (!memcache.putIfUntouched(key, existingValue, null));
        // Key was associated with oldValue and is now associated
        // with null (considered as deleted by this implementation).
        return true;        
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MemcacheConcurrentMap<K, V> other = (MemcacheConcurrentMap<K, V>) obj;
        if ((this.namespace == null) ? (other.namespace != null) : !this.namespace.equals(other.namespace)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + (this.namespace != null ? this.namespace.hashCode() : 0);
        return hash;
    }
    
    
}
