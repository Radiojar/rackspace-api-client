package com.pynode.rackspace.service;

import com.rackspace.cloud.api.CloudServersAPIFault;

/**
 *
 * @author Christos Fragoulides
 */
public class NotifyEvent<T> {

    private boolean error;
    private T targetEntity;
    private CloudServersAPIFault fault;
    
    public NotifyEvent(boolean error, T targetEntity, CloudServersAPIFault fault) {
        this.error = error;
        this.targetEntity = targetEntity;
        this.fault = fault;
    }

    public boolean isError() {
        return error;
    }

    public CloudServersAPIFault getFault() {
        return fault;
    }

    public T getTargetEntity() {
        return targetEntity;
    }
    
}
