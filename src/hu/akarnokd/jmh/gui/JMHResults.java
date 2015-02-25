package hu.akarnokd.jmh.gui;

import hu.akarnokd.utils.lang.StringUtils;

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
    public static final int IO_ERROR = -4;
    
    public int parse(String results) {
        try (BufferedReader in = new BufferedReader(new StringReader(results))) {
            String line = in.readLine();
            if (line == null || line.isEmpty()) {
                return EMPTY;
            }
            if (!line.startsWith("Benchmark")) {
                System.err.println(line);
                return NO_BENCHMARK;
            }
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
            
            while ((line = in.readLine()) != null) {
                line = line.replaceAll("\\s{2,}", " ");
                
                List<String> columns = StringUtils.split(line, " ");
                if (columns.size() != 6 + parameterNames.size()) {
                    System.err.println(line);
                    return ROW_FORMAT;
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
                    rl.value = Double.parseDouble(error);
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
}
