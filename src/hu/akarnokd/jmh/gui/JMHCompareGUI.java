package hu.akarnokd.jmh.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.File;

import javax.swing.*;

public class JMHCompareGUI extends JFrame {

    /** */
    private static final long serialVersionUID = -4168653287697309256L;
    private JTabbedPane tabs;
    final File configFile = new File("./jmh-compare-gui-config.xml");

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
                tabs.setComponentAt(idx, ct);
                tabs.setTitleAt(idx, "New tab");
                tabs.addTab("+", new JLabel());
            }
        });
    }
    
    boolean initConfig() {
        if (configFile.canRead()) {
            
        }
        return false;
    }
    
    void saveConfig() {
        
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
