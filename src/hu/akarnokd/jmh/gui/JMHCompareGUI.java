package hu.akarnokd.jmh.gui;

import java.awt.event.*;

import javax.swing.*;

public class JMHCompareGUI extends JFrame {

    /** */
    private static final long serialVersionUID = -4168653287697309256L;

    void init() {
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                quit();
            }
        });
        setSize(640, 480);
        setLocationRelativeTo(null);
        setTitle("JMH Results Comparison");
    }
    
    void quit() {
        dispose();
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JMHCompareGUI gui = new JMHCompareGUI();
            gui.init();
            gui.setVisible(true);
        });
    }

}
