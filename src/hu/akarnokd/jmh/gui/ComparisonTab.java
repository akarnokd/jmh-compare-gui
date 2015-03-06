package hu.akarnokd.jmh.gui;

import hu.akarnokd.utils.sequence.SequenceUtils;
import hu.akarnokd.utils.xml.XElement;

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
    private JTable table;
    public JMHResultModel model;
    public final List<JMHResults> results = new ArrayList<>();
    private JComboBox<String> cols;
    public int valueStart;
    public int compareIndex = -1;
    JButton delete;
    JButton use;
    final JTabbedPane parent;
    private JButton renameCol;
    private DefaultTableCellRenderer rightRenderer;
    final DiffConfig diff;

    public ComparisonTab(JTabbedPane parent, DiffConfig diff) {
        this.parent = parent;
        this.diff = diff;
        setLayout(new BorderLayout());
        
        table = new JTable();
        
        JScrollPane sp = new JScrollPane(table);
        
        model = new JMHResultModel();

        table.setModel(model);
        
        rightRenderer = new ColoringCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        table.setDefaultRenderer(String.class, rightRenderer);
        table.setColumnSelectionAllowed(true);
        
        
        add(sp, BorderLayout.CENTER);
        
        JPanel commands = new JPanel();
        commands.setLayout(new FlowLayout());
        add(commands, BorderLayout.PAGE_START);
        
        JButton rename = new JButton("Rename tab");
        JButton close = new JButton("Close tab");
        JButton clear = new JButton("Clear");
        JButton paste = new JButton("Paste");
        cols = new JComboBox<>();
        renameCol = new JButton("Rename column");
        delete = new JButton("Delete");
        use = new JButton("Use as baseline");
        JButton nouse = new JButton("No baseline");
        
        commands.add(rename);
        commands.add(close);
        commands.add(new JLabel("    "));
        commands.add(clear);
        commands.add(paste);
        commands.add(new JLabel("    "));
        commands.add(cols);
        commands.add(renameCol);
        commands.add(delete);
        commands.add(use);
        commands.add(nouse);
        
        paste.addActionListener(al -> pasteFromClipboard());
        
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
            repaint();
        });
        nouse.addActionListener(al -> {
            compareIndex = -1;
            repaint();
        });
        
        rename.addActionListener(al -> {
            int idx = parent.indexOfComponent(this);
            String name = JOptionPane.showInputDialog(ComparisonTab.this, "Rename tab", parent.getTitleAt(idx));
            
            parent.setTitleAt(idx, name);
        });
        
        close.addActionListener(al -> close());
        
        renameCol.addActionListener(al -> renameColumn());
        
        cols.setEnabled(false);
        renameCol.setEnabled(false);
        delete.setEnabled(false);
        use.setEnabled(false);
    }
    public void close() {
        int idx = parent.indexOfComponent(this);
        if (idx > 0) {
            parent.setSelectedIndex(idx - 1);
        }
        parent.removeTabAt(idx);
    }
    public void setTableFont(Font font) {
        table.setFont(font);
        adjustTable();
        autoSize();
        table.repaint();
    }
    public void setPadding(int padding) {
        table.setIntercellSpacing(new Dimension(padding, padding));
        adjustTable();
        autoSize();
        table.repaint();
    }

    void adjustTable() {
        Dimension dim = table.getIntercellSpacing();
        Font font = table.getFont();
        
        table.setRowHeight(dim.height * 2 + font.getSize() + 1);
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
                rm.strings.addAll(rl.parameters);
                rm.strings.add(String.format("%,.3f", rl.value));
                
                rl.parameters.forEach(c -> rm.values.add(null));
                
                rm.values.add(rl.value);
                
                String key = rl.benchmark + "\t"
                        + SequenceUtils.join(rl.parameters, "\t"); 
                benchmarkMap.put(key, benchmarkMap.size());

                for (int i = 1; i < results.size(); i++) {
                    rm.strings.add("");
                    rm.values.add(null);
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
                        rm.strings.set(valueStart + i - 1, String.format("%,.3f", rl.value));
                        rm.values.set(valueStart + i - 1, rl.value);
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
    
    private final class ColoringCellRenderer extends
            DefaultTableCellRenderer {
        /** */
        private static final long serialVersionUID = -4132082823550790050L;

        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
                    row, column);
            int idx = column - valueStart;
            if (!isSelected) {
                if (compareIndex >= 0 && idx >= 0 && idx != compareIndex && column >= valueStart) {
                    int comp = valueStart + compareIndex - 1;
                    Double c0 = model.get(row).values.get(comp);
                    if (c0 != null) {
                        Double c1 = model.get(row).values.get(column - 1);
                        if (c1 != null) {
                            double ratio = c1 / c0;
                            if (ratio >= 1 + diff.largeDiff / 100) {
                                c.setBackground(diff.largePlus);
                            } else
                            if (ratio <= 1 - diff.largeDiff / 100) {
                                c.setBackground(diff.largeMinus);
                            } else
                            if (ratio >= 1 + diff.smallDiff / 100) {
                                c.setBackground(diff.smallPlus);
                            } else
                            if (ratio <= 1 - diff.smallDiff / 100) {
                                c.setBackground(diff.smallMinus);
                            } else {
                                c.setBackground(table.getBackground());
                            }
                        } else {
                            c.setBackground(table.getBackground());
                        }
                    } else {
                        c.setBackground(table.getBackground());
                    }
                } else {
                    c.setBackground(table.getBackground());
                }
            }
            
            return c;
        }
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
            return item.strings.get(columnIndex - 1);
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
        public final List<String> strings = new ArrayList<>();
        public final List<Double> values = new ArrayList<>();
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
        int idx = cols.getSelectedIndex();
        JMHResults rs = results.get(idx);
        String name = JOptionPane.showInputDialog(ComparisonTab.this, "Rename result", rs.name != null ? rs.name : "");
        rs.name = name;
        table.getColumnModel().getColumn(valueStart + idx).setHeaderValue(name);
        ((DefaultComboBoxModel<String>)cols.getModel()).removeElementAt(idx);
        ((DefaultComboBoxModel<String>)cols.getModel()).insertElementAt((idx + 1) + ": " + name, idx);
        cols.setSelectedIndex(idx);
        repaint();
    }
    public void save(XElement out) {
        int idx = parent.indexOfComponent(this);
        out.set("title", parent.getTitleAt(idx));
        out.set("compare-index", compareIndex);
        
        for (JMHResults rs : results) {
            XElement xrs = out.add("results");
            rs.save(xrs);
        }
    }
    public void load(XElement in) {
        int idx = parent.indexOfComponent(this);
        parent.setTitleAt(idx, in.get("title", "New tab"));
        compareIndex = in.getInt("compare-index", -1);
        
        results.clear();
        for (XElement xrs : in.childrenWithName("results")) {
            JMHResults rs = new JMHResults();
            rs.load(xrs);
            results.add(rs);
        }
        
        buildModel();
    }
}
