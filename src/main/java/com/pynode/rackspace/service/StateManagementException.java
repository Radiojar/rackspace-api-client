package com.pynode.rackspace.service;

/**
 * A {@code RuntimeException} raised by methods of {@link StateManager} and
 * by the methods of the {@link java.util.concurrent.ConcurrentMap} returned 
 * from its {@link StateManager#getState() getState()} method, indicating
 * failure of the underlying mechanism.
 * @author Christos Fragoulides
 */
public class StateManagementException extends RuntimeException {

    public StateManagementException(Throwable cause) {
        super(cause);
    }

    public StateManagementException(String message, Throwable cause) {
        super(message, cause);
    }

    public StateManagementException(String message) {
        super(message);
    }

    public StateManagementException() { }
    
}
