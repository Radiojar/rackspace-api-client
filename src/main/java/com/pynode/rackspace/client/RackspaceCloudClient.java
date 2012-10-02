package com.pynode.rackspace.client;

import com.pynode.cxf.utils.GAEHttpTransportFactory;
import com.pynode.rackspace.client.aop.Intercept;
import com.pynode.rackspace.client.aop.Interceptable;
import com.pynode.rackspace.client.aop.Interceptor;
import com.rackspace.cloud.api.AddressList;
import com.rackspace.cloud.api.Addresses;
import com.rackspace.cloud.api.BackupSchedule;
import com.rackspace.cloud.api.ConfirmResize;
import com.rackspace.cloud.api.Flavor;
import com.rackspace.cloud.api.Flavors;
import com.rackspace.cloud.api.Image;
import com.rackspace.cloud.api.Images;
import com.rackspace.cloud.api.Limits;
import com.rackspace.cloud.api.ObjectFactory;
import com.rackspace.cloud.api.Reboot;
import com.rackspace.cloud.api.Rebuild;
import com.rackspace.cloud.api.Resize;
import com.rackspace.cloud.api.RevertResize;
import com.rackspace.cloud.api.Server;
import com.rackspace.cloud.api.Servers;
import com.rackspace.cloud.api.ShareIp;
import com.rackspace.cloud.api.SharedIpGroup;
import com.rackspace.cloud.api.SharedIpGroups;
import com.rackspace.cloud.api.Version;
import com.rackspace.cloud.client.FlavorsResource;
import com.rackspace.cloud.client.ImagesResource;
import com.rackspace.cloud.client.LimitsResource;
import com.rackspace.cloud.client.ServerAddresses;
import com.rackspace.cloud.client.ServerIDResource;
import com.rackspace.cloud.client.ServersResource;
import com.rackspace.cloud.client.SharedIpGroupsResource;
import com.rackspace.cloud.client.Versions;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.ClientWebApplicationException;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.ServerWebApplicationException;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.common.gzip.GZIPInInterceptor;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A client for the Rackspace Cloud Service. Public methods whose results may be cached
 * are annotated with an {@link Intercept &#64;Intercept} annotation. A system using this client 
 * may register an {@link Interceptor} that will provide caching behavior for these methods.
 * @author Christos Fragoulides
 * @see <a href="http://docs.rackspace.com/servers/api/v1.0/cs-devguide/content/index.html">
 * Cloud Servers Developer Guide</a>
 * @see <a href="http://docs.rackspace.com/servers/api/cs-bindguide-latest.pdf">
 * API Language Binding Guide</a>
 */
public class RackspaceCloudClient implements Interceptable {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RackspaceCloudClient.class);


    private static final JAXBContext JAXB_CTX;
    static {
        try {
            JAXB_CTX = JAXBContext.newInstance(Version.class.getPackage().getName());
        } catch (JAXBException ex) {
            String msg = "Failed to initialize JAXBContext.";
            LOGGER.error(msg, ex);
            throw new RuntimeException("Failed to initialize JAXBContext.", ex);
        }
    }
    
    private static final int MAX_RETRIES = 3;
    
    /**
     * Base URL for Cloud Servers API calls.
     * @see <a href="http://docs.rackspace.com/servers/api/cs-bindguide-latest.pdf">
     * API Language Binding Guide, section "Authentication"</a>
     */
    private URI baseUri;
    
    private AccountBase accountBase;
    private String user;
    private String authKey;
    
    /**
     * Flag that determines if GZIP compression should be used or not. Default is {@code true}.
     */
    private boolean compressionEnabled = true;
    /**
     * Flag that when set to {@code true}, a customized HttpTransportFactory will be used,
     * that utilizes the URLFetchService of GAE.
     */
    private boolean appEngineCompatible = false;
    
    private Interceptor interceptor = null;
    
    private static final ThreadLocal<Client> CLIENT_STORE = new ThreadLocal<Client>();
    
    private Long accountId = null;
    private String authToken = null;

    public RackspaceCloudClient(final AccountBase accountBase, String user, String authKey) {
        this.accountBase = accountBase;
        this.user = user;
        this.authKey = authKey;

    }
    
    
    
    /* ------------------------- General API Calls ------------------------- */
    
    /**
     * Returns details about the Rackspace Cloud Servers API version being
     * used by this client.
     * @return {@code Version} version details.
     * @throws RackspaceCloudClientException 
     */
    @Intercept
    public Version getVersionDetails() throws RackspaceCloudClientException {
        return getVersions().versionDetails();
    }
    
    /**
     * Returns the Limits configured for the active account used to access
     * Rackspace Cloud Servers API.
     * @return Limits - rate and absolute limits.
     * @throws RackspaceCloudClientException in case of an error.
     * @see <a href="http://docs.rackspace.com/servers/api/v1.0/cs-devguide/content/index.html">
     * Cloud Servers Developer Guide, section 3.8</a>
     */
    @Intercept
    public Limits listLimits() throws RackspaceCloudClientException {
        
        final LimitsResource limitsResource = getVersions().getAccountID(accountId).getLimitsResource();
        
        return new ApiCaller<Limits>(this) {

            @Override
            Limits call() throws RackspaceCloudClientException {                
                return limitsResource.listLimits(authToken);
            }
            
        }.makeCall(limitsResource);
    }
    
    
    /* ------------------------- Server Images methods ------------------------- */
    
    public Images listImages(final Long changesSince, final Long offset, final Long limit) 
            throws RackspaceCloudClientException {
        
        final ImagesResource imagesResource = getVersions().getAccountID(accountId).getImagesResource();
        return new ApiCaller<Images>(this) {

            @Override
            Images call() throws RackspaceCloudClientException {
                
                return imagesResource.listImages(authToken, changesSince, offset, limit);
            }                       
            
        }.makeCall(imagesResource);
    }
    
    public Images listImagesDetail(final Long changesSince, final Long offset, final Long limit) 
            throws RackspaceCloudClientException {
        
        final ImagesResource imagesResource = getVersions().getAccountID(accountId).getImagesResource();
        
        return new ApiCaller<Images>(this) {

            @Override
            Images call() throws RackspaceCloudClientException {                
                return imagesResource.listImagesDetail(authToken, changesSince, offset, limit);
            }
            
        }.makeCall(imagesResource);
    }
    
    public Image getImage(final int imageId) throws RackspaceCloudClientException {
    
        final ImagesResource imagesResource = getVersions().getAccountID(accountId).getImagesResource();
        
        return new ApiCaller<Image>(this) {
                
            
            @Override
            Image call() throws RackspaceCloudClientException {                               
                return imagesResource.getImage(imageId, authToken);
            }
            
        }.makeCall(imagesResource);
    }
    
    /* -------------------------    Server methods     ------------------------- */
    
    @Intercept
    public Servers listServers(final Long changesSince, final Long offset, final Long limit) 
            throws RackspaceCloudClientException {
        
        final ServersResource serversResource = getVersions().getAccountID(accountId).getServersResource();
        
        return new ApiCaller<Servers>(this) {

            @Override
            Servers call() throws RackspaceCloudClientException {
                return serversResource.listServers(authToken, changesSince, offset, limit);
            }
            
        }.makeCall(serversResource);
    }
    
    @Intercept
    public Servers listServersDetail(final Long changesSince, final Long offset, final Long limit) 
            throws RackspaceCloudClientException {
        
        final ServersResource serversResource = getVersions().getAccountID(accountId).getServersResource();
        
        return new ApiCaller<Servers>(this) {

            @Override
            Servers call() throws RackspaceCloudClientException {
                return serversResource.listServersDetail(authToken, changesSince, offset, limit);
            }
            
        }.makeCall(serversResource);
    }
    
    public Server createServer(final Server server) throws RackspaceCloudClientException {
                
        final ServersResource serversResource = getVersions().getAccountID(accountId).getServersResource();
        
        return new ApiCaller<Server>(this) {

            @Override
            Server call() throws RackspaceCloudClientException {
                return serversResource.createServer(authToken, server);
            }
            
        }.makeCall(serversResource);
    }
    
    public Server getServer(final int serverId) throws RackspaceCloudClientException {
        
        final ServerIDResource serverIDResource = getVersions().getAccountID(accountId)
                                                               .getServersResource()
                                                               .getServerIDResource(serverId);        
        return new ApiCaller<Server>(this) {

            @Override
            Server call() throws RackspaceCloudClientException {
                return serverIDResource.getServer(authToken);
            }
            
        }.makeCall(serverIDResource);        
    }
    
    public void updateServer(final int serverId, final Server newValues) 
            throws RackspaceCloudClientException {
                
        final ServerIDResource serverIDResource = getVersions().getAccountID(accountId)
                                                               .getServersResource()
                                                               .getServerIDResource(serverId);
        
        new ApiCaller<Void>(this) {

            @Override
            Void call() throws RackspaceCloudClientException {
                serverIDResource.updateServer(authToken, newValues);                
                return null;
            }
            
        }.makeCall(serverIDResource);
    }
    
    public void deleteServer(final int serverId) throws RackspaceCloudClientException {
                
        final ServerIDResource serverIDResource = getVersions().getAccountID(accountId)
                                                               .getServersResource()
                                                               .getServerIDResource(serverId);
        
        new ApiCaller<Void>(this) {

            @Override
            Void call() throws RackspaceCloudClientException {                
                serverIDResource.deleteServer(authToken);                
                return null;
            }
            
        }.makeCall(serverIDResource);
    }
    
    /* -------------------------   Server Addresses    ------------------------- */
    
    public Addresses listAddresses(final int serverId, final Long changesSince) 
            throws RackspaceCloudClientException {
        
        final ServerAddresses serverAddresses = getVersions().getAccountID(accountId)
                                                             .getServersResource()
                                                             .getServerIDResource(serverId)
                                                             .getServerAddresses();
        
        return new ApiCaller<Addresses>(this) {

            @Override
            Addresses call() throws RackspaceCloudClientException {
                return serverAddresses.getServerAddresses(authToken, changesSince);                
            }
            
        }.makeCall(serverAddresses);
    }
    
    public AddressList listPublicAddresses(final int serverId, final Long changesSince)
            throws RackspaceCloudClientException {
                
        final ServerAddresses serverAddresses = getVersions().getAccountID(accountId)
                                                             .getServersResource()
                                                             .getServerIDResource(serverId)
                                                             .getServerAddresses();
        
        return new ApiCaller<AddressList>(this) {

            @Override
            AddressList call() throws RackspaceCloudClientException {
                return serverAddresses.getServerPublicAddresses(authToken, changesSince);
            }
            
        }.makeCall(serverAddresses);
    }

    public AddressList listPrivateAddresses(final int serverId, final Long changesSince)
            throws RackspaceCloudClientException {
                
        final ServerAddresses serverAddresses = getVersions().getAccountID(accountId)
                                                             .getServersResource()
                                                             .getServerIDResource(serverId)
                                                             .getServerAddresses();
        
        return new ApiCaller<AddressList>(this) {

            @Override
            AddressList call() throws RackspaceCloudClientException {
                return serverAddresses.getServerPrivateAddresses(authToken, changesSince);
            }
            
        }.makeCall(serverAddresses);
    }
    
    public void shareIP(final int serverId, final String ip, final ShareIp shareIp) 
            throws RackspaceCloudClientException {
                
        final ServerAddresses serverAddresses = getVersions().getAccountID(accountId)
                                                             .getServersResource()
                                                             .getServerIDResource(serverId)
                                                             .getServerAddresses();
        
        new ApiCaller<Void>(this) {

            @Override
            Void call() throws RackspaceCloudClientException {
                serverAddresses.shareIp(ip, authToken, shareIp);                
                return null;
            }
            
        }.makeCall(serverAddresses);
    }

    public void unshareIP(final int serverId, final String ip, final ShareIp shareIp) 
            throws RackspaceCloudClientException {
                
        final ServerAddresses serverAddresses = getVersions().getAccountID(accountId)
                                                             .getServersResource()
                                                             .getServerIDResource(serverId)
                                                             .getServerAddresses();
        
        new ApiCaller<Void>(this) {

            @Override
            Void call() throws RackspaceCloudClientException {
                serverAddresses.unshareIp(ip, authToken);                
                return null;
            }
            
        }.makeCall(serverAddresses);
    }
    
    /* -------------------------    Server Actions     ------------------------- */
    
    public void action(final int serverId, final Reboot reboot) throws RackspaceCloudClientException {
                
        final ServerIDResource serverIDResource = getVersions().getAccountID(accountId)
                                                               .getServersResource()
                                                               .getServerIDResource(serverId);
        
        new ApiCaller<Void>(this) {

            @Override
            Void call() throws RackspaceCloudClientException {
                serverIDResource.actionreboot(authToken, reboot);                
                return null;
            }
            
        }.makeCall(serverIDResource);
    }
    
    public void action(final int serverId, final Rebuild rebuild) throws RackspaceCloudClientException {
                
        final ServerIDResource serverIDResource = getVersions().getAccountID(accountId)
                                                               .getServersResource()
                                                               .getServerIDResource(serverId);
        
        new ApiCaller<Void>(this) {

            @Override
            Void call() throws RackspaceCloudClientException {
                serverIDResource.actionrebuild(authToken, rebuild);                
                return null;
            }
            
        }.makeCall(serverIDResource);
    }
    
    public void action(final int serverId, final Resize resize) throws RackspaceCloudClientException {
                
        final ServerIDResource serverIDResource = getVersions().getAccountID(accountId)
                                                               .getServersResource()
                                                               .getServerIDResource(serverId);
        
        new ApiCaller<Void>(this) {

            @Override
            Void call() throws RackspaceCloudClientException {
                serverIDResource.actionresize(authToken, resize);                
                return null;
            }
            
        }.makeCall(serverIDResource);
    }
    
    public void action(final int serverId, final ConfirmResize confResize) 
            throws RackspaceCloudClientException {
        
        final ServerIDResource serverIDResource = getVersions().getAccountID(accountId)
                                                               .getServersResource()
                                                               .getServerIDResource(serverId);
        
        new ApiCaller<Void>(this) {

            @Override
            Void call() throws RackspaceCloudClientException {
                serverIDResource.actionconfirmResize(authToken, confResize);                
                return null;
            }
            
        }.makeCall(serverIDResource);
    }
    
    public void action(final int serverId, final RevertResize revertResize) 
            throws RackspaceCloudClientException {
        
        final ServerIDResource serverIDResource = getVersions().getAccountID(accountId)
                                                               .getServersResource()
                                                               .getServerIDResource(serverId);
        
        new ApiCaller<Void>(this) {

            @Override
            Void call() throws RackspaceCloudClientException {
                serverIDResource.actionrevertResize(authToken, revertResize);                
                return null;
            }
            
        }.makeCall(serverIDResource);
    }
    
    /* -------------------------        Flavors        ------------------------- */
    
    public Flavors listFlavors(final Long changesSince, final Long offset, final Long limit) 
            throws RackspaceCloudClientException {
        
        final FlavorsResource flavorsResource = getVersions().getAccountID(accountId)
                                                             .getFlavorsResource();
        
        return new ApiCaller<Flavors>(this) {

            @Override
            Flavors call() throws RackspaceCloudClientException {
                return flavorsResource.listFlavors(authToken, changesSince, offset, limit);                
            }
            
        }.makeCall(flavorsResource);
    }
    
    public Flavors listFlavorsDetail(final Long changesSince, final Long offset, final Long limit) 
            throws RackspaceCloudClientException {
        
        final FlavorsResource flavorsResource = getVersions().getAccountID(accountId)
                                                             .getFlavorsResource();
        
        return new ApiCaller<Flavors>(this) {

            @Override
            Flavors call() throws RackspaceCloudClientException {
                return flavorsResource.listFlavorsDetailed(authToken, changesSince, offset, limit);
            }
            
        }.makeCall(flavorsResource);
    }
    
    public Flavor getFlavor(final int flavorId) throws RackspaceCloudClientException {
        
        final FlavorsResource flavorsResource = getVersions().getAccountID(accountId)
                                                             .getFlavorsResource();
        
        return new ApiCaller<Flavor>(this) {

            @Override
            Flavor call() throws RackspaceCloudClientException {
                return flavorsResource.getFlavor(flavorId, authToken);            
            }
            
        }.makeCall(flavorsResource);
    }    
    
    /* -------------------------   Backup Schedules    ------------------------- */
    
    public BackupSchedule getBackupSchedule(final int serverId) throws RackspaceCloudClientException {
        final ServerIDResource serverIDResource = getVersions().getAccountID(accountId)
                                                               .getServersResource()
                                                               .getServerIDResource(serverId);
        
        return new ApiCaller<BackupSchedule>(this) {

            @Override
            BackupSchedule call() throws RackspaceCloudClientException {
                return serverIDResource.getBackupSchedule(authToken);                
            }
            
        }.makeCall(serverIDResource);
    }
    
    public void setBackupSchedule(final int serverId, final BackupSchedule schedule) 
            throws RackspaceCloudClientException {        
        
        final ServerIDResource serverIDResource = getVersions().getAccountID(accountId)
                                                               .getServersResource()
                                                               .getServerIDResource(serverId);
        
        new ApiCaller<Void>(this) {

            @Override
            Void call() throws RackspaceCloudClientException {
                serverIDResource.setBackupSchedule(authToken, schedule);
                return null;
            }
            
        }.makeCall(serverIDResource);
    }
    
    public void disableBackupSchedule(final int serverId) throws RackspaceCloudClientException {
        
        final ServerIDResource serverIDResource = getVersions().getAccountID(accountId)
                                                               .getServersResource()
                                                               .getServerIDResource(serverId);
        
        new ApiCaller<Void>(this) {

            @Override
            Void call() throws RackspaceCloudClientException {
                serverIDResource.disableBackupSchedule(authToken);                
                return null;
            }
            
        }.makeCall(serverIDResource);
    }
    
    /* -------------------------    Shared IP Groups   ------------------------- */
    
    public SharedIpGroups listSharedIpGroups(final Long changesSince, final Long offset, final Long limit) 
            throws RackspaceCloudClientException {
        
        final SharedIpGroupsResource sharedIpGroups = getVersions().getAccountID(accountId)
                                                                   .getSharedIpGroupsResource();
        
        return new ApiCaller<SharedIpGroups>(this) {

            @Override
            SharedIpGroups call() throws RackspaceCloudClientException {
                return sharedIpGroups.listSharedIpGroups(authToken, changesSince, offset, limit);
            }
            
        }.makeCall(sharedIpGroups);
    }
    
    public SharedIpGroups listSharedIpGroupsDetail(final Long changesSince, final Long offset,
            final Long limit) throws RackspaceCloudClientException {
        
        final SharedIpGroupsResource sharedIpGroups = getVersions().getAccountID(accountId)
                                                                   .getSharedIpGroupsResource();
        
        return new ApiCaller<SharedIpGroups>(this) {

            @Override
            SharedIpGroups call() throws RackspaceCloudClientException {
                return sharedIpGroups.listSharedIpGroupsDetailed(authToken, changesSince, offset, limit);
                
            }
            
        }.makeCall(sharedIpGroups);
    }
    
    public SharedIpGroup createSharedIpGroup(final SharedIpGroup sharedIpGroup)
            throws RackspaceCloudClientException {
        
        final SharedIpGroupsResource sharedIpGroups = getVersions().getAccountID(accountId)
                                                                   .getSharedIpGroupsResource();

        return new ApiCaller<SharedIpGroup>(this) {

            @Override
            SharedIpGroup call() throws RackspaceCloudClientException {
                return sharedIpGroups.createSharedIpGroup(authToken, sharedIpGroup);                
            }
            
        }.makeCall(sharedIpGroups);
    }
    
    public SharedIpGroup getSharedIpGroup(final int sharedIpGroupId) throws RackspaceCloudClientException {
        
        final SharedIpGroupsResource sharedIpGroups = getVersions().getAccountID(accountId)
                                                                   .getSharedIpGroupsResource();
        
        return new ApiCaller<SharedIpGroup>(this) {

            @Override
            SharedIpGroup call() throws RackspaceCloudClientException {
                return sharedIpGroups.getSharedIpGroupDetail(sharedIpGroupId, authToken);                
            }
            
        }.makeCall(sharedIpGroups);
    }
    
    public void deleteSharedIpGroup(final int sharedIpGroupId) throws RackspaceCloudClientException {
        
        final SharedIpGroupsResource sharedIpGroups = getVersions().getAccountID(accountId)
                                                                   .getSharedIpGroupsResource();
        
        new ApiCaller<Void>(this) {

            @Override
            Void call() throws RackspaceCloudClientException {
                sharedIpGroups.deleteSharedIpGroup(sharedIpGroupId, authToken);
                return null;
            }
            
        }.makeCall(sharedIpGroups);
    }
    
    /* ------------------------- Client Internal Works ------------------------- */
    
    /**
     * Creates and returns a {@code WebClient} proxy for the root resource of
     * Rackspace Cloud Service. All to sub-resources are then derived from this
     * client.
     * @return {@code Versions} the client proxy for the root resource.
     * @throws RackspaceCloudClientException in case of an error.
     */
    private Versions getVersions() throws RackspaceCloudClientException {
        // Not authenitcated.
        if (authToken == null) {
            authenticate();
        }
        
        long timeTaken = -System.currentTimeMillis();
        
        JAXRSClientFactoryBean clientFactory = getClientFactory();
        
        clientFactory.setAddress(baseUri.toString());
        clientFactory.setServiceClass(Versions.class);
        clientFactory.setInheritHeaders(true);
        
        Versions versions = clientFactory.create(Versions.class);
        
        CLIENT_STORE.set(WebClient.client(versions));
        
        timeTaken += System.currentTimeMillis();
        LOGGER.debug("Created WebClient in [{}] msec.", timeTaken);
        
        return versions;
    }
    
    /**
     * Rackspace Cloud Servers authentication procedure.
     * @throws RackspaceCloudClientException 
     * @see <a href="http://docs.rackspace.com/servers/api/v1.0/cs-devguide/content/index.html">
     * Cloud Servers Developer Guide, section 3.1</a>
     * @see <a href="http://docs.rackspace.com/servers/api/cs-bindguide-latest.pdf">
     * API Language Binding Guide, section "Authentication"</a>
     */
    private synchronized void authenticate() throws RackspaceCloudClientException {
        if (authToken == null) {
            
            LOGGER.info("RackspaceCloudClient authenticating..");
            
            authToken = null;
            Response authRS;
            // TODO: API Language Binding Guide suggests that UnauthorizedFaults should not
            // be raised unless multiple attempts to authenticate fail. This logic have to be
            // implemented below.
            try {
                JAXRSClientFactoryBean clientFactory = getClientFactory();
                clientFactory.setAddress(accountBase.getAuthUrl());
                WebClient authClient = clientFactory.createWebClient()
                                                    .header(Constants.AUTH_USER_HEADER, user)
                                                    .header(Constants.AUTH_KEY_HEADER, authKey);
                           
                authRS = authClient.get();              
                
            } catch (Exception e) {
                String msg = "Failed to authenticate: " + e.getMessage();
                LOGGER.error(msg, e);
                throw new RackspaceCloudClientException(msg, e);
            }
            
            int status = authRS.getStatus();
            
            if (status != HttpURLConnection.HTTP_NO_CONTENT) {
                String msg = "Authentication failed, response status: " + status;
                LOGGER.error(msg);
                throw new RackspaceCloudClientException(msg);
            }
            
            // Got HTTP 204 - (No Content) status code, authentication was successful.
            String mgmtUrl = 
                    authRS.getMetadata().getFirst(Constants.SERVER_MGMT_URL_HEADER).toString();
            // Extract account ID from the management URL.
            String[] split = mgmtUrl.split("/");
            accountId = Long.parseLong(split[split.length - 1]);
            // Extract base URL from the management URL.
            String baseUriStr = mgmtUrl.substring(0, mgmtUrl.lastIndexOf('/'));
            baseUriStr = baseUriStr.substring(0, baseUriStr.lastIndexOf('/'));
            try {
                baseUri = new URI(baseUriStr);
            } catch (URISyntaxException ex) {
                String msg = "Failed to get Rackspace Cloud Servers management URL.";
                LOGGER.error(msg, ex);
                throw new RackspaceCloudClientException(msg, ex);
            }
            authToken = authRS.getMetadata().getFirst(Constants.AUTH_TOKEN_HEADER).toString();
            
            LOGGER.info("RackspaceCloudClient authenticated.");
            LOGGER.info("Account ID is: {}", accountId);
        }
    }
    
    /* -------------------------    Accessor Methods    ------------------------- */

    /**
     * Creates and returns a {@code JAXRSClientFactoryBean} configured as needed based
     * on this client's configuration.
     * @return a new {@code JAXRSClientFactoryBean}.
     */
    private JAXRSClientFactoryBean getClientFactory() {
        
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        
        // Enable customized CXF HTTP Transport for compatibility with GAE.
        if (appEngineCompatible) {
            GAEHttpTransportFactory gaeTransport = new GAEHttpTransportFactory();
            gaeTransport.setBus(bean.getBus());
        } else {
            HTTPTransportFactory httpTransport = new HTTPTransportFactory();
            httpTransport.setBus(bean.getBus());
        }
        
        // Add support for response compression.
        // GAE does not support GZIP (it does it by automaticaly when fetching URLs).
        if (compressionEnabled && !appEngineCompatible) {
            Map<String, String> headers = new HashMap<String, String>();
            headers.put(Constants.ACCEPT_ENCODING_HEADER, "gzip");
            bean.setHeaders(headers);
            bean.getInInterceptors().add(new GZIPInInterceptor());
        }

        // Enable logging interceptors if logging level is DEBUG.
        if (LOGGER.isDebugEnabled()) {
            LoggingInInterceptor loggingIn = new LoggingInInterceptor();
            loggingIn.setPrettyLogging(true);
            LoggingOutInterceptor loggingOut = new LoggingOutInterceptor();
            loggingOut.setPrettyLogging(true);
            bean.getOutInterceptors().add(loggingOut);
            bean.getInInterceptors().add(loggingIn);
        }
        
        return bean;
    }

    /**
     * Gets the GAE compatibility of this client.
     * @return {@code true} if this client is compatible with GAE, false otherwise.
     */
    public boolean isAppEngineCompatible() {
        return appEngineCompatible;
    }

    /**
     * Sets the GAE compatibility of this client. When enabled, this client will only work
     * inside App Engine's environment.
     * @param appEngineCompatible {@code boolean} value, when {@code true} GAE compatibility
     * is enabled.
     */
    public void setAppEngineCompatible(boolean appEngineCompatible) {
        this.appEngineCompatible = appEngineCompatible;
    }    

    /**
     * Gets the GZIP compression setting of this client.
     * @return {@code true} if this client is using GZIP compression.
     */
    public boolean isCompressionEnabled() {
        return compressionEnabled;
    }

    /**
     * Sets the GZIP compression setting of this client. Note that GAE compatible clients
     * will have GZIP enabled by default.
     * @param compressionEnabled {@code boolean} value, when {@code true} this client will
     * request GZIP compression from web service endpoints.
     */
    public void setCompressionEnabled(boolean compressionEnabled) {
        this.compressionEnabled = compressionEnabled;
    }

    @Override
    public Interceptor getInterceptor() {
        return interceptor;
    }

    public void setInterceptor(Interceptor interceptor) {
        this.interceptor = interceptor;
    }
    
    /**
     * Returns the {@link Response} object associated with the last call any method of
     * this client that returned results from Rackspace's API. The call should originate
     * from the same thread that initiated the remote call.<br />
     * For example, if a thread called the {@link #getImage(int) getImage()} method, a
     * subsequent call of {@code getResponse()} from the same thread will return the
     * {@link Response} object from which the result of the previous method was extracted.<br />
     * This allows access to extra information related with the underlying protocol.
     * @return the {@link Response} object as described above, or {@code null} if no
     * such object exists (e.g. when no previous call was made that returned results
     * from Rackspace's API).
     */
    public Response getResponse() {
        Client client = CLIENT_STORE.get();
        
        if (client != null) {
            return client.getResponse();
        }
        
        return null;
    }
    
    /* ------------------------- Static Utility Mehtods ------------------------- */
    
    public static JAXBContext getJAXBContext() {
        return JAXB_CTX;
    }
    
    public static String toXmlString(Object element) throws JAXBException {
        ObjectFactory f = new ObjectFactory();
        element = f.createVersion((Version) element);
        Marshaller marshaller = JAXB_CTX.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
        marshaller.marshal(element, baos);
        return baos.toString();
    }
    
    
    /**
     * Inner helper class that encapsulates the logic required by all the methods
     * of {@code RackspaceCloudClient} involving remote calls to Rackspace Cloud
     * Servers API.<br />
     * A typical use case is demonstrated by the {@link RackspaceCloudClient#listLimits()}
     * method.
     * @param <T> the return type of the API call.
     */
    private abstract static class ApiCaller<T> {
        
        /** Reference to the client that will actually make the remote call. */
        private RackspaceCloudClient client;
        
        /**
         * Constructor.
         * @param client a reference of the client instance that will make the
         * remote call.
         */
        public ApiCaller(RackspaceCloudClient client) {
            this.client = client;
        }
        
        /**
         * The method to be implemented in line within the actual
         * methods of {@code RackspaceCloudClient}.
         * @return
         * @throws RackspaceCloudClientException 
         */
        abstract T call() throws RackspaceCloudClientException;
        
        /**
         * Calls {@link ApiCaller#call()}. The call is wrapped in such a way so that
         * retries are taking place in case of unauthenticated faults. There is also
         * some basic error handling in place.<br />
         * TODO: Improve error handling.
         * @return
         * @throws RackspaceCloudClientException 
         */
        public T makeCall(Object cxfClient) throws RackspaceCloudClientException {
            
            int retries = 0;
            while (retries < MAX_RETRIES) {
                try {
                    RackspaceCloudClient.CLIENT_STORE.set(WebClient.client(cxfClient));
                    return call();
                } catch (ClientWebApplicationException cex) {
                    // Client side error.
                    throw new RackspaceCloudClientException("Client side error.", cex);
                } catch (ServerWebApplicationException wex) {
                    // Server side error. If Unauthenticated, retry.
                    int status = wex.getResponse().getStatus();
                    if (HttpURLConnection.HTTP_UNAUTHORIZED == status) {
                        // Invalidate login. This way the next request will trigger authentication.
                        client.authToken = null;
                        retries++;
                    } else throw new RackspaceCloudClientException("Server side error.", wex);
                }
            }
            // Max retries reached.
            throw new RackspaceCloudClientException("Authentication failed after multiple attempts.");
        }
    }    
    
    /**
     * An inner wrapper class defining constant values used by {@code RackspaceCloudClient}.
     */
    private static class Constants {
        
        public static final String AUTH_USER_HEADER = "X-Auth-User";
        public static final String AUTH_KEY_HEADER = "X-Auth-Key";
        public static final String SERVER_MGMT_URL_HEADER = "X-Server-Management-Url";
        public static final String AUTH_TOKEN_HEADER = "X-Auth-Token";
        public static final String ACCEPT_ENCODING_HEADER = "Accept-Encoding";
        public static final String CONTENT_ENCODING_HEADER = "Content-Encoding";
        
    }
    
}
