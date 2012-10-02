package com.pynode.rackspace.service.utils;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.appengine.tools.development.testing.LocalMemcacheServiceTestConfig;
import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import static org.junit.Assert.*;

/**
 *
 * @author Christos Fragoulides
 */
public class GAEConcurrentMapTest {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(GAEConcurrentMapTest.class);
    
    private final LocalServiceTestHelper helper =
        new LocalServiceTestHelper(new LocalMemcacheServiceTestConfig(),
            new LocalDatastoreServiceTestConfig().setDefaultHighRepJobPolicyUnappliedJobPercentage(100));
    
    public GAEConcurrentMapTest() {
    }
 
    @Before
    public void setUp() {
        helper.setUp();
    }
    
    @After
    public void tearDown() {
        helper.tearDown();
    }
    
    
    @Test
    public void testGAEConcurrentMaps() throws Exception {
        
        // Sanity check of the test itself using a ConcurrentHashMap.
        ConcurrentMap<Integer, WorkItem> map = new ConcurrentHashMap<Integer, WorkItem>();
        testMap(map);
        
        // Now perform the actual test with memcache.
        map = new MemcacheConcurrentMap<Integer, WorkItem>("test");
        for (int i = 0; i < 100; i++) testMap(map);
        
        // And with datastore.
        map = new DatastoreConcurrentMap<Integer, WorkItem>("test");
        for (int i = 0; i < 100; i++) testMap(map);
    }
    
    @Test
    public void testGAEStateManager() throws Exception {
        // Test GAEStateManager.
        GAEStateManager stateManager = new GAEStateManager();
        ConcurrentMap<Integer, WorkItem> map =
                (ConcurrentMap<Integer, WorkItem>) ((ConcurrentMap<?, ?>) stateManager.getState());
        
        for (int i = 0; i < 100; i++) testMap(map);
    }
    
    private void testMap(ConcurrentMap<Integer, WorkItem> map) {
        
        Integer key = 0;
        Integer nonExistingKey = 1;
        WorkItem item = new WorkItem();
        WorkItem newItem = new WorkItem();
        
        // Test putIfAbsent().
        assertNull("putIfAbsent() failed.", map.putIfAbsent(key, item));        
        assertEquals("putIfAbsent() failed.", map.putIfAbsent(key, item), item);
        
        // Test replace(key, oldValue, newValue)
        assertFalse("replace(key, oldValue, newValue) failed.", map.replace(nonExistingKey, item, newItem));
        assertFalse("replace(key, oldValue, newValue) failed.", map.replace(key, newItem, newItem));
        assertTrue("replace(key, oldValue, newValue) failed.", map.replace(key, item, newItem));
        // key is now mapped to newItem.
        
        // Test replace(key, value)
        assertNull("replace(key, value) failed.", map.replace(nonExistingKey, item));
        assertEquals("replace(key, value) failed.", newItem, map.replace(key, item));
        // Map now contains item.
        
        assertFalse("remove(key, value) failed.", map.remove(nonExistingKey, item));
        assertFalse("remove(key, value) failed.", map.remove(key, newItem));
        assertTrue("remove(key, value) failed.", map.remove(key, item));
        // Map is now empty.
                
    }    
    
    private static class WorkItem implements Serializable {
        
        private static int itemsCreated = 0;
        
        private int work = 10;
        private int serialNumber;

        public WorkItem() {
            serialNumber = ++itemsCreated;
        }

        public int getWork() {
            return work;
        }

        public void setWork(int work) {
            this.work = work;
        }

        @Override
        protected Object clone() {
            WorkItem clone = new WorkItem();
            clone.serialNumber = serialNumber;
            clone.work = work;
            return clone;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final WorkItem other = (WorkItem) obj;
            if (this.work != other.work) {
                return false;
            }
            if (this.serialNumber != other.serialNumber) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 97 * hash + this.work;
            hash = 97 * hash + this.serialNumber;
            return hash;
        }       
        
    }
}
