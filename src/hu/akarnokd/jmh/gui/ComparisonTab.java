package hu.akarnokd.jmh.gui;

import hu.akarnokd.utils.sequence.SequenceUtils;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.IOException;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;

public class ComparisonTab extends JPanel {

    /** */
    private static final long serialVersionUID = -7892819950169164119L;
    public JTable table;
    public JMHResultModel model;
    public final List<JMHResults> results = new ArrayList<>();
    private JComboBox<String> cols;
    public int valueStart;
    public int compareIndex;
    JButton delete;
    JButton use;
    final JTabbedPane parent;
    private JButton renameCol;

    public ComparisonTab(JTabbedPane parent) {
        this.parent = parent;
        setLayout(new BorderLayout());
        
        table = new JTable();
        
        JScrollPane sp = new JScrollPane(table);
        
        model = new JMHResultModel();

        table.setModel(model);
        
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        table.setDefaultRenderer(String.class, rightRenderer);
        
        add(sp, BorderLayout.CENTER);
        
        JPanel commands = new JPanel();
        commands.setLayout(new FlowLayout());
        add(commands, BorderLayout.PAGE_START);
        
        JButton rename = new JButton("Rename tab");
        JButton close = new JButton("Close tab");
        JButton clear = new JButton("Clear");
        JButton paste = new JButton("Paste");
        JButton pasteExample = new JButton("Paste example");
        cols = new JComboBox<>();
        renameCol = new JButton("Rename column");
        delete = new JButton("Delete");
        use = new JButton("Use as base");
        
        commands.add(rename);
        commands.add(close);
        commands.add(clear);
        commands.add(paste);
        commands.add(pasteExample);
        commands.add(cols);
        commands.add(delete);
        commands.add(use);
        
        paste.addActionListener(al -> pasteFromClipboard());
        
        pasteExample.addActionListener(al -> {
            JMHResults r = new JMHResults();
            r.parse(JMHResults.example());
            results.add(r);
            buildModel();
            autoSize();
        });
        
        clear.addActionListener(al -> {
            results.clear();
            buildModel();
            autoSize();
        });
        
        delete.addActionListener(al -> {
            int i = cols.getSelectedIndex();
            if (i >= 0 && i < results.size()) {
                results.remove(i);
            }
            buildModel();
            autoSize();
        });
        use.addActionListener(al -> {
            int i = cols.getSelectedIndex();
            if (i >= 0 && i < results.size()) {
                compareIndex = i;
            } else {
                compareIndex = -1;
            }
        });
        
        rename.addActionListener(al -> {
            int idx = parent.indexOfComponent(this);
            String name = JOptionPane.showInputDialog(ComparisonTab.this, "Rename tab", parent.getTitleAt(idx));
            
            parent.setTitleAt(idx, name);
        });
        
        close.addActionListener(al -> {
            int idx = parent.indexOfComponent(this);
            parent.removeTabAt(idx);
        });
        
        renameCol.addActionListener(al -> renameColumn());
    }
    
    public void buildModel() {
        
        List<JMHRowModel> rows = new ArrayList<>();
        List<String> columnNames = new ArrayList<>();
        List<Class<?>> columnClasses = new ArrayList<>();
        columnNames.add("Benchmark");
        columnClasses.add(String.class);

        cols.removeAllItems();
        valueStart = 0;
        
        if (!results.isEmpty()) {
            JMHResults r0 = results.get(0);
            for (String cn : r0.parameterNames) {
                columnNames.add(cn);
                columnClasses.add(String.class);
            }
            valueStart = columnNames.size();
            for (JMHResults r1 : results) {
                columnNames.add(r1.name);
                columnClasses.add(String.class);
                
                cols.addItem((cols.getItemCount() + 1) + " - " + r1.name);
            }
            Map<String, Integer> benchmarkMap = new HashMap<>();
            for (JMHResultLine rl : r0.lines) {
                JMHRowModel rm = new JMHRowModel();
                
                rm.benchmark = rl.benchmark;
                rm.values.addAll(rl.parameters);
                rm.values.add(String.format("%,.3f", rl.value));
                
                String key = rl.benchmark + "\t"
                        + SequenceUtils.join(rl.parameters, "\t"); 
                benchmarkMap.put(key, benchmarkMap.size());

                for (int i = 1; i < results.size(); i++) {
                    rm.values.add("");
                }
                
                rows.add(rm);
            }
            for (int i = 1; i < results.size(); i++) {
                JMHResults r1 = results.get(i);

                for (JMHResultLine rl : r1.lines) {
                    String key = rl.benchmark + "\t"
                            + SequenceUtils.join(rl.parameters, "\t");
                    Integer idx = benchmarkMap.get(key);
                    if (idx != null) {
                        JMHRowModel rm = rows.get(idx);
                        rm.values.set(valueStart + i - 1, String.format("%,.3f", rl.value));
                    }
                }
                
            }
            cols.setEnabled(true);
            delete.setEnabled(true);
            use.setEnabled(true);
            renameCol.setEnabled(true);
        } else {
            cols.setEditable(false);
            delete.setEnabled(false);
            use.setEnabled(false);
            renameCol.setEnabled(false);
            compareIndex = -1;
        }
        
        model.clear();
        
        model.setColumnNames(columnNames.toArray(new String[0]));
        model.setColumnTypes(columnClasses.toArray(new Class<?>[0]));
        model.add(rows);
        
        model.fireTableStructureChanged();
    }
    
    static final class JMHResultModel extends GenericTableModel<JMHRowModel> {
        /** */
        private static final long serialVersionUID = -2306469113149083137L;
        @Override
        public Object getValueFor(JMHRowModel item, int rowIndex,
                int columnIndex) {
            if (columnIndex == 0) {
                return item.benchmark;
            }
            return item.values.get(columnIndex - 1);
        }
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }
    }
    public void autoSize() {
        GUIUtils.autoResizeColWidth(table, model);
    }
    static final class JMHRowModel {
        public String benchmark;
        public final List<String> values = new ArrayList<>();
    }
    
    void pasteFromClipboard() {
        Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
        
        Transferable contents = cb.getContents(null);
        if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            try {
                String value = (String)contents.getTransferData(DataFlavor.stringFlavor);
                
                JMHResults r = new JMHResults();
                int e = r.parse(value);
                if (e < 0) {
                    switch (e) {
                    case JMHResults.EMPTY:
                        JOptionPane.showMessageDialog(this, "Empty contents", "Error", JOptionPane.ERROR_MESSAGE);
                        break;
                    case JMHResults.IO_ERROR:
                        JOptionPane.showMessageDialog(this, "IO error while processing contents", "Error", JOptionPane.ERROR_MESSAGE);
                        break;
                    case JMHResults.NO_BENCHMARK:
                        JOptionPane.showMessageDialog(this, "No header row found starting with 'Benchmark'", "Error", JOptionPane.ERROR_MESSAGE);
                        break;
                    case JMHResults.NUMBER_FORMAT:
                        JOptionPane.showMessageDialog(this, "Unsupported number format", "Error", JOptionPane.ERROR_MESSAGE);
                        break;
                    case JMHResults.ROW_FORMAT:
                        JOptionPane.showMessageDialog(this, "Error in the row format", "Error", JOptionPane.ERROR_MESSAGE);
                        break;
                    default:
                    }
                } else {
                    results.add(r);
                    buildModel();
                    autoSize();
                }
            } catch (UnsupportedFlavorException | IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Unable to paste the contents of the clipboard", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "The clipboard doesn't contain text", "Information", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    void reportError(Throwable ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this, ex.toString(), "Error", JOptionPane.ERROR_MESSAGE);
    }
    void renameColumn() {
        
    }
}
