package com.pynode.rackspace.service;

import com.pynode.rackspace.client.RackspaceCloudClientException;
import com.rackspace.cloud.api.BackupSchedule;
import com.rackspace.cloud.api.RebootType;
import com.rackspace.cloud.api.Server;

/**
 *
 * @author Christos Fragoulides
 */
public interface ServerManager extends EntityManager<Server> {
    
    void reboot(Server s, RebootType type) throws RackspaceCloudClientException;
    
    void rebuild(Server s, int imageId) throws RackspaceCloudClientException;
    
    void resize(Server s, int flavorId) throws RackspaceCloudClientException;
    
    void confirmResize(Server s) throws RackspaceCloudClientException;
    
    void revertResize(Server s) throws RackspaceCloudClientException;
    
    void shareIp(Server s, String ip, long sharedIpGroupId, boolean configureServer) 
            throws RackspaceCloudClientException;
    
    void unshareIp(Server s, String ip) throws RackspaceCloudClientException;
    
    void setSchedule(Server s, BackupSchedule schedule) throws RackspaceCloudClientException;
    
    BackupSchedule getSchedule(Server s) throws RackspaceCloudClientException;
    
}
