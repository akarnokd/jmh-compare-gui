package hu.akarnokd.jmh.gui;

import hu.akarnokd.utils.xml.XElement;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URI;

import javax.swing.*;
import javax.xml.stream.XMLStreamException;

import say.swing.JFontChooser;

public class JMHCompareGUI extends JFrame {

    /** */
    private static final long serialVersionUID = -4168653287697309256L;
    private JTabbedPane tabs;
    final File configFile = new File("./jmh-compare-gui-config.xml");
    Font tableFont;
    int cellPadding;
    File workdir = new File(".");

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
        setTitle("JMH Results Comparison");
        
        tabs = new JTabbedPane();
        Container c = getContentPane();
        c.setLayout(new BorderLayout());
        c.add(tabs, BorderLayout.CENTER);
        
        tableFont = tabs.getFont().deriveFont(Font.PLAIN);

        initMenu();
        
        if (!initConfig()) {
            ComparisonTab ct = new ComparisonTab(tabs);
            tabs.addTab("New tab", ct);
            tabs.addTab("+", new JLabel());
        }
        tabs.addChangeListener(cl -> {
            int idx = tabs.getSelectedIndex();
            if (idx >= 0) {
                Component tc = tabs.getComponentAt(idx);
                if (tc instanceof JLabel) {
                    ComparisonTab ct = new ComparisonTab(tabs);
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

        JMenuItem mnuExit = new JMenuItem("Exit");
        mnuFile.add(mnuExit);

        JMenu mnuView = new JMenu("View");
        menubar.add(mnuView);
        
        JMenuItem mnuFont = new JMenuItem("Font...");
        mnuView.add(mnuFont);

        JMenuItem mnuCellPadding = new JMenuItem("Cell padding...");
        mnuView.add(mnuCellPadding);

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
        ComparisonTab ct = new ComparisonTab(tabs);
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
        }
        for (XElement xtab : parent.childrenWithName("tab")) {
            ComparisonTab ct = new ComparisonTab(tabs);
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
    
    void saveAllTabs(XElement parent) {
        parent.set("cell-padding", cellPadding);
        
        if (tableFont != null) {
            XElement xfont = parent.add("font");
            xfont.set("name", tableFont.getName());
            xfont.set("style", tableFont.getStyle());
            xfont.set("size", tableFont.getSize());
        }
        
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

}
