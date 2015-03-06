package hu.akarnokd.jmh.gui;

import hu.akarnokd.utils.lang.StringUtils;
import hu.akarnokd.utils.xml.XElement;

import java.io.*;
import java.util.*;

public class JMHResults {
    public String name;
    public final List<String> parameterNames = new ArrayList<>();
    public final List<JMHResultLine> lines = new ArrayList<>();
    
    public static final int OK = 0;
    public static final int EMPTY = -1;
    public static final int NO_BENCHMARK = -2;
    public static final int ROW_FORMAT = -3;
    public static final int NUMBER_FORMAT = -4;
    public static final int IO_ERROR = -5;
    
    public int parse(String results) {
        try (BufferedReader in = new BufferedReader(new StringReader(results))) {
            // locate the first line saying Benchmark
            String line;
            
            while ((line = in.readLine()) != null) {
                line = cleanup(line);
                if (line.startsWith("Benchmark")) {
                    break;
                }
            }
            // we did not find the header
            if (line == null) {
                return NO_BENCHMARK;
            }
            // parse header
            int idx = line.indexOf("(");
            while (idx >= 0) {
                int idx2 = line.indexOf(")", idx + 1);
                if (idx2 < 0) {
                    System.err.println(line);
                    return ROW_FORMAT;
                }
                parameterNames.add(line.substring(idx + 1, idx2));
                idx = line.indexOf("(", idx2 + 1);
            }

            // benchmark, mode, count, score, error + number of parameters, we ignore any further columns
            int expectedColumns = 5 + parameterNames.size();

            while ((line = in.readLine()) != null) {
                line = cleanup(line).replaceAll("\\s{2,}", " ");
                List<String> columns = StringUtils.split(line, " ");
                // skip empty lines
                if (columns.isEmpty()) {
                    continue;
                }
                int size = columns.size();
                if (size < expectedColumns) {
                    System.err.println(line);
                    return ROW_FORMAT;
                } else
                if (size > expectedColumns) {
                    // we probably have an extra symbol between score and error
                    String toCheck = columns.get(expectedColumns - 1).replace(',', '.');
                    try {
                        Double.parseDouble(toCheck);
                        // its a value, let's assume it is the error
                    } catch (NumberFormatException ex) {
                        // just ignore it
                        columns.remove(expectedColumns - 1);
                    }
                }
                
                JMHResultLine rl = new JMHResultLine();
                rl.benchmark = columns.get(0);
                for (int i = 1; i <= parameterNames.size(); i++) {
                    rl.parameters.add(columns.get(i));
                }
                String score = columns.get(3 + parameterNames.size());
                score = score.replace(',', '.');
                try {
                    rl.value = Double.parseDouble(score);
                } catch (NumberFormatException ex) {
                    System.err.println(score);
                    return NUMBER_FORMAT; 
                }
                String error = columns.get(4 + parameterNames.size());
                error = error.replace(',', '.');
                try {
                    rl.error = Double.parseDouble(error);
                } catch (NumberFormatException ex) {
                    System.err.println(error);
                    return NUMBER_FORMAT; 
                }
                
                lines.add(rl);
            }
            
            return OK;
        } catch (IOException ex) {
            ex.printStackTrace();
            return IO_ERROR;
        }
    }
    /**
     * Remove any leading space, star, slash-slash, semicolon or hashmark as these
     * are common parts of the comment.
     * @param s the string to cleanup
     * @return the cleaned up string
     */
    static String cleanup(String s) {
        for (;;) {
            s = s.trim();
            if (s.startsWith("*") || s.startsWith(";") || s.startsWith("/") || s.startsWith("#")) {
                s = s.substring(1);
            } else {
                return s;
            }
        }
    }
    
    public static String example() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println("Benchmark                                 (size)   Mode   Samples        Score  Score error    Units");
        pw.println("r.o.OperatorMapPerf.mapPassThru                1  thrpt         5 19543238,471   305242,943    ops/s");
        pw.println("r.o.OperatorMapPerf.mapPassThru             1000  thrpt         5   101728,439     8456,200    ops/s");
        pw.println("r.o.OperatorMapPerf.mapPassThru          1000000  thrpt         5       93,412        4,454    ops/s");
        pw.println("r.o.OperatorMapPerf.mapPassThruViaLift         1  thrpt         5 21308546,094   365190,879    ops/s");
        pw.println("r.o.OperatorMapPerf.mapPassThruViaLift      1000  thrpt         5   101707,671     9011,277    ops/s");
        pw.println("r.o.OperatorMapPerf.mapPassThruViaLift   1000000  thrpt         5       84,032       38,481    ops/s");

        return sw.toString();
    }
    public void save(XElement out) {
        out.set("name", name);
        for (String pn : parameterNames) {
            XElement xpn = out.add("parameter-name");
            xpn.set("value", pn);
        }
        for (JMHResultLine rl : lines) {
            XElement xrl = out.add("result-line");
            rl.save(xrl);
        }
    }
    public void load(XElement in) {
        name = in.get("name", "");
        for (XElement xpn : in.childrenWithName("parameter-name")) {
            parameterNames.add(xpn.get("value", ""));
        }
        for (XElement xrl : in.childrenWithName("result-line")) {
            JMHResultLine rl = new JMHResultLine();
            rl.load(xrl);
            lines.add(rl);
        }
    }
}
