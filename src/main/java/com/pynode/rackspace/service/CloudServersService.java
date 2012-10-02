package com.pynode.rackspace.service;

import com.pynode.rackspace.client.RackspaceCloudClientException;
import com.rackspace.cloud.api.Limits;
import com.rackspace.cloud.api.Version;
import java.util.Map;

/**
 * Interface providing a means by which entity managers for the Cloud Servers API may be created.
 * This interface is based on the guidelines contained in Rackspace's language binding guide.
 * @author Christos Fragoulides
 * @see <a href="http://docs.rackspace.com/servers/api/cs-bindguide-latest.pdf">
 * API Language Binding Guide, section "The Cloud Servers Service"</a>.
 */
public interface CloudServersService {
    
    ServiceInfo getServiceInfo();
    ServerManager getServerManager();
    ImageManager getImageManager();
    SharedIpGroupManager getSharedIpGroupManager();
    FlavorManager getFlavorManager();
    
    public interface ServiceInfo {
        Version getVersionInfo() throws RackspaceCloudClientException;
        Limits getLimits() throws RackspaceCloudClientException;
        Map<String, Object> getSettings();
    }
    
}
