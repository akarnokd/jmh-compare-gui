package hu.akarnokd.jmh.gui;

import hu.akarnokd.jmh.gui.ComparisonTab.JMHRowModel;
import hu.akarnokd.utils.lang.StringUtils;
import hu.akarnokd.utils.xml.XElement;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URI;
import java.util.function.Consumer;

import javax.swing.*;
import javax.xml.stream.XMLStreamException;

import say.swing.JFontChooser;

public class JMHCompareGUI extends JFrame {
    private final String VERSION = "1.2.0";
    /** */
    private static final long serialVersionUID = -4168653287697309256L;
    private JTabbedPane tabs;
    final File configFile = new File("./jmh-compare-gui-config.xml");
    Font tableFont;
    int cellPadding;
    String csvSeparator;
    boolean localeDecimalSeparator;
    File workdir = new File(".");
    final DiffConfig diff = new DiffConfig();

    void init() {
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveConfig();
                quit();
            }
        });
        setSize(1024, 720);
        setLocationRelativeTo(null);
        setTitle("JMH Results Comparison (" + VERSION + ")");
        
        tabs = new JTabbedPane();
        Container c = getContentPane();
        c.setLayout(new BorderLayout());
        c.add(tabs, BorderLayout.CENTER);
        
        tableFont = tabs.getFont().deriveFont(Font.PLAIN);

        initMenu();
        
        if (!initConfig()) {
            ComparisonTab ct = new ComparisonTab(tabs, diff);
            tabs.addTab("New tab", ct);
            tabs.addTab("+", new JLabel());
        }
        tabs.addChangeListener(cl -> {
            int idx = tabs.getSelectedIndex();
            if (idx >= 0) {
                Component tc = tabs.getComponentAt(idx);
                if (tc instanceof JLabel) {
                    ComparisonTab ct = new ComparisonTab(tabs, diff);
                    ct.setTableFont(tableFont);
                    ct.setPadding(cellPadding);
                    tabs.setComponentAt(idx, ct);
                    tabs.setTitleAt(idx, "New tab");
                    tabs.addTab("+", new JLabel());
                }
            }
        });
    }
    
    void initMenu() {
        JMenuBar menubar = new JMenuBar();
        setJMenuBar(menubar);
        
        JMenu mnuFile = new JMenu("File");
        menubar.add(mnuFile);
        
        JMenuItem mnuNewTab = new JMenuItem("New tab");
        mnuFile.add(mnuNewTab);
        mnuFile.addSeparator();
        
        JMenuItem mnuOpenTab = new JMenuItem("Open tab...");
        mnuFile.add(mnuOpenTab);

        JMenuItem mnuOpenWorkspace = new JMenuItem("Open workspace...");
        mnuFile.add(mnuOpenWorkspace);

        mnuFile.addSeparator();

        JMenuItem mnuCloseTab = new JMenuItem("Close tab");
        mnuFile.add(mnuCloseTab);
        JMenuItem mnuCloseAllTabs = new JMenuItem("Close all tabs");
        mnuFile.add(mnuCloseAllTabs);

        mnuFile.addSeparator();
        
        JMenuItem mnuSaveTab = new JMenuItem("Save tab...");
        mnuFile.add(mnuSaveTab);
        JMenuItem mnuSaveWorkspace = new JMenuItem("Save workspace...");
        mnuFile.add(mnuSaveWorkspace);
        
        mnuFile.addSeparator();
        
        JMenuItem mnuExportSettings = new JMenuItem("Export settings...");
        mnuFile.add(mnuExportSettings);
        
        JMenuItem mnuExportCSV = new JMenuItem("Export into CSV (text)...");
        mnuFile.add(mnuExportCSV);
        
        JMenuItem mnuExportXLS = new JMenuItem("Export into XLS (html)...");
        mnuFile.add(mnuExportXLS);

        mnuFile.addSeparator();

        JMenuItem mnuExit = new JMenuItem("Exit");
        mnuFile.add(mnuExit);

        JMenu mnuView = new JMenu("View");
        menubar.add(mnuView);
        
        JMenuItem mnuFont = new JMenuItem("Font...");
        mnuView.add(mnuFont);

        JMenuItem mnuCellPadding = new JMenuItem("Cell padding...");
        mnuView.add(mnuCellPadding);
        
        mnuView.addSeparator();
        
        JMenuItem mnuSmallDiff = new JMenuItem("Small diff percentage...");
        mnuView.add(mnuSmallDiff);
        
        JMenuItem mnuLargeDiff = new JMenuItem("Large diff percentage...");
        mnuView.add(mnuLargeDiff);

        mnuView.addSeparator();
        
        JMenuItem mnuSmallPlus = new JMenuItem("Small plus color...");
        mnuView.add(mnuSmallPlus);
        JMenuItem mnuLargePlus = new JMenuItem("Large plus color...");
        mnuView.add(mnuLargePlus);
        JMenuItem mnuSmallMinus = new JMenuItem("Small minus color...");
        mnuView.add(mnuSmallMinus);
        JMenuItem mnuLargeMinus = new JMenuItem("Large minus color...");
        mnuView.add(mnuLargeMinus);

        mnuView.addSeparator();

        JMenuItem mnuResetDiff = new JMenuItem("Reset percentages");
        mnuView.add(mnuResetDiff);
        JMenuItem mnuResetColors = new JMenuItem("Reset colors");
        mnuView.add(mnuResetColors);

        JMenu mnuHelp = new JMenu("Help");
        menubar.add(mnuHelp);
        
        JMenuItem mnuHomepage = new JMenuItem("Homepage...");
        mnuHelp.add(mnuHomepage);
        
        // ------------------------------------------
        
        mnuFont.addActionListener(al -> doChangeFont());
        
        mnuCellPadding.addActionListener(al -> doCellPadding());
        
        mnuHomepage.addActionListener(al -> doHomepage());
        
        mnuNewTab.addActionListener(al -> doNewTab());
        
        mnuCloseAllTabs.addActionListener(al -> doCloseAllTabs());
        
        mnuCloseTab.addActionListener(al -> doCloseTab());
        
        mnuOpenTab.addActionListener(al -> doOpenTab(false, false));
        
        mnuOpenWorkspace.addActionListener(al -> doOpenWorkspace());
        
        mnuSaveTab.addActionListener(al -> doSaveTab());
        
        mnuSaveWorkspace.addActionListener(al -> doSaveWorkspace());
        
        mnuExit.addActionListener(al -> quit());
        
        mnuSmallDiff.addActionListener(al -> doSmallDiff());

        mnuLargeDiff.addActionListener(al -> doLargeDiff());
        
        mnuSmallPlus.addActionListener(al -> doChangeColor(diff.smallPlus, "Small plus color", rgb -> diff.smallPlus = rgb));
        mnuSmallMinus.addActionListener(al -> doChangeColor(diff.smallMinus, "Small minus color", rgb -> diff.smallMinus = rgb));
        mnuLargePlus.addActionListener(al -> doChangeColor(diff.largePlus, "Large plus color", rgb -> diff.largePlus = rgb));
        mnuLargeMinus.addActionListener(al -> doChangeColor(diff.largeMinus, "Large minus color", rgb -> diff.largeMinus = rgb));
        
        mnuResetColors.addActionListener(al -> doResetColors());
        mnuResetDiff.addActionListener(al -> doResetDiff());
        
        mnuExportCSV.addActionListener(al -> doExportCSV());
        mnuExportXLS.addActionListener(al -> doExportXLS());
        
        mnuExportSettings.addActionListener(al -> doExportSettings());
    }
    
    void doResetColors() {
        DiffConfig def = new DiffConfig();
        diff.smallPlus = def.smallPlus;
        diff.smallMinus = def.smallMinus;
        diff.largePlus = def.largePlus;
        diff.largeMinus = def.largeMinus;
        repaint();
    }
    void doResetDiff() {
        repaint();
    }
    
    void doChangeColor(Color current, String name, Consumer<Color> newColor) {
        Color c = JColorChooser.showDialog(this, name, current);
        if (c != null) {
            newColor.accept(c);
            repaint();
        }
    }
    
    void doSmallDiff() {
        String value = JOptionPane.showInputDialog(this, "Small diff percentage (float)", "" + diff.smallDiff);
        if (value != null && !value.isEmpty()) {
            value = value.replace(',', '.');
            diff.smallDiff = Double.parseDouble(value);
            repaint();
        }
    }
    void doLargeDiff() {
        String value = JOptionPane.showInputDialog(this, "Large diff percentage (float)", "" + diff.largeDiff);
        if (value != null && !value.isEmpty()) {
            value = value.replace(',', '.');
            diff.largeDiff = Double.parseDouble(value);
            repaint();
        }
    }
    
    void doSaveTab() {
        int idx = tabs.getSelectedIndex();
        if (idx >= 0) {
            Component c = tabs.getComponentAt(idx);
            if (c instanceof ComparisonTab) {
                ComparisonTab ct = (ComparisonTab) c;
                JFileChooser fc = new JFileChooser(workdir);
                if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                    File f = fc.getSelectedFile();
                    workdir = f.getParentFile();
                    
                    XElement xtab = new XElement("jmh-compare-gui-tabs");
                    saveTab(xtab, ct);
                    
                    try {
                        xtab.save(f);
                    } catch (IOException e) {
                        reportError(e);
                    }
                }
            }
        }
    }
    
    void doSaveWorkspace() {
        JFileChooser fc = new JFileChooser(workdir);
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            workdir = f.getParentFile();
            
            XElement xtab = new XElement("jmh-compare-gui-tabs");
            saveAllTabs(xtab);
            
            try {
                xtab.save(f);
            } catch (IOException e) {
                reportError(e);
            }
        }
    }
    
    void doOpenWorkspace() {
        doOpenTab(true, true);
    }
    
    void doOpenTab(boolean clear, boolean workspace) {
        JFileChooser fc = new JFileChooser(workdir);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            workdir = f.getParentFile();
            
            try {
                if (clear) {
                    tabs.removeAll();
                }
                loadTabs(XElement.parseXML(f), workspace);
            } catch (XMLStreamException ex) {
                reportError(ex);
            }
        }
    }
    
    void doCloseTab() {
        int idx = tabs.getSelectedIndex();
        if (idx >= 0) {
            Component c = tabs.getComponentAt(idx);
            if (c instanceof ComparisonTab) {
                ComparisonTab ct = (ComparisonTab) c;
                ct.close();
            }
        }
    }
    
    void doCloseAllTabs() {
        tabs.removeAll();
        tabs.addTab("+", new JLabel());
    }
    
    void doNewTab() {
        ComparisonTab ct = new ComparisonTab(tabs, diff);
        ct.setTableFont(tableFont);
        ct.setPadding(cellPadding);
        int idx = tabs.getTabCount() - 1;
        tabs.insertTab("New tab", null, ct, null, idx);
        tabs.setSelectedIndex(idx);
    }
    
    void doHomepage() {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                URI uri = new URI("https://github.com/akarnokd/jmh-compare-gui");
                desktop.browse(uri);
                return;
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        reportError("Your platform doesn't seem to support Java Desktop. Please navigate to https://github.com/akarnokd/jmh-compare-gui manually.");
    }
    
    void doCellPadding() {
        String value = JOptionPane.showInputDialog(this, "Enter padding value", "" + cellPadding);
        
        if (value != null) {
            try {
                cellPadding = Integer.parseInt(value);
                for (int i = 0; i < tabs.getTabCount(); i++) {
                    Component tc = tabs.getComponentAt(i);
                    if (tc instanceof ComparisonTab) {
                        ComparisonTab ct = (ComparisonTab) tc;
                        ct.setPadding(cellPadding);
                    }
                }
            } catch (NumberFormatException ex) {
                reportError(ex);
            }
        }
    }
    
    void doChangeFont() {
        JFontChooser jfc = new JFontChooser();
        jfc.setSelectedFont(tableFont);
        if (jfc.showDialog(this) == JFontChooser.OK_OPTION) {
            tableFont = jfc.getSelectedFont();
            for (int i = 0; i < tabs.getTabCount(); i++) {
                Component tc = tabs.getComponentAt(i);
                if (tc instanceof ComparisonTab) {
                    ComparisonTab ct = (ComparisonTab) tc;
                    ct.setTableFont(tableFont);
                    ct.autoSize();
                }
            }
        }
    }
    
    boolean initConfig() {
        if (configFile.canRead()) {
            try {
                XElement config = XElement.parseXML(configFile);
                processConfig(config);
                return true;
            } catch (XMLStreamException ex) {
                ex.printStackTrace();
            }
        }
        return false;
    }
    
    void processConfig(XElement config) {
        int state = config.getInt("window-state", getState());
        setState(state);
        if (state != MAXIMIZED_BOTH && state != ICONIFIED) {
            setLocation(config.getInt("window-x", getX()), config.getInt("window-y", getY()));
            setSize(config.getInt("window-width", getWidth()), config.getInt("window-height", getHeight()));
        }
        
        loadTabs(config, true);
        
        if (tabs.getTabCount() > 0) {
            tabs.setSelectedIndex(Math.max(0, config.getInt("selected-tab", 0)));
        }
        
        String workdir = config.get("workdir", null);
        if (workdir != null) {
            this.workdir = new File(workdir);
        }
    }
    void loadTabs(XElement parent, boolean workspace) {
        int c = tabs.getTabCount();
        if (c > 0) {
            if (tabs.getComponentAt(c - 1) instanceof JLabel) {
                tabs.removeTabAt(c - 1);
            }
        }
        if (workspace) {
            cellPadding = parent.getInt("cell-padding", 0);
            
            XElement xfont = parent.childElement("font");
            if (xfont != null) {
                tableFont = new Font(xfont.get("name"), xfont.getInt("style"), xfont.getInt("size"));
            }
            localeDecimalSeparator = parent.getBoolean("locale-decimal-separator", false);
            csvSeparator = parent.get("csv-separator", ",");
            
            diff.smallDiff = parent.getDouble("small-diff", diff.smallDiff);
            diff.largeDiff = parent.getDouble("large-diff", diff.largeDiff);
            getColor(parent, "small-plus-color", rgb -> diff.smallPlus = rgb, diff.smallPlus);
            getColor(parent, "small-minus-color", rgb -> diff.smallMinus = rgb, diff.smallMinus);
            getColor(parent, "large-plus-color", rgb -> diff.largePlus = rgb, diff.largePlus);
            getColor(parent, "large-minus-color", rgb -> diff.largeMinus = rgb, diff.largeMinus);
        }
        for (XElement xtab : parent.childrenWithName("tab")) {
            ComparisonTab ct = new ComparisonTab(tabs, diff);
            if (tableFont != null) {
                ct.setTableFont(tableFont);
            }
            ct.setPadding(cellPadding);
            tabs.add(ct, "New tab");
            ct.load(xtab);
            ct.autoSize();
        }
        tabs.addTab("+", new JLabel());
    }
    
    void saveConfig() {
        XElement xconfig = new XElement("jmh-compare-gui-config");
        
        int state = getState();
        xconfig.set("window-state", state);
        if (state != MAXIMIZED_BOTH && state != ICONIFIED) {
            xconfig.set("window-x", getX());
            xconfig.set("window-y", getY());
            xconfig.set("window-width", getWidth());
            xconfig.set("window-height", getHeight());
        }
        xconfig.set("workdir", this.workdir.getAbsolutePath());
        saveAllTabs(xconfig);
        
        try {
            xconfig.save(configFile);
        } catch (IOException ex) {
            reportError(ex);
        }
    }
    
    void saveTab(XElement parent, ComparisonTab ct) {
        XElement tab = parent.add("tab");
        ct.save(tab);
    }
    
    void addColor(XElement parent, String name, Color c) {
        XElement xcolor = parent.add(name);
        xcolor.set("r", c.getRed());
        xcolor.set("g", c.getGreen());
        xcolor.set("b", c.getBlue());
    }
    void getColor(XElement parent, String name, Consumer<Color> colors, Color def) {
        XElement xcolor = parent.childElement(name);
        if (xcolor != null) {
            int r = xcolor.getInt("r", -1);
            int g = xcolor.getInt("g", -1);
            int b = xcolor.getInt("b", -1);
            if (r < 0 || g < 0 || b < 0) {
                colors.accept(def);
            } else {
                colors.accept(new Color(r, g, b));
            }
        }
    }
    
    void saveAllTabs(XElement parent) {
        parent.set("cell-padding", cellPadding);
        
        if (tableFont != null) {
            XElement xfont = parent.add("font");
            xfont.set("name", tableFont.getName());
            xfont.set("style", tableFont.getStyle());
            xfont.set("size", tableFont.getSize());
        }
        
        parent.set("csv-separator", csvSeparator);
        parent.set("locale-decimal-separator", localeDecimalSeparator);
        
        parent.set("small-diff", diff.smallDiff);
        parent.set("large-diff", diff.largeDiff);
        addColor(parent, "small-plus-color", diff.smallPlus);
        addColor(parent, "small-minus-color", diff.smallMinus);
        addColor(parent, "large-plus-color", diff.largePlus);
        addColor(parent, "large-minus-color", diff.largeMinus);
        
        parent.set("selected-tab", tabs.getSelectedIndex());
        
        for (int i = 0; i < tabs.getTabCount(); i++) {
            Component c = tabs.getComponentAt(i);
            if (c instanceof ComparisonTab) {
                ComparisonTab ct = (ComparisonTab) c;
                
                saveTab(parent, ct);
            }
        }
    }
    
    void quit() {
        dispose();
    }
    
    void reportError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    void reportError(Throwable ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this, ex.toString(), "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JMHCompareGUI gui = new JMHCompareGUI();
            gui.init();
            gui.setVisible(true);
        });
    }

    void doExportCSV() {
        ComparisonTab ct = (ComparisonTab)tabs.getSelectedComponent();
        if (ct != null) {
            JFileChooser fc = new JFileChooser(workdir);
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File f = fc.getSelectedFile();
                workdir = f.getParentFile();
                
                try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f)))) {
                    int c = 0;
                    for (String s : ct.model.columnNames) {
                        if (c++ > 0) {
                            out.print(csvSeparator);
                        }
                        printCell(out, s);
                    }
                    out.println();
                    for (JMHRowModel rm : ct.model) {
                        printCell(out, rm.benchmark);
                        int vs = ct.valueStart;
                        int j = 1;
                        for (Double v : rm.values) {
                            out.print(csvSeparator);
                            if (j < vs) {
                                printCell(out, rm.strings.get(j - 1));
                                j++;
                                continue;
                            }
                            if (v != null) {
                                if (localeDecimalSeparator) {
                                    printCell(out, String.format("%f", v));
                                } else {
                                    printCell(out, v.toString());
                                }
                            } else {
                                printCell(out, "");
                            }
                            j++;
                        }
                        out.println();
                    }
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, ex.toString(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
    static void printCell(PrintWriter out, String s) {
        out.print("\"");
        if (s != null) {
            s = StringUtils.replaceAll(s, "\"", "\"\"");
            out.print(s);
        }
        out.print("\"");
    }
    void doExportXLS() {
        ComparisonTab ct = (ComparisonTab)tabs.getSelectedComponent();
        if (ct != null) {
            JFileChooser fc = new JFileChooser(workdir);
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File f = fc.getSelectedFile();
                workdir = f.getParentFile();
                
                try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f)))) {
                    out.print("<html><head><title>JMH Benchmark Comparison: ");
                    out.print(XElement.sanitize(tabs.getTitleAt(tabs.getSelectedIndex())));
                    out.println("</title></head>");
                    out.println("<body>");
                    out.println("  <table>");
                    out.println("    <thead>");
                    out.println("      <tr>");
                    for (String s : ct.model.columnNames) {
                        out.print("        <th>");
                        out.print(XElement.sanitize(s));
                        out.println("</th>");
                    }
                    out.println("      </tr>");
                    out.println("    </thead>");
                    out.println("    <tbody>");
                    
                    for (JMHRowModel rm : ct.model) {
                        out.println("      <tr>");
                        out.print("        <td>");
                        out.print(XElement.sanitize(rm.benchmark));
                        out.println("</td>");
                        int vs = ct.valueStart;
                        int j = 1;
                        int col = 0;
                        for (Double v : rm.values) {
                            if (j < vs) {
                                out.print("        <td>");
                                out.print(XElement.sanitize(rm.strings.get(j - 1)));
                                out.println("</td>");
                                j++;
                                continue;
                            }
                            
                            out.print("        <td");
                            if (ct.compareIndex >= 0 && ct.compareIndex != col) {
                                Double v0 = rm.values.get(ct.valueStart + ct.compareIndex - 1);
                                if (v0 != null) {
                                    if (v != null) {
                                        double ratio = v / v0;
                                        if (ratio >= 1 + diff.largeDiff / 100) {
                                            printColor(out, diff.largePlus);
                                        } else
                                        if (ratio <= 1 - diff.largeDiff / 100) {
                                            printColor(out, diff.largeMinus);
                                        } else
                                        if (ratio >= 1 + diff.smallDiff / 100) {
                                            printColor(out, diff.smallPlus);
                                        } else
                                        if (ratio <= 1 - diff.smallDiff / 100) {
                                            printColor(out, diff.smallMinus);
                                        }
                                    }
                                }
                            }
                            out.print(">");
                            if (v != null) {
                                if (localeDecimalSeparator) {
                                    out.print(String.format("%f", v));
                                } else {
                                    out.print(v.toString());
                                }
                            }
                            out.println("</td>");
                            col++;
                            j++;
                        }
                        out.println("      </tr>");
                    }
                    out.println("    </tbody>");
                    out.println("  </table>");
                    out.println("</body>");
                    out.println("</html>");
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, ex.toString(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
    static void printColor(PrintWriter out, Color color) {
        out.print(" style='background-color: rgb(");
        out.print(color.getRed());
        out.print(", ");
        out.print(color.getGreen());
        out.print(", ");
        out.print(color.getBlue());
        out.print(");'");
    }
    void doExportSettings() {
        ExportSettingsDialog dlg = new ExportSettingsDialog(localeDecimalSeparator, csvSeparator);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
        if (dlg.isApproved()) {
            localeDecimalSeparator = dlg.isLocalDecimalSeparator();
            csvSeparator = dlg.getCsvSeparator();
        }
    }
}
