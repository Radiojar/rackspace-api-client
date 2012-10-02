package com.pynode.rackspace.client.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Christos Fragoulides
 */
public class InterceptionTestSubject implements Interceptable {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(InterceptionTestSubject.class);
    
    private InterceptionTestSubject thiz = this;
    
    private Interceptor interceptor = new Interceptor() {

        @Override
        public Object intercept(ProceedingJoinPoint pjp) throws Throwable {
            Object result;
            try {
                String arg = (String) pjp.getArgs()[0];
                Integer newArg = Integer.parseInt(arg) * 2;
                result = pjp.proceed(new Object[] {newArg.toString()});
                if (result instanceof Integer) {
                    result = 2 * ((Integer) result);
                }
            } catch (Throwable ex) {
                return null;
            }
            return result;
        }
        
    };
    
    public Integer notIntercepted(int value) {
        // AspectJ should leave this unaffected.
        return value;
    }
    
    @Intercept
    public Integer testMe(String intString) throws Exception {
        return Integer.parseInt(intString);
    }

    @Override
    public Interceptor getInterceptor() {
        return interceptor;
    }
    
}
