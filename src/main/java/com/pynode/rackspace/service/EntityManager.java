package com.pynode.rackspace.service;

import com.pynode.rackspace.client.RackspaceCloudClientException;

/**
 *
 * @author Christos Fragoulides
 */
public interface EntityManager<T> {
    
    long MAX_LIMIT = 1000;
    
    /* -------------   CRUD Operations  -------------*/
    T create(T e);
    
    void remove(T e);
    
    void update(T e);
    
    T refresh(T e);
    
    T find(long id);
    
    /* ------------- Polling Operations -------------*/
    void wait(T e);
    
    void wait(T e, long timeout);
    
    void notify(T e, ChangeListener<T> ch);
    
    void stopNotify(T e, ChangeListener<T> ch);
    
    /* -------------        Lists       -------------*/
    
    /**
     * Creates an auto paginating entity list.
     * @param detail
     * @return
     * @throws RackspaceCloudClientException 
     */
    EntityList<T> createList(boolean detail) throws RackspaceCloudClientException;
    
    /**
     * Creates a partial entity list(one that will not perform automatic pagination), containing
     * the elements that fall into the range specified by the {@code offset} and {@code limit} 
     * parameters.
     * @param detail
     * @param offset
     * @param limit
     * @return
     * @throws RackspaceCloudClientException 
     */
    EntityList<T> createList(boolean detail, long offset, long limit) throws RackspaceCloudClientException;
    
    EntityList<T> createDeltaList(boolean detail, long changesSince) throws RackspaceCloudClientException;
    
    EntityList<T> createDeltaList(boolean detail, long changesSince, long offset, long limit)
             throws RackspaceCloudClientException;;
}
