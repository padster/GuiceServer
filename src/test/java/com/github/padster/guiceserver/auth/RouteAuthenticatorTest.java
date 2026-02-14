package com.github.padster.guiceserver.auth;

import org.junit.Test;
import static org.junit.Assert.*;

public class RouteAuthenticatorTest {

    private final RouteAuthenticator authenticator = new RouteAuthenticator(null, null);

    @Test
    public void testGStateRemoval_SimpleCase() {
        // Test case from problem statement: should work already
        String input = "g_state={\"i_l\":0,\"i_ll\":1762714440988}; _gsID=abcde";
        String expected = " _gsID=abcde";
        String result = authenticator.removeGStateParameter(input);
        assertEquals(expected, result);
    }

    @Test
    public void testGStateRemoval_NestedBraces() {
        // Test case from problem statement: currently broken
        String input = "g_state={\"i_l\":0,\"i_ll\":1771047714245,\"i_e\":{\"enable_itp_optimization\":0}}; _gsID=abcde";
        String expected = " _gsID=abcde";
        String result = authenticator.removeGStateParameter(input);
        assertEquals(expected, result);
    }

    @Test
    public void testGStateRemoval_NoGState() {
        // Test without g_state
        String input = "_gsID=abcde";
        String expected = "_gsID=abcde";
        String result = authenticator.removeGStateParameter(input);
        assertEquals(expected, result);
    }

    @Test
    public void testGStateRemoval_OnlyGState() {
        // Test with only g_state
        String input = "g_state={\"i_l\":0,\"i_ll\":1771047714245};";
        String expected = "";
        String result = authenticator.removeGStateParameter(input);
        assertEquals(expected, result);
    }

    @Test
    public void testGStateRemoval_MultipleNestedLevels() {
        // Test with deeper nesting
        String input = "g_state={\"i_l\":0,\"i_e\":{\"a\":{\"b\":1}}}; _gsID=xyz";
        String expected = " _gsID=xyz";
        String result = authenticator.removeGStateParameter(input);
        assertEquals(expected, result);
    }

    @Test
    public void testGStateRemoval_BracesInStringValue() {
        // Test with braces inside a JSON string value
        String input = "g_state={\"key\":\"value{with}braces\"}; _gsID=test";
        String expected = " _gsID=test";
        String result = authenticator.removeGStateParameter(input);
        assertEquals(expected, result);
    }
}
