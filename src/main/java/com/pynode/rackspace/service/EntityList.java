package com.pynode.rackspace.service;

import com.pynode.rackspace.client.RackspaceCloudClientException;
import java.util.Iterator;

/**
 *
 * @author Christos Fragoulides
 */
public abstract class EntityList<T> implements Iterator<T> {
    
    public abstract long getLastModified();
    
    public abstract boolean isEmpty() throws RackspaceCloudClientException;
    
    public abstract void reset() throws RackspaceCloudClientException;
    
    public abstract void delta() throws RackspaceCloudClientException;

    @Override
    public abstract boolean hasNext();

    @Override
    public abstract T next();

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Remove operation not supported by Entity Lists.");
    }
    
    
}
