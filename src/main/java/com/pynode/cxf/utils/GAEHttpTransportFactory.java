package com.pynode.cxf.utils;

import java.io.IOException;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

/**
 * A modified version of CXF's HTTPTransportFactory, that will use {@code GAEHttpConduit}
 * instead of CXF's {@code HTTPConduit}. This will allow CXF to run on GAE.
 * @author Christos Fragoulides
 */
public class GAEHttpTransportFactory extends HTTPTransportFactory {

    @Override
    public Conduit getConduit(EndpointInfo endpointInfo) throws IOException {
        return getConduit(endpointInfo, endpointInfo.getTarget());
    }

    @Override
    public Conduit getConduit(EndpointInfo endpointInfo, EndpointReferenceType target) throws IOException {
        GAEHttpConduit conduit = new GAEHttpConduit(bus, endpointInfo, target);
        return conduit;
    }    
    
}
