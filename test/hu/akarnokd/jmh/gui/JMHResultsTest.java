package hu.akarnokd.jmh.gui;

import static org.junit.Assert.assertEquals;

import java.io.*;
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

}
