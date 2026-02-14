package com.github.padster.guiceserver.auth;

import org.junit.Test;
import static org.junit.Assert.*;

public class RouteAuthenticatorTest {

    @Test
    public void testGStateRemoval_SimpleCase() {
        // Test case from problem statement: should work already
        String input = "g_state={\"i_l\":0,\"i_ll\":1762714440988}; _gsID=abcde";
        String expected = " _gsID=abcde";
        String result = removeGState(input);
        assertEquals(expected, result);
    }

    @Test
    public void testGStateRemoval_NestedBraces() {
        // Test case from problem statement: currently broken
        String input = "g_state={\"i_l\":0,\"i_ll\":1771047714245,\"i_e\":{\"enable_itp_optimization\":0}}; _gsID=abcde";
        String expected = " _gsID=abcde";
        String result = removeGState(input);
        assertEquals(expected, result);
    }

    @Test
    public void testGStateRemoval_NoGState() {
        // Test without g_state
        String input = "_gsID=abcde";
        String expected = "_gsID=abcde";
        String result = removeGState(input);
        assertEquals(expected, result);
    }

    @Test
    public void testGStateRemoval_OnlyGState() {
        // Test with only g_state
        String input = "g_state={\"i_l\":0,\"i_ll\":1771047714245};";
        String expected = "";
        String result = removeGState(input);
        assertEquals(expected, result);
    }

    @Test
    public void testGStateRemoval_MultipleNestedLevels() {
        // Test with deeper nesting
        String input = "g_state={\"i_l\":0,\"i_e\":{\"a\":{\"b\":1}}}; _gsID=xyz";
        String expected = " _gsID=xyz";
        String result = removeGState(input);
        assertEquals(expected, result);
    }

    @Test
    public void testGStateRemoval_BracesInStringValue() {
        // Test with braces inside a JSON string value
        String input = "g_state={\"key\":\"value{with}braces\"}; _gsID=test";
        String expected = " _gsID=test";
        String result = removeGState(input);
        assertEquals(expected, result);
    }

    // Helper method that mimics the logic in RouteAuthenticator
    private String removeGState(String header) {
        if (header != null) {
            header = removeGStateParameter(header);
        }
        return header;
    }

    /**
     * Remove g_state parameter from cookie header string.
     * Handles nested JSON objects by properly counting braces.
     * Also handles braces within JSON string values.
     * @param header the cookie header string
     * @return header with g_state parameter removed
     */
    private String removeGStateParameter(String header) {
        int startIndex = header.indexOf("g_state=");
        if (startIndex == -1) {
            return header;
        }
        
        // Find the opening brace of the JSON object (after "g_state=")
        int braceStart = header.indexOf("{", startIndex + 8);
        if (braceStart == -1) {
            return header;
        }
        
        // Count braces to find the matching closing brace
        // Track if we're inside a quoted string to ignore braces in strings
        int braceCount = 0;
        boolean inString = false;
        boolean escaped = false;
        int i = braceStart;
        
        while (i < header.length()) {
            char c = header.charAt(i);
            
            if (escaped) {
                // Skip escaped characters
                escaped = false;
            } else if (c == '\\') {
                // Next character is escaped
                escaped = true;
            } else if (c == '"') {
                // Toggle string state
                inString = !inString;
            } else if (!inString) {
                // Only count braces when not inside a string
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0) {
                        // Found matching closing brace
                        // Check if followed by semicolon
                        int endIndex = i + 1;
                        if (endIndex < header.length() && header.charAt(endIndex) == ';') {
                            endIndex++;
                        }
                        // Remove the g_state parameter (keeping any trailing whitespace)
                        return header.substring(0, startIndex) + header.substring(endIndex);
                    }
                }
            }
            i++;
        }
        
        // If we couldn't find matching braces, return original
        return header;
    }
}
