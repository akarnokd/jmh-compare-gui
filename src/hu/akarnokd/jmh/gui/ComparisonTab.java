package hu.akarnokd.jmh.gui;

import hu.akarnokd.utils.sequence.SequenceUtils;

import java.awt.*;
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

    public ComparisonTab() {
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
        cols = new JComboBox<String>();
        JButton delete = new JButton("Delete");
        JButton use = new JButton("Use as base");
        
        commands.add(rename);
        commands.add(close);
        commands.add(clear);
        commands.add(paste);
        commands.add(cols);
        commands.add(delete);
        commands.add(use);
        
        paste.addActionListener(al -> {
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
    }
    
    public void buildModel() {
        
        List<JMHRowModel> rows = new ArrayList<>();
        List<String> columnNames = new ArrayList<>();
        List<Class<?>> columnClasses = new ArrayList<>();
        columnNames.add("Benchmark");
        columnClasses.add(String.class);

        cols.removeAllItems();
        
        if (!results.isEmpty()) {
            JMHResults r0 = results.get(0);
            for (String cn : r0.parameterNames) {
                columnNames.add(cn);
                columnClasses.add(String.class);
            }
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
                        rm.values.add(String.format("%,.3f", rl.value));
                    }
                }
            }
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
}
