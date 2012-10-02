package com.pynode.rackspace.client;

import com.pynode.rackspace.client.RackspaceCloudClientException.ErrorSource;
import com.rackspace.cloud.api.Servers;
import java.util.Calendar;
import com.rackspace.cloud.api.Image;
import com.rackspace.cloud.api.Images;
import com.rackspace.cloud.api.Limits;
import com.rackspace.cloud.api.OverLimitAPIFault;
import org.slf4j.Logger;
import com.rackspace.cloud.api.Version;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import static org.junit.Assert.*;

/**
 *
 * @author Christos Fragoulides
 */
public class RackspaceCloudClientTest extends ClientTest {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RackspaceCloudClientTest.class);
    
    public RackspaceCloudClientTest() {}
    
    @Test
    public void testVersionDetails() throws RackspaceCloudClientException {
        Version verDetails = getClient().getVersionDetails();
        StringBuilder sb = new StringBuilder("Version:\n");
        sb.append("   ID: ");
        sb.append(verDetails.getId());
        sb.append('\n');
        sb.append("   Status: ");
        sb.append(verDetails.getStatus());
        sb.append('\n');
        sb.append("   DocURL: ");
        sb.append(verDetails.getDocURL());
        sb.append('\n');
        sb.append("   WADL: ");
        sb.append(verDetails.getWadl());
        sb.append('\n');
        LOGGER.info("Received XML response for versionDetails() call:\n{}", sb);    
    }
    
    @Test
    public void testLimits() throws Exception {
        try {
            Limits limits = getClient().listLimits();
        } catch (Exception e) {
            LOGGER.error("testLimits() failed.", e);
            throw e;
        }
    }
    
    @Test
    public void testListImages() throws Exception {
        try {
            Images images = getClient().listImages(null, null, null);
        } catch (Exception e) {
            LOGGER.error("testListImages() failed.", e);
            throw e;
        }
    }
    
    @Test
    public void testListImagesDetail() throws Exception {
        try {
            Calendar since = Calendar.getInstance();
            since.add(Calendar.MONTH, -6);
            LOGGER.info("Getting images details, changed since {}", String.format("%1$tF", since));
            Images images = getClient().listImagesDetail(since.getTimeInMillis() / 1000, null, null);
            int testCalls = 0;
            for (Image image : images.getImage()) {
                Image rImage = getClient().getImage(image.getId());
                if (testCalls++ == 9) break;
            }            
        } catch (Exception e) {
            LOGGER.error("testListImagesDetail() failed.", e);
            throw e;
        }
    }
    
    @Test
    public void testServers() throws Exception {
        try {
            // Test listServers()
            Servers servers = getClient().listServers(null, null, null);
            // Test listServersDetail()            
            Calendar since = Calendar.getInstance();
            since.add(Calendar.MONTH, -6);
            servers = getClient().listServersDetail(since.getTimeInMillis() / 1000, null, null);           
        } catch (RackspaceCloudClientException e) {        
            if (ErrorSource.SERVER.equals(e.getErrorSource())) {
                if (e.getServerFault() instanceof OverLimitAPIFault) {
                    OverLimitAPIFault overLimit = (OverLimitAPIFault) e.getServerFault();
                    Calendar retry = overLimit.getRetryAfter().toGregorianCalendar();
                    LOGGER.warn("testServers() failed because of an over limit, retry after: {}", retry);
                    return;
                }
            }
            LOGGER.error("testServers() failed.", e);
            throw e;
        }
    }
    
    @Test
    public void testPaginatedCollections() throws Exception {
        
        LOGGER.info("++++++++ Testing Pagination ++++++++");
        
        long totalUnpaged = getClient().listImages(null, null, null).getImage().size();
        
        long pageSize = 10;
        long offset = 0;
        long total = 0;
        Images currentPage;
        do {
            currentPage = getClient().listImages(null, offset, pageSize);
            offset += pageSize;
            total += currentPage.getImage().size();
        } while (!currentPage.getImage().isEmpty());
        
        assertEquals(totalUnpaged, total);
        
    }    
    
}
