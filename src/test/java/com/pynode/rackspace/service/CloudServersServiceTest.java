package com.pynode.rackspace.service;

import com.pynode.rackspace.client.AccountBase;
import com.pynode.rackspace.service.CloudServersServiceFactory.ServiceSetting;
import com.rackspace.cloud.api.Limits;
import com.rackspace.cloud.api.RateLimit;
import com.rackspace.cloud.api.Server;
import java.util.HashMap;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Christos Fragoulides
 */
public class CloudServersServiceTest {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudServersService.class);
    
    private static final String USER = System.getProperty("rackspace.user");
    private static final String API_KEY = System.getProperty("rackspace.apiKey");
    
    public CloudServersServiceTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Test
    public void testService() throws Exception {
        
        LOGGER.info("Rackspace User: {}", System.getProperty("rackspace.user"));
        LOGGER.info("Rackspace API key: {}", System.getProperty("rackspace.apiKey"));
        
        Map<String, Object> settings = new HashMap<String, Object>();
        settings.put(ServiceSetting.CLIENT_RESPONSE_CACHING.name(), true);
        CloudServersService service = 
                CloudServersServiceFactory.getService(AccountBase.UK, USER, API_KEY, settings);
        
        service.getServiceInfo().getLimits();
        // This call should return results from the cache.
        Limits limits = service.getServiceInfo().getLimits();
        
        StringBuilder rateLimits = new StringBuilder();
        for (RateLimit l : limits.getRate().getLimit()) {
            rateLimits.append(l.getVerb()).append('\t')
                      .append(l.getRegex()).append('\t')
                      .append(l.getValue()).append('\t')
                      .append(l.getUnit()).append('\n');
        }
        
        LOGGER.info("Rate Limits:\n{}", rateLimits.toString());
        
        ServerManager serverManager = service.getServerManager();
        for (int i = 0; i < 3; i++) {
            
            EntityList<Server> list = serverManager.createList(true, 0, 1000);
            while (list.hasNext()) {
                Server s = list.next();
                LOGGER.info("Server - ID: {},\t Name: {},\t Status: {}",
                        new Object[]{s.getId(), s.getName(), s.getStatus()});
            }
            
        }
    }
}
