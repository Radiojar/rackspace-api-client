package com.pynode.rackspace.service.impl;

import java.io.Serializable;
import com.pynode.rackspace.service.utils.DefaultStateManager;
import java.util.List;
import java.util.Iterator;
import java.util.concurrent.ConcurrentMap;
import com.rackspace.cloud.api.Servers;
import com.rackspace.cloud.api.Rebuild;
import com.pynode.rackspace.service.ChangeListener;
import com.pynode.rackspace.service.EntityList;
import com.rackspace.cloud.api.BackupSchedule;
import com.rackspace.cloud.api.RebootType;
import com.rackspace.cloud.api.Server;
import com.pynode.rackspace.client.RackspaceCloudClientException;
import com.pynode.rackspace.client.aop.Interceptor;
import com.pynode.rackspace.client.AccountBase;
import com.pynode.rackspace.service.CloudServersService;
import com.pynode.rackspace.service.CloudServersServiceFactory.ServiceSetting;
import com.pynode.rackspace.service.FlavorManager;
import com.pynode.rackspace.service.ImageManager;
import com.pynode.rackspace.client.RackspaceCloudClient;
import com.pynode.rackspace.service.ServerManager;
import com.pynode.rackspace.service.SharedIpGroupManager;
import com.pynode.rackspace.service.StateManager;
import com.rackspace.cloud.api.Limits;
import com.rackspace.cloud.api.Reboot;
import com.rackspace.cloud.api.Resize;
import com.rackspace.cloud.api.Version;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.Map;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.pynode.rackspace.service.CloudServersServiceFactory.ServiceSetting.*;

/**
 *
 * @author Christos Fragoulides
 */
public class CloudServersServiceImpl implements CloudServersService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudServersServiceImpl.class);
    
    private RackspaceCloudClient client;
    private Map<String, Object> settings;
    private ServiceInfo serviceInfo;
    private ServerManager serverManager;
    private StateManager stateManager;

    public CloudServersServiceImpl(AccountBase accountBase, String username, String apiKey,
            Map<String, Object> settings) {
        
        this.settings = settings;
        
        // Create the client, apply settings.
        client = new RackspaceCloudClient(accountBase, username, apiKey);                    
        boolean cacheEnabled = false;
        long cacheTTL = 0;
        for (String setting : settings.keySet()) {
            
            switch (ServiceSetting.valueOf(setting)) {
                // Client GAE compatibility.
                case CLIENT_GAE_COMPATIBLE:
                    if (Boolean.TRUE.equals(settings.get(setting)))
                        client.setAppEngineCompatible(true);
                    break;
                // Client response cache timeout.
                case CLIENT_CACHE_TTL:
                    if (settings.get(setting) instanceof Long)
                        cacheTTL = (Long) settings.get(setting);
                    else {
                        LOGGER.warn("Invalid value for cache timeout setting, will use the default value.");
                    }
                    break;
                // Client response caching.
                case CLIENT_RESPONSE_CACHING:
                    if (Boolean.TRUE.equals(settings.get(setting))) cacheEnabled = true;
                    break;
                // StateManager setting. Use the default if none is set.
                case STATE_MANAGER:
                    if (settings.get(setting) instanceof StateManager) {
                        stateManager = (StateManager) settings.get(setting);
                        break;
                    } else {
                        LOGGER.warn("The passed in object for {} setting does not implement {}. "
                                + "Will use the default one.",
                                STATE_MANAGER.toString(), StateManager.class.getName());
                        break;
                    }
                default:
                    break;
            }
        }
        
        if (cacheEnabled) client.setInterceptor(new CachingInterceptor(this, cacheTTL));
        
        if (stateManager == null) 
            stateManager = DefaultStateManager.getInstance(accountBase, username, apiKey);
        
        // Initialize ServiceInfo.
        serviceInfo = new ServiceInfoImpl(this);
        // Inialize Entity Managers.
        serverManager = new ServerManagerImpl(this);
    }
    
    /* ----------------------------------------------------------------------------------------------- *
     *                                      Interface Implementation                                   *
     * ----------------------------------------------------------------------------------------------- */

    @Override
    public ServiceInfo getServiceInfo() {
        return serviceInfo;
    }

    @Override
    public ServerManager getServerManager() {
        return serverManager;
    }

    @Override
    public ImageManager getImageManager() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public SharedIpGroupManager getSharedIpGroupManager() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FlavorManager getFlavorManager() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    /* ----------------------------------------------------------------------------------------------- *
     *                                         Helper Classes                                          *
     * ----------------------------------------------------------------------------------------------- */
    
    private static class ServiceAccessor {
        
        private final CloudServersServiceImpl service;

        public ServiceAccessor(CloudServersServiceImpl service) {
            this.service = service;
        }

        protected CloudServersServiceImpl getService() {
            return service;
        }
        
        protected StateManager getStateManager() {
            return service.stateManager;
        }
        
    }
    
    /**
     * {@code ServiceInfo} implementation.
     */
    private static class ServiceInfoImpl extends ServiceAccessor implements ServiceInfo {
        
        public ServiceInfoImpl(CloudServersServiceImpl service) {
            super(service);
        }

        @Override
        public Version getVersionInfo() throws RackspaceCloudClientException {
            return getService().client.getVersionDetails();
        }

        @Override
        public Limits getLimits() throws RackspaceCloudClientException {
            return getService().client.listLimits();
        }

        @Override
        public Map<String, Object> getSettings() {
            return Collections.unmodifiableMap(getService().settings);
        }
        
    }
    
    /**
     * {@code ServerManager} implementation.
     */
    private static class ServerManagerImpl extends ServiceAccessor implements ServerManager {

        public ServerManagerImpl(CloudServersServiceImpl service) {
            super(service);
        }

        @Override
        public void reboot(Server s, RebootType type) throws RackspaceCloudClientException {
            Reboot reboot = new Reboot();
            reboot.setType(type);
            getService().client.action(s.getId(), reboot);
        }

        @Override
        public void rebuild(Server s, int imageId) throws RackspaceCloudClientException {
            Rebuild rebuild = new Rebuild();
            rebuild.setImageId(imageId);
            getService().client.action(s.getId(), rebuild);
        }

        @Override
        public void resize(Server s, int flavorId) throws RackspaceCloudClientException {
            Resize resize = new Resize();
            resize.setFlavorId(flavorId);
            getService().client.action(s.getId(), resize);
        }

        @Override
        public void confirmResize(Server s) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void revertResize(Server s) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void shareIp(Server s, String ip, long sharedIpGroupId, boolean configureServer) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void unshareIp(Server s, String ip) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setSchedule(Server s, BackupSchedule schedule) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public BackupSchedule getSchedule(Server s) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Server create(Server e) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void remove(Server e) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void update(Server e) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Server refresh(Server e) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Server find(long id) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void wait(Server e) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void wait(Server e, long timeout) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void notify(Server e, ChangeListener<Server> ch) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void stopNotify(Server e, ChangeListener<Server> ch) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public EntityList<Server> createList(boolean detail) throws RackspaceCloudClientException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public EntityList<Server> createList(final boolean detail, final long offset, final long limit) 
                throws RackspaceCloudClientException {
            
            final CloudServersServiceImpl service = getService();
            
            return new EntityList<Server>() {
                
                private List<Server> servers = getServers();
                private Iterator<Server> serversIterator = servers.iterator();
                
                private List<Server> getServers() throws RackspaceCloudClientException {
                    Servers result;
                    if (detail) result = service.client.listServersDetail(null, offset, limit);
                    else result = service.client.listServers(null, offset, limit);
                    return result.getServer();
                }

                @Override
                public long getLastModified() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public boolean isEmpty() {
                    return servers.isEmpty();
                }

                @Override
                public boolean hasNext() {
                    return serversIterator.hasNext();
                }

                @Override
                public Server next() {
                    return serversIterator.next();
                }

                @Override
                public void reset() throws RackspaceCloudClientException {
                    servers = getServers();
                    serversIterator = servers.iterator();
                }

                @Override
                public void delta() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }
                
            };
        }

        @Override
        public EntityList<Server> createDeltaList(boolean detail, long changesSince) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public EntityList<Server> createDeltaList(boolean detail, long changesSince, long offset,
                long limit) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        
    }

    /**
     * An {@code Interceptor} that provides caching of Rackspace responses (using the 
     * {@code StateManager}).
     * This helps avoiding hitting rate limits and makes the {@code RackspaceCloudService}
     * implementation operate faster.
     */
    private static class CachingInterceptor extends ServiceAccessor implements Interceptor {

        private static final long DEFAULT_CACHE_TTL = 30000;
        
        private long cacheTimeout = DEFAULT_CACHE_TTL;
        
        public CachingInterceptor(CloudServersServiceImpl service, long cacheTimeout) {
            super(service);
            if (cacheTimeout > 0) this.cacheTimeout = cacheTimeout;
        }

        private CachableResponse getCachedResponse(Integer requestHash) {
            
            ConcurrentMap<Integer, CachableResponse> responseCache = getResponseCache();           
            CachableResponse result = responseCache.get(requestHash);
            
            return result;
        }

        private void putCachedResponse(Integer requestHash, CachableResponse response) {
            
            ConcurrentMap<Integer, CachableResponse> responseCache = getResponseCache();
            
            CachableResponse existingRS = responseCache.putIfAbsent(requestHash, response);
            if (existingRS == null) {
                LOGGER.debug("Added new response to cache.");
                return;
            }
            
            long timeStamp = response.getTimeStamp(); 
            long existingTimestamp = existingRS.getTimeStamp();
            
            if (timeStamp > existingTimestamp) {
                boolean replaced = responseCache.replace(requestHash, existingRS, response);
                if (replaced) LOGGER.debug("Response cache updated.");
                else LOGGER.debug("Response cache contained a more recent response.");
            }
            
        }

        private synchronized ConcurrentMap<Integer, CachableResponse> getResponseCache() {
            ConcurrentMap<?, ?> toCast = getStateManager().getState();            
            return (ConcurrentMap<Integer, CachableResponse>) toCast;
        }

        @Override
        public Object intercept(ProceedingJoinPoint pjp) throws Throwable {
            LOGGER.debug("Intercepted call: {}", pjp.getSignature());
            
            int requestHash = pjp.getSignature().toLongString().hashCode();
            for (Object o : pjp.getArgs()) requestHash += o == null ? 0 : o.hashCode();
            
            Object response;
            boolean updateCache = false;
            CachableResponse cachedRS = getCachedResponse(requestHash);
            
            if (cachedRS == null) {
                LOGGER.debug("No cached response found, proceeding with actual call.");
                response = pjp.proceed();
                updateCache = true;
            } else {
                long timestamp = cachedRS.getTimeStamp();
                if (System.currentTimeMillis() - timestamp < cacheTimeout) {
                    LOGGER.debug("Found an up-to-date cached response.");
                    response = deserializeResponse(cachedRS.getData());
                } else {
                    LOGGER.debug("Found an outdated cached response, will update cache.");
                    response = pjp.proceed();
                    updateCache = true;
                }
            }
            
            if (updateCache) {
                LOGGER.debug("Updating cache..");
                byte[] data = serializeResponse(response);
                cachedRS = new CachableResponse(data);
                putCachedResponse(requestHash, cachedRS);
            }
            
            return response;
        }
        
        private byte[] serializeResponse(Object response) throws JAXBException {
            QName qname = new QName("http://docs.rackspacecloud.com/servers/api/v1.0",
                    response.getClass().getSimpleName().toLowerCase());
            JAXBElement el = new JAXBElement(qname, response.getClass(), response);
            Marshaller marshaller = RackspaceCloudClient.getJAXBContext().createMarshaller();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            marshaller.marshal(el, baos);
            return baos.toByteArray();
        }
        
        private Object deserializeResponse(byte[] data) throws JAXBException {
            Unmarshaller unmarshaller = RackspaceCloudClient.getJAXBContext().createUnmarshaller();
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            JAXBElement el = (JAXBElement) unmarshaller.unmarshal(bais);
            return el.getValue();
        }
    }
    
    private static class CachableResponse implements Serializable {

        public CachableResponse(byte[] data) {
            timeStamp = System.currentTimeMillis();
            this.data = data;
        }       
        
        private long timeStamp = 0;
        private byte[] data;

        public long getTimeStamp() {
            return timeStamp;
        }

        public byte[] getData() {
            return data;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final CachableResponse other = (CachableResponse) obj;
            if (this.timeStamp != other.timeStamp) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 23 * hash + (int) (this.timeStamp ^ (this.timeStamp >>> 32));
            return hash;
        }
        
    }
    
}
