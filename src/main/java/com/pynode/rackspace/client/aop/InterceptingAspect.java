package com.pynode.rackspace.client.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

/**
 * Aspect to be used for intercepting method calls.
 * @author Christos Fragoulides
 */
@Aspect
public class InterceptingAspect {
    
    @Pointcut("execution(* *(..)) && this(Interceptable) && @annotation(Intercept)")
    void intercepted() { }
    
    @Around("intercepted()")
    public Object intercept(ProceedingJoinPoint pjp) throws Throwable {
        
        Interceptor interceptor = null;
        
        try {
            interceptor = ((Interceptable) pjp.getThis()).getInterceptor();
        } catch (Throwable t) {
            interceptor = null;
        }
        
        if (interceptor == null) return pjp.proceed();
        
        return interceptor.intercept(pjp);
    }
    
}
