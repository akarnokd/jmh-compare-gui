/**
 * 
 */
package hu.akarnokd.jmh.gui;

import java.awt.Container;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;

/**
 *
 */
public class ExportSettingsDialog extends JDialog {

    /** */
    private static final long serialVersionUID = -5337321390846247856L;
    private JCheckBox cbLocale;
    private JTextField txSeparator;
    private boolean approved;

    public ExportSettingsDialog(boolean localeDecimalSeparator, String csvSeparator) {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setTitle("Export settings");

        Container c = getContentPane();
        
        cbLocale = new JCheckBox("Locale-specific decimal separator (example: " + String.format("%.1f", 1.5) + ")");
        cbLocale.setSelected(localeDecimalSeparator);
        JLabel lblSeparator = new JLabel("CSV cell separator: ");
        txSeparator = new JTextField(csvSeparator);
        txSeparator.setColumns(3);
        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Cancel");
        
        GroupLayout gl = new GroupLayout(c);
        c.setLayout(gl);
        gl.setAutoCreateContainerGaps(true);
        gl.setAutoCreateGaps(true);
        
        gl.setHorizontalGroup(
           gl.createParallelGroup(Alignment.CENTER)
           .addGroup(
                gl.createParallelGroup(Alignment.LEADING)
                .addComponent(cbLocale)
                .addGroup(
                     gl.createSequentialGroup()
                     .addComponent(lblSeparator)
                     .addComponent(txSeparator, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                )
           )
           .addGroup(
                gl.createSequentialGroup()
                .addComponent(ok)
                .addComponent(cancel)
           )
        );
        
        gl.setVerticalGroup(
            gl.createSequentialGroup()
            .addComponent(cbLocale)
            .addGroup(
                 gl.createParallelGroup(Alignment.BASELINE)
                 .addComponent(lblSeparator)
                 .addComponent(txSeparator)
            )
            .addGroup(
                gl.createParallelGroup(Alignment.BASELINE)
                .addComponent(ok)
                .addComponent(cancel)
           )
        );
        
        gl.linkSize(SwingConstants.HORIZONTAL, ok, cancel);
        
        ok.addActionListener(e -> {
            approved = true;
            setVisible(false);
        });
        cancel.addActionListener(e -> {
            setVisible(false);
        });
        
        pack();
        setResizable(false);
        setModal(true);
    }
    
    public boolean isApproved() {
        return approved;
    }
    public boolean isLocalDecimalSeparator() {
        return cbLocale.isSelected();
    }
    public String getCsvSeparator() {
        return txSeparator.getText();
    }
}
