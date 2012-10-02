package com.pynode.rackspace.client;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Christos Fragoulides
 */
public class ClientTest {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientTest.class);
    
    private RackspaceCloudClient client;
    
    public ClientTest() { }

    @BeforeClass
    public static void setUpClass() throws Exception {

    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
        String user = System.getProperty("rackspace.user");
        String key = System.getProperty("rackspace.apiKey");
                
        client = new RackspaceCloudClient(AccountBase.UK, user, key);
        LOGGER.info("Created new client instance.");
    }
    
    @After
    public void tearDown() {
    }

    protected RackspaceCloudClient getClient() {
        return client;
    }
    
}
