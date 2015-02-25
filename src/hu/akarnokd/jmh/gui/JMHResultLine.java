package hu.akarnokd.jmh.gui;

import java.util.*;

public class JMHResultLine {
    public String benchmark;
    public final List<String> parameters = new ArrayList<>();
    public double value;
    public double error;
}
