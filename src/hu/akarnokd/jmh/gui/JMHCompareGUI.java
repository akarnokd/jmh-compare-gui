package hu.akarnokd.jmh.gui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

public class JMHCompareGUI extends JFrame {

    /** */
    private static final long serialVersionUID = -4168653287697309256L;
    private JTabbedPane tabs;

    void init() {
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
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
        createTab();
    }
    void createTab() {
        ComparisonTab ct = new ComparisonTab();
        String example = JMHResults.example();
        JMHResults r = new JMHResults();
        r.parse(example);
        ct.results.add(r);
        ct.results.add(r);
        ct.buildModel();
        ct.autoSize();
        tabs.addTab("New tab", ct);
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
