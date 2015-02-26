package hu.akarnokd.jmh.gui;

import hu.akarnokd.utils.xml.XElement;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

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
        }
        tabs.addTab("+", new JLabel());
        tabs.addChangeListener(cl -> {
            int idx = tabs.getSelectedIndex();
            Component tc = tabs.getComponentAt(idx);
            if (tc instanceof JLabel) {
                ComparisonTab ct = new ComparisonTab(tabs);
                ct.setTableFont(tableFont);
                ct.setPadding(cellPadding);
                tabs.setComponentAt(idx, ct);
                tabs.setTitleAt(idx, "New tab");
                tabs.addTab("+", new JLabel());
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
        JMenuItem mnuNewWorkspace = new JMenuItem("New workspace");
        mnuFile.add(mnuNewWorkspace);
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
    }
    
    void doCellPadding() {
        String value = JOptionPane.showInputDialog(this, "Enter padding value", "" + cellPadding);
        
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
        cellPadding = config.getInt("cell-padding", 0);
        
        XElement xfont = config.childElement("font");
        if (xfont != null) {
            tableFont = new Font(xfont.get("name"), xfont.getInt("style"), xfont.getInt("size"));
        }
        
        for (XElement xtab : config.childrenWithName("tab")) {
            ComparisonTab ct = new ComparisonTab(tabs);
            if (tableFont != null) {
                ct.setTableFont(tableFont);
            }
            ct.setPadding(cellPadding);
            tabs.add(ct, "New tab");
            ct.load(xtab);
            ct.autoSize();
        }
    }
    
    void saveConfig() {
        XElement config = new XElement("jmh-compare-gui-config");
        
        int state = getState();
        config.set("window-state", state);
        if (state != MAXIMIZED_BOTH && state != ICONIFIED) {
            config.set("window-x", getX());
            config.set("window-y", getY());
            config.set("window-width", getWidth());
            config.set("window-height", getHeight());
        }
        config.set("cell-padding", cellPadding);
        
        if (tableFont != null) {
            XElement xfont = config.add("font");
            xfont.set("name", tableFont.getName());
            xfont.set("style", tableFont.getStyle());
            xfont.set("size", tableFont.getSize());
        }
        
        int tabCount = 0;
        for (int i = 0; i < tabs.getTabCount(); i++) {
            Component c = tabs.getComponentAt(i);
            if (c instanceof ComparisonTab) {
                
                ComparisonTab ct = (ComparisonTab) c;
                
                XElement tab = config.add("tab");
                ct.save(tab);
                
                tabCount++;
            }
        }
        config.set("tab-count", tabCount);
        
        try {
            config.save(configFile);
        } catch (IOException ex) {
            reportError(ex);
        }
    }
    
    
    void quit() {
        dispose();
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
