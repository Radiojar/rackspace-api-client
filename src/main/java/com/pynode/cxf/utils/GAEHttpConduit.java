package com.pynode.cxf.utils;

import com.google.appengine.api.urlfetch.FetchOptions;
import com.google.appengine.api.urlfetch.HTTPHeader;
import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.HttpHeaderHelper;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.AbstractConduit;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPException;
import org.apache.cxf.transport.http.Headers;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

/**
 * The CXF's {@code HTTPConduit} uses the JDK's {@code HttpURLConnection} to perform
 * its tasks. In GAE, this is not going to work since the {@code HttpURLConnection}s are
 * based on URLFetchService and do not provide all of the required functionality.
 * This class is a workaround for the problem.
 * @author Christos Fragoulides
 */
class GAEHttpConduit extends AbstractConduit {
    
    private static final Logger LOGGER = LogUtils.getL7dLogger(GAEHttpConduit.class);
    
    private URLFetchService fetchService = URLFetchServiceFactory.getURLFetchService();
    
    private EndpointInfo endpointInfo;
    
    /**
     * This field holds the "default" URL for this particular conduit, which
     * is created on demand.
     */
    private URL defaultEndpointURL;
    private String defaultEndpointURLString;
    private boolean fromEndpointReferenceType;    
    
    public GAEHttpConduit(Bus bus, EndpointInfo endpointInfo, EndpointReferenceType target) 
            throws IOException {
        
        super(getTargetReference(endpointInfo, target, bus));
        this.endpointInfo = endpointInfo;
        
        if (target != null) {
            fromEndpointReferenceType = true;
        }
    }    

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    public void prepare(Message message) throws IOException {
        
        // This call can possibly change the conduit endpoint address and 
        // protocol from the default set in EndpointInfo that is associated
        // with the Conduit.
        URL url = setupURL(message);
        
        // If the HTTP_REQUEST_METHOD is not set, the default is "POST".
        HTTPMethod method;
        try {
            method = HTTPMethod.valueOf((String) message.get(Message.HTTP_REQUEST_METHOD));
        } catch (Exception e) {
            method = HTTPMethod.POST;
        }
        
        // Create the request.
        FetchOptions options = FetchOptions.Builder.withDefaults();
        HTTPRequest req = new HTTPRequest(url, method, options);
        
        // Handle Headers.
        Map<String, List<String>> headers = Headers.getSetProtocolHeaders(message);
        for (String key : headers.keySet()) {
            
            StringBuilder value = new StringBuilder();
            Iterator<String> valueIter = headers.get(key).iterator();
            while (valueIter.hasNext()) {
                value.append(valueIter.next());
                if (valueIter.hasNext()) value.append(", ");
            }
            
            req.addHeader(new HTTPHeader(key, value.toString()));
        }
        
        OutputStream out = new WrappedOutputStream(message, req);
        message.setContent(OutputStream.class, out);
        
        
        
    }
        
    /**
     * This function sets up a URL based on ENDPOINT_ADDRESS, PATH_INFO,
     * and QUERY_STRING properties in the Message. The QUERY_STRING gets
     * added with a "?" after the PATH_INFO. If the ENDPOINT_ADDRESS is not
     * set on the Message, the endpoint address is taken from the 
     * "defaultEndpointURL".
     * <p>
     * The PATH_INFO is only added to the endpoint address string should 
     * the PATH_INFO not equal the end of the endpoint address string.
     * 
     * @param message The message holds the addressing information.
     * 
     * @return The full URL specifying the HTTP request to the endpoint.
     * 
     * @throws MalformedURLException
     */
    private URL setupURL(Message message) throws MalformedURLException {
        String result = (String) message.get(Message.ENDPOINT_ADDRESS);
        String pathInfo = (String) message.get(Message.PATH_INFO);
        String queryString = (String) message.get(Message.QUERY_STRING);
        if (result == null) {
            if (pathInfo == null && queryString == null) {
                URL url = getURL();
                message.put(Message.ENDPOINT_ADDRESS, defaultEndpointURLString);
                return url;
            }
            result = getURL().toString();
            message.put(Message.ENDPOINT_ADDRESS, result);
        }
        
        // REVISIT: is this really correct?
        if (null != pathInfo && !result.endsWith(pathInfo)) { 
            result = result + pathInfo;
        }
        if (queryString != null) {
            result = result + "?" + queryString;
        }        
        return new URL(result);    
    }    
    
    /**
     * @return the default target URL
     */
    protected URL getURL() throws MalformedURLException {
        return getURL(true);
    }

    /**
     * @param createOnDemand create URL on-demand if null
     * @return the default target URL
     */
    protected synchronized URL getURL(boolean createOnDemand)
        throws MalformedURLException {
        if (defaultEndpointURL == null && createOnDemand) {
            if (fromEndpointReferenceType && getTarget().getAddress().getValue() != null) {
                defaultEndpointURL = new URL(this.getTarget().getAddress().getValue());
                defaultEndpointURLString = defaultEndpointURL.toExternalForm();
                return defaultEndpointURL;
            }
            if (endpointInfo.getAddress() == null) {
                throw new MalformedURLException("Invalid address. Endpoint address cannot be null.");
            }
            defaultEndpointURL = new URL(endpointInfo.getAddress());
            defaultEndpointURLString = defaultEndpointURL.toExternalForm();
        }
        return defaultEndpointURL;
    }
    
    protected class WrappedOutputStream extends OutputStream {
        
        private Message outMessage;
        private HTTPRequest request;
        private ByteArrayOutputStream out;

        public WrappedOutputStream(Message message, HTTPRequest request) {
            outMessage = message;
            this.request = request;
            out = new ByteArrayOutputStream();
        }

        @Override
        public void write(int b) throws IOException {
            out.write(b);
        }

        /**
         * Time to fetch the URL and handle the response.
         * @throws IOException 
         */
        @Override
        public void close() throws IOException {
            request.setPayload(out.toByteArray());
            HTTPResponse response = fetchService.fetch(request);
            handleResponse(response);
        }

        private void handleResponse(HTTPResponse response) throws IOException {
            
            Exchange exchange = outMessage.getExchange();
            int responseCode = response.getResponseCode();
            if (outMessage != null && exchange != null) {
                exchange.put(Message.RESPONSE_CODE, responseCode);
            }
            
            // This property should be set in case the exceptions should not be handled here
            // For example jax rs uses this
            boolean noExceptions = MessageUtils.isTrue(outMessage.getContextualProperty(
                "org.apache.cxf.http.no_io_exceptions"));
            if (responseCode >= 400 && responseCode != 500 && !noExceptions) {
                throw new HTTPException(responseCode, null, response.getFinalUrl());
            }
            
            Message inMessage = new MessageImpl();
            inMessage.setExchange(exchange);
            
            // Handle headers.
            Map<String, List<String>> headers = Headers.getSetProtocolHeaders(inMessage);
            String ct = null;
            for (HTTPHeader header : response.getHeaders()) {
                String key = header.getName();
                List<String> value = Arrays.asList(header.getValue().split(","));
                headers.put(key, value);
                if ("content-type".equals(key.toLowerCase())) {
                    ct = value.isEmpty() ? null : value.get(0);
                }
            }
            
            inMessage.put(Message.RESPONSE_CODE, responseCode);
            inMessage.put(Message.CONTENT_TYPE, ct);
            
            String charset = HttpHeaderHelper.findCharset(ct);
            String normalizedEncoding = HttpHeaderHelper.mapCharset(charset);
            if (normalizedEncoding == null) {
                String m = new org.apache.cxf.common.i18n.Message("INVALID_ENCODING_MSG",
                        LOGGER, charset).toString();
                LOGGER.log(Level.WARNING, m);
                throw new IOException(m);
            }
            inMessage.put(Message.ENCODING, normalizedEncoding);
            
            byte[] content = response.getContent();
            InputStream in = new ByteArrayInputStream(content != null ? content : new byte[] {});
            inMessage.setContent(InputStream.class, in);

            DummyHttpURLConnection conn = new DummyHttpURLConnection(request.getURL(), in, headers);
            outMessage.put(HTTPConduit.KEY_HTTP_CONNECTION, conn);
            
            incomingObserver.onMessage(inMessage);
        }
        
    }
    
    /**
     * WebClient (actually AbstractClient) expects an HttpURLConnection to be set
     * as a property of the message by HTTPConduit. We have to add an appropriate
     * instance to let the code flow as usually.
     */
    protected class DummyHttpURLConnection extends HttpURLConnection {
        
        private InputStream in;
        private Map<String, List<String>> headers;
        
        public DummyHttpURLConnection(URL u, InputStream out, Map<String, List<String>> headers) {
            super(u);
            this.in = out;
            this.headers = headers;
        }

        @Override
        public void disconnect() { }

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public void connect() throws IOException { }

        @Override
        public InputStream getErrorStream() {
            return in;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return in;
        }

        @Override
        public Map<String, List<String>> getHeaderFields() {
            return headers;
        }
        
        
    }
    
}
