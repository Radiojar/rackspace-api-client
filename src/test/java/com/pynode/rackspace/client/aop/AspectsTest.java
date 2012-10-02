package com.pynode.rackspace.client.aop;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Christos Fragoulides
 */
public class AspectsTest {
    
    public AspectsTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Test
    public void testInterception() throws Exception {
        
        InterceptionTestSubject test = new InterceptionTestSubject();
                
        Integer input = 15;
        int result = test.testMe(input.toString());
        
        assertEquals(4 * input, result);
        assertEquals(input, test.notIntercepted(input));
        
        // This should not throw an exception.
        test.testMe("asd4");
    }
}
