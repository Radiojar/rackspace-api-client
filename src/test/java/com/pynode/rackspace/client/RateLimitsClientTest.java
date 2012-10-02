package com.pynode.rackspace.client;

import java.util.Calendar;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import static org.junit.Assert.*;

/**
 *
 * @author Christos Fragoulides
 */
public class RateLimitsClientTest extends ClientTest {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitsClientTest.class);
    
    public RateLimitsClientTest() { }
    
    @Test
    public void testRateLimits() throws RackspaceCloudClientException {
        
        LOGGER.info("Performing Rate Limits Test....\n++++++++++++++++++++++++++++++++++++++++++");
        
        int numCalls = 10;
        
        long timeTaken = -System.currentTimeMillis();
        for (int i = 0; i < numCalls; i++) {
            getClient().listLimits();
        }
        timeTaken += System.currentTimeMillis();
        
        LOGGER.info("Requested limits from rackspace {} times in {} secs.", numCalls, timeTaken / 1000F);
    }
    
    @Test
    public void testChangesSince() throws RackspaceCloudClientException {
        
        LOGGER.info("Performing Changes Since Test....\n++++++++++++++++++++++++++++++++++++++++++");
        
        getClient().listLimits();
        getClient().listImagesDetail(null, null, null);
        getClient().listImagesDetail(null, null, null);
        getClient().listImagesDetail(null, null, null);
        getClient().listImagesDetail(null, null, null);
        getClient().listImagesDetail(null, null, null);
        getClient().listImagesDetail(null, null, null);
        getClient().listImagesDetail(null, null, null);
        getClient().listLimits();
        
        LOGGER.info("With a value other than 0:\n+++++++++++++++++++++++++++++++++++++++++++");
        
        getClient().listLimits();
        Calendar since = Calendar.getInstance();
        since.add(Calendar.MONTH, -6);
        getClient().listImagesDetail(since.getTimeInMillis() / 1000, null, null);
        getClient().listImagesDetail(since.getTimeInMillis() / 1000, null, null);
        getClient().listLimits();
    }

}
