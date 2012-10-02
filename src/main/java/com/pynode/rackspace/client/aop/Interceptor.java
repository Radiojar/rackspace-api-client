package com.pynode.rackspace.client.aop;

//import com.pynode.rackspace.client.aop.InterceptingAspect.InterceptionJoinPoint;

import org.aspectj.lang.ProceedingJoinPoint;


/**
 *
 * @author Christos Fragoulides
 */
public interface Interceptor {
    
    Object intercept(ProceedingJoinPoint pjp) throws Throwable;
    
}
