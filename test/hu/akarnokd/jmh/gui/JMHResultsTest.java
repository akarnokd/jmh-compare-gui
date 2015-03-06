package hu.akarnokd.jmh.gui;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;

public class JMHResultsTest {

    @Test
    public void testParse() throws IOException {
        JMHResults r = new JMHResults();

        assertEquals(0, r.parse(JMHResults.example()));
        
        assertEquals(Arrays.asList("size"), r.parameterNames);
        assertEquals(6, r.lines.size());
    }

    @Test
    public void testLeadingSpaces() {
        String spaces = "   Benchmark (size) Mode Cnt Score Error    \r\n   a 1 mode 1 1.0 1.0\r\n";
        JMHResults r = new JMHResults();
        assertEquals(0, r.parse(spaces));

        assertEquals(Arrays.asList("size"), r.parameterNames);
        assertEquals(1, r.lines.size());
    }
    @Test
    public void testLeadingComments() {
        for (String s : new String[] { "//", "/*", "/**", "#", "*", ";" }) {
            String spaces = "  " + s + "   Benchmark (size) Mode Cnt Score Error    \r\n   a 1 mode 1 1.0 1.0\r\n";
            JMHResults r = new JMHResults();
            assertEquals("Failed with prefix: " + s, 0, r.parse(spaces));
    
            assertEquals(Arrays.asList("size"), r.parameterNames);
            assertEquals(1, r.lines.size());
        }
    }
    @Test
    public void testExtraSymbolsBetweenScoreAndError() {
        for (String s : new String[] { "\u00b1", "plus/minus", "" }) {
            String spaces = "   Benchmark (size) Mode Cnt Score Error    \r\n   a 1 mode 1 1.0 " + s + " 1.0\r\n";
            JMHResults r = new JMHResults();
            assertEquals("Failed with symbol: " + s, 0, r.parse(spaces));
    
            assertEquals(Arrays.asList("size"), r.parameterNames);
            assertEquals(1, r.lines.size());
            assertEquals(1.0, r.lines.get(0).error, 0.00001);
        }
    }
    @Test
    public void testEmptyRows() {
        String spaces = "\r\n   Benchmark (size) Mode Cnt Score Error    \r\n\r\n   a 1 mode 1 1.0 1.0\r\n\r\n";
        JMHResults r = new JMHResults();
        assertEquals(0, r.parse(spaces));

        assertEquals(Arrays.asList("size"), r.parameterNames);
        assertEquals(1, r.lines.size());
    }
    @Test
    public void testHeaderComesLater() {
        for (String s : new String[] { "\r\n", "     * \r\n"}) {
            String spaces = "    /*\r\nThis benchmark blah blah blah.\r\n     * Benchmark (size) Mode Cnt Score Error    \r\n" + s + "\r\n     * a 1 mode 1 1.0 1.0\r\n     */\r\n";
            JMHResults r = new JMHResults();
            assertEquals(0, r.parse(spaces));
    
            assertEquals(Arrays.asList("size"), r.parameterNames);
            assertEquals(1, r.lines.size());
        }
    }
}
