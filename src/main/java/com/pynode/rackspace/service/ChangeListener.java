package com.pynode.rackspace.service;

/**
 *
 * @author Christos Fragoulides
 */
public interface ChangeListener<T> {
    void notify(NotifyEvent<T> e);
}
