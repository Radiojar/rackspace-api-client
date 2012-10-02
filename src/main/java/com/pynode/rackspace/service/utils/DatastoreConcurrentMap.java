package com.pynode.rackspace.service.utils;

import com.google.appengine.api.NamespaceManager;
import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceConfig;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Transaction;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.util.AbstractMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Christos Fragoulides
 */
public class DatastoreConcurrentMap<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V> {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DatastoreConcurrentMap.class);
    
    private static final String NULL_NOT_SUPPORTED = "This map does not support null values, nor keys.";
    
    private static final String ENTITY_KIND = "ConcurrentMapEntry";
    private static final String ENTRY_DATA_PROPERTY = "entryData";
    
    private DatastoreService datastore;
    private String namespace;

    public DatastoreConcurrentMap(String namespace) {
        DatastoreServiceConfig config = DatastoreServiceConfig.Builder.withDefaults();
        datastore = DatastoreServiceFactory.getDatastoreService(config);
        this.namespace = namespace;
    }    

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException("Only ConcurrentMap methods supported, in addition to get()");
    }

    /* ----------  Create Operation --------- */
    
    @Override
    public V putIfAbsent(K key, V value) {
        
        if (key == null || value == null) throw new IllegalArgumentException(NULL_NOT_SUPPORTED);
        
        V oldValue;
        Key entityKey = getEntityKey(key.hashCode());
        boolean rolledBack = false;
        
        int retries = 0;
        do {
            if (retries++ > 0 && LOGGER.isDebugEnabled()) {
                LOGGER.debug("Retrying putIfAbsent() after {} attempts..", retries);
            }
            
            Transaction txn = datastore.beginTransaction();
            try {
                Entity entry = datastore.get(txn, entityKey);
                oldValue = deserializeEntry((Blob) entry.getProperty(ENTRY_DATA_PROPERTY)).getValue();
                txn.commit();
            } catch (EntityNotFoundException ex) {
                // Entity is absent.
                Entity entry = new Entity(entityKey);
                entry.setUnindexedProperty(ENTRY_DATA_PROPERTY, serializeEntry(key, value));
                datastore.put(txn, entry);
                oldValue = null;
                txn.commit();
            } finally {
                if (txn.isActive()) {
                    txn.rollback();
                    rolledBack = true;
                }
            }
        } while (rolledBack);
        
        return oldValue;
    }

    /* ----------   Read Operation  --------- */
    
    @Override
    public V get(Object key) {
        
        if (key == null) throw new IllegalArgumentException(NULL_NOT_SUPPORTED);
        
        Key entityKey = getEntityKey(key.hashCode());
        try {
            Entity entry = datastore.get(entityKey);
            return deserializeEntry((Blob) entry.getProperty(ENTRY_DATA_PROPERTY)).getValue();
        } catch (EntityNotFoundException ex) {
            return null;
        }
        
    }
    
    /* ---------- Update Operations --------- */

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        
        if (key == null || oldValue == null || newValue == null) 
            throw new IllegalArgumentException(NULL_NOT_SUPPORTED);
        

        V existingValue;
        Key entityKey = getEntityKey(key.hashCode());
        boolean rolledBack = false;
        
        int retries = 0;
        do {
            if (retries++ > 0 && LOGGER.isDebugEnabled()) {
                LOGGER.debug("Retrying replace(key, oldValue, newValue) after {} attempts..", retries);
            }
            
            Transaction txn = datastore.beginTransaction();
            try {
                Entity entry = datastore.get(txn, entityKey);
                existingValue = deserializeEntry((Blob) entry.getProperty(ENTRY_DATA_PROPERTY)).getValue();
                if (oldValue.equals(existingValue)) {
                    entry.setUnindexedProperty(ENTRY_DATA_PROPERTY, serializeEntry(key, newValue));
                    datastore.put(txn, entry);
                } else existingValue = null;
                txn.commit();
            } catch (EntityNotFoundException ex) {
                // Entity is absent.
                existingValue = null;
                txn.commit();
            } finally {
                if (txn.isActive()) {
                    txn.rollback();
                    rolledBack = true;
                }
            }
        } while (rolledBack);
        
        
        return existingValue != null;
    }

    @Override
    public V replace(K key, V value) {
        
        if (key == null || value == null) throw new IllegalArgumentException(NULL_NOT_SUPPORTED);
        
        V existingValue;
        Key entityKey = getEntityKey(key.hashCode());
        boolean rolledBack = false;
        
        int retries = 0;
        do {
            if (retries++ > 0 && LOGGER.isDebugEnabled()) {
                LOGGER.debug("Retrying replace(key, value) after {} attempts..", retries);
            }
            
            Transaction txn = datastore.beginTransaction();
            try {
                Entity entry = datastore.get(txn, entityKey);
                existingValue = deserializeEntry((Blob) entry.getProperty(ENTRY_DATA_PROPERTY)).getValue();
                entry.setUnindexedProperty(ENTRY_DATA_PROPERTY, serializeEntry(key, value));
                datastore.put(txn, entry);
                txn.commit();
            } catch (EntityNotFoundException ex) {
                // Entity is absent.
                existingValue = null;
                txn.commit();
            } finally {
                if (txn.isActive()) {
                    txn.rollback();
                    rolledBack = true;
                }
            }
        } while (rolledBack);
        
        
        return existingValue;
    }
    
    /* ---------- Delete Operation  --------- */
    
    @Override
    public boolean remove(Object key, Object value) {
        
        if (key == null || value == null) throw new IllegalArgumentException(NULL_NOT_SUPPORTED);
        
        V existingValue;
        Key entityKey = getEntityKey(key.hashCode());
        boolean rolledBack = false;
        
        int retries = 0;
        do {
            if (retries++ > 0 && LOGGER.isDebugEnabled()) {
                LOGGER.debug("Retrying remove(key, value) after {} attempts..", retries);
            }
                        
            Transaction txn = datastore.beginTransaction();
            try {
                Entity entry = datastore.get(txn, entityKey);                
                existingValue = deserializeEntry((Blob) entry.getProperty(ENTRY_DATA_PROPERTY)).getValue();
                if (value.equals(existingValue)) {
                    datastore.delete(txn, entityKey);
                } else existingValue = null;
                txn.commit();
            } catch (EntityNotFoundException ex) {
                // Entity is absent.
                existingValue = null;
                txn.commit();
            } finally {
                if (txn.isActive()) {
                    txn.rollback();
                    rolledBack = true;
                }
            }
        } while (rolledBack);
        
        
        return existingValue != null;
    }
    
    /* ---------- Implementation Methods & Classes ---------- */
    
    
    private Key getEntityKey(Integer mapKeyHash) {
        String oldNamespace = NamespaceManager.get();
        NamespaceManager.set(namespace);
        try {            
            return KeyFactory.createKey(ENTITY_KIND, mapKeyHash.toString());
        } finally {
            NamespaceManager.set(oldNamespace);
        }        
    }

    private Blob serializeEntry(K key, V value) {
        ObjectOutputStream out = null;
        try {
            DatastoreEntry<K, V> entry = new DatastoreEntry<K, V>(key, value);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            out = new ObjectOutputStream(baos);
            out.writeObject(entry);
            out.close();
            return new Blob(baos.toByteArray());
        } catch (IOException ex) {
            throw new SerializationException("Failed to serialize entry.", ex);
        }
    }

    private Entry<K, V> deserializeEntry(Blob data) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data.getBytes());
            ObjectInputStream in = new ObjectInputStream(bais);
            Entry<K, V> result = (Entry<K, V>) in.readObject();
            in.close();
            return result;
        } catch (Exception e) {
            throw new SerializationException("Failed to deserialize entry", e);
        }
    }
    
    private static class SerializationException extends RuntimeException {

        public SerializationException(String message, Throwable cause) {
            super(message, cause);
        }
        
    }
    
    private class DatastoreEntry<K, V> implements Entry<K, V>, Serializable {
        
        private K key;
        private V value;

        public DatastoreEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            V old = this.value;
            this.value = value;
            return value;
        }
        
        private void writeObject(java.io.ObjectOutputStream out) throws IOException {
            out.writeObject(key);
            out.writeObject(value);
        }
        
        private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
            key = (K) in.readObject();
            value = (V) in.readObject();
        }
        
        private void readObjectNoData() throws ObjectStreamException {
            throw new StreamCorruptedException();
        }
    }
    
}
