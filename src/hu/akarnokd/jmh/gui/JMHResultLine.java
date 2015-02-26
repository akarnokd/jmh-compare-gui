package hu.akarnokd.jmh.gui;

import hu.akarnokd.utils.xml.XElement;

import java.util.*;

public class JMHResultLine {
    public String benchmark;
    public final List<String> parameters = new ArrayList<>();
    public double value;
    public double error;
    public void save(XElement out) {
        out.set("benchmark", benchmark);
        out.set("value", value);
        out.set("error", error);
        for (String p : parameters) {
            XElement xp = out.add("parameter");
            xp.set("value", p);
        }
    }
    public void load(XElement in) {
        benchmark = in.get("benchmark", "");
        value = in.getDouble("value", -1d);
        error = in.getDouble("error", -1d);
        for (XElement xp : in.childrenWithName("parameter")) {
            parameters.add(xp.get("value", ""));
        }
    }
}
