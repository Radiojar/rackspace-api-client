package com.pynode.rackspace.client;

import org.slf4j.Logger;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.appengine.tools.development.testing.LocalURLFetchServiceTestConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
//import static org.junit.Assert.*;

/**
 * 
 * @author Christos Fragoulides
 */
public class GAECompatibilityTest extends ClientTest {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(GAECompatibilityTest.class);
    
    private final LocalServiceTestHelper helper =
            new LocalServiceTestHelper(new LocalURLFetchServiceTestConfig());
    
    public GAECompatibilityTest() { }
    
    @Before
    @Override
    public void setUp() {
        super.setUp();
        helper.setUp();
        getClient().setAppEngineCompatible(true);
    }
    
    @After
    @Override
    public void tearDown() {
        super.tearDown();
        helper.tearDown();
    }
    
    @Test
    public void testClientOnGAE() throws RackspaceCloudClientException {
        getClient().listLimits();
    }
    
}
