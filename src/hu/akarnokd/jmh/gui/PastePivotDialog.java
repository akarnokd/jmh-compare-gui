package hu.akarnokd.jmh.gui;

import java.awt.Container;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;

public class PastePivotDialog extends JDialog {
    /** */
    private static final long serialVersionUID = 506187977384511894L;
    private boolean approve;
    private String parameter;
    
    public PastePivotDialog(JMHResults r) {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setTitle("Chose pivot parameter");
        
        Container c = getContentPane();

        JLabel lblParams = new JLabel("Available parameters (pick one):");
        JList<String> list = new JList<>();
        DefaultListModel<String> model = new DefaultListModel<>();
        r.parameterNames.forEach(model::addElement);
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && list.getSelectedIndex() >= 0) {
                    approve = true;
                    parameter = list.getSelectedValue();
                    dispose();
                }
            }
        });
        
        JButton ok = new JButton("OK");
        ok.addActionListener(e -> {
            approve = true;
            parameter = list.getSelectedValue();
            dispose();
        });
        ok.setEnabled(false);
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> {
            setVisible(false);
        });

        list.setModel(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addListSelectionListener(e -> {
            ok.setEnabled(list.getSelectedIndex() >= 0);
        });

        JScrollPane sp = new JScrollPane(list);
        
        GroupLayout gl = new GroupLayout(c);
        c.setLayout(gl);
        gl.setAutoCreateContainerGaps(true);
        gl.setAutoCreateGaps(true);
        
        gl.setHorizontalGroup(
            gl.createParallelGroup(Alignment.CENTER)
            .addComponent(lblParams)
            .addComponent(sp, 30, 200, Short.MAX_VALUE)
            .addGroup(
                gl.createSequentialGroup()
                .addComponent(ok)
                .addComponent(cancel)
            )
        );
        gl.setVerticalGroup(
            gl.createSequentialGroup()
            .addComponent(lblParams)
            .addComponent(sp)
            .addGroup(
                gl.createParallelGroup(Alignment.BASELINE)
                .addComponent(ok)
                .addComponent(cancel)
            )
        );
        
        gl.linkSize(SwingConstants.HORIZONTAL, ok, cancel);
        
        pack();
        setModal(true);
    }
    public boolean isApprove() {
        return approve;
    }
    public String getParameter() {
        return parameter;
    }
}
