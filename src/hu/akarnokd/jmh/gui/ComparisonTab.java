package hu.akarnokd.jmh.gui;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.table.*;

import hu.akarnokd.utils.sequence.SequenceUtils;
import hu.akarnokd.utils.xml.XElement;

public class ComparisonTab extends JPanel {

    /** */
    private static final long serialVersionUID = -7892819950169164119L;
    private JTable table;
    public JMHResultModel model;
    public final List<JMHResults> results = new ArrayList<>();
    public int valueStart;
    public int compareIndex = -1;
    final JTabbedPane parent;
    private DefaultTableCellRenderer rightRenderer;
    final DiffConfig diff;
    JPopupMenu popup;
    Point rowCol;
    private JMenuItem mnuRename;
    private JMenuItem mnuDelete;
    private JMenuItem mnuUse;
    private JMenuItem mnuNoBaseline;
    private JMenuItem mnuClearSelection;
    private JMenuItem mnuDuplicate;
    JCheckBox cbShowErrors;
    private JMenuItem mnuDeleteRow;
    JCheckBox cbShowPercentages;
    JCheckBox cbReverseColors;

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
        commands.setLayout(new FlowLayout(FlowLayout.LEADING));
        add(commands, BorderLayout.PAGE_START);
        
        JButton rename = new JButton();//("Rename tab");
        JButton duplicate = new JButton();//("Duplicate tab");
        JButton close = new JButton();//("Close tab");
        JButton clear = new JButton();//("Clear");
        JButton paste = new JButton();//("Paste");
        JButton pastePivot = new JButton("Paste and pivot...");
        JButton nouse = new JButton("No baseline");
        
        commands.add(rename);
        commands.add(duplicate);
        commands.add(close);
        commands.add(new JLabel("    "));
        commands.add(clear);
        commands.add(paste);
        commands.add(pastePivot);
        commands.add(new JLabel("    "));
        commands.add(nouse);
        
        JPanel shows = new JPanel();
        shows.setLayout(new BoxLayout(shows, BoxLayout.PAGE_AXIS));
        
        cbShowErrors = new JCheckBox("Show errors");
        shows.add(cbShowErrors);
        
        cbShowPercentages = new JCheckBox("Show percentages");
        shows.add(cbShowPercentages);

        JPanel shows2 = new JPanel();
        shows2.setLayout(new BoxLayout(shows2, BoxLayout.PAGE_AXIS));

        cbReverseColors = new JCheckBox("Reverse colors");
        shows2.add(cbReverseColors);

        commands.add(shows);
        commands.add(shows2);

        commands.add(new JLabel("    "));
        
        JButton screenshot = new JButton();//("Screenshot");
        commands.add(screenshot);

        rename.setIcon(new ImageIcon(getClass().getResource("rename_icon.png")));
        rename.setToolTipText("Rename the current tab");
        duplicate.setIcon(new ImageIcon(getClass().getResource("duplicate_icon.png")));
        duplicate.setToolTipText("Duplicate the current tab");

        close.setIcon(new ImageIcon(getClass().getResource("close_icon.png")));
        close.setToolTipText("Close the current tab");

        clear.setIcon(new ImageIcon(getClass().getResource("clear_icon.png")));
        clear.setToolTipText("Clear the current tab");

        paste.setIcon(new ImageIcon(getClass().getResource("paste_icon.png")));
        paste.setToolTipText("Paste JMH results from the clipboard");

        screenshot.setIcon(new ImageIcon(getClass().getResource("screenshot_icon.png")));
        screenshot.setToolTipText("Take the screenshot of the results");
        
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                rowCol = new Point(row, col);
                boolean en = col >= valueStart;
                mnuRename.setEnabled(en);
                mnuDelete.setEnabled(en);
                mnuUse.setEnabled(en);
                mnuDuplicate.setEnabled(en);
                
                boolean er = row >= 0;
                mnuDeleteRow.setEnabled(er);
            }
        });

        popup = new JPopupMenu();

        table.setComponentPopupMenu(popup);
        
        mnuRename = new JMenuItem("Rename column...");
        mnuDelete = new JMenuItem("Delete column");
        mnuUse = new JMenuItem("Use as baseline");
        mnuNoBaseline = new JMenuItem("No baseline");
        mnuClearSelection = new JMenuItem("Clear selection");
        mnuDuplicate = new JMenuItem("Duplicate column");
        mnuDeleteRow = new JMenuItem("Delete row");
        
        popup.add(mnuRename);
        popup.addSeparator();
        popup.add(mnuDuplicate);
        popup.add(mnuDelete);
        popup.addSeparator();
        popup.add(mnuDeleteRow);
        popup.addSeparator();
        popup.add(mnuUse);
        popup.add(mnuNoBaseline);
        popup.addSeparator();
        popup.add(mnuClearSelection);
        
        paste.addActionListener(al -> pasteFromClipboard());
        pastePivot.addActionListener(al -> pasteFromClipboardAndPivot());
        
        clear.addActionListener(al -> {
            results.clear();
            buildModel();
            autoSize();
        });
        
        nouse.addActionListener(al -> {
            compareIndex = -1;
            repaint();
        });
        
        rename.addActionListener(al -> {
            int idx = parent.indexOfComponent(this);
            String name = JOptionPane.showInputDialog(ComparisonTab.this, "Rename tab", parent.getTitleAt(idx));
            if (name != null) {
                parent.setTitleAt(idx, name);
            }
        });
        
        close.addActionListener(al -> close());
        
        duplicate.addActionListener(al -> duplicate());
        
        // -------
        
        mnuClearSelection.addActionListener(al -> {
            table.clearSelection();
            requestFocusInWindow();
        });
        
        mnuRename.addActionListener(al -> {
            int i = rowCol.y - valueStart;
            if (i >= 0) {
                int div = 1;
                if (cbShowErrors.isSelected()) {
                    div++;
                }
                if (cbShowPercentages.isSelected()) {
                    div++;
                }
                i /= div;
                
                renameColumn(i);
            }
        });
        mnuUse.addActionListener(al -> {
            int i = rowCol.y - valueStart;
            if (i >= 0) {
                int div = 1;
                if (cbShowErrors.isSelected()) {
                    div++;
                }
                if (cbShowPercentages.isSelected()) {
                    div++;
                }
                i /= div;
                compareIndex = i;
            }
            repaint();
        });
        mnuNoBaseline.addActionListener(al -> {
            compareIndex = -1;
            repaint();
        });
        mnuDelete.addActionListener(al -> {
            int i = rowCol.y - valueStart;
            if (i >= 0) {
                int div = 1;
                if (cbShowErrors.isSelected()) {
                    div++;
                }
                if (cbShowPercentages.isSelected()) {
                    div++;
                }
                i /= div;
                
                int ci = compareIndex;
                
                results.remove(i);
                buildModel();
                autoSize();
                
                if (ci >= 0) {
                    if (i < ci) {
                        compareIndex = ci - 1;
                        repaint();
                    } else
                    if (i > ci) {
                        compareIndex = ci;
                        repaint();
                    }
                }
            }
        });
        
        mnuDuplicate.addActionListener(al -> {
            int i = rowCol.y - valueStart;
            if (i >= 0) {
                int div = 1;
                if (cbShowErrors.isSelected()) {
                    div++;
                }
                if (cbShowPercentages.isSelected()) {
                    div++;
                }
                i /= div;
                results.add(results.get(i).copy());
                buildModel();
                autoSize();
            }
        });
        
        cbShowErrors.addActionListener(al -> {
            buildModel();
            autoSize();
        });
        
        cbShowPercentages.addActionListener(al -> {
            buildModel();
            autoSize();
        });
        
        cbReverseColors.addActionListener(al -> {
            table.invalidate();
            table.repaint();
        });
        
        mnuDeleteRow.addActionListener(al -> {
            for (JMHResults r : results) {
                r.lines.remove(rowCol.x);
            }
            buildModel();
            autoSize();
        });
        
        screenshot.addActionListener(al -> doScreenshot());
    }
    
    void doScreenshot() {
        table.clearSelection();
        requestFocusInWindow();
        
        JTableHeader th = table.getTableHeader();

        int w = table.getWidth();
        int h = table.getHeight() + th.getHeight();
        
        BufferedImage bimg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        
        Graphics2D g2 = bimg.createGraphics();
        
        
        th.paint(g2);
        g2.translate(0, th.getHeight());
        table.paint(g2);
        
        g2.dispose();
        
        TransferableImage trans = new TransferableImage(bimg);
        Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
        c.setContents(trans, (e, f) -> { });
        
        Toolkit.getDefaultToolkit().beep();
    }
    
    public void close() {
        int idx = parent.indexOfComponent(this);
        if (idx > 0) {
            parent.setSelectedIndex(idx - 1);
        }
        parent.removeTabAt(idx);
    }
    
    public void duplicate() {
        int us = parent.indexOfComponent(this);
        int idx = parent.getTabCount() - 1;
        parent.setSelectedIndex(idx);

        parent.setTitleAt(idx, parent.getTitleAt(us));
        
        ComparisonTab ctbl = (ComparisonTab)parent.getComponentAt(idx);
        for (JMHResults r : results) {
            ctbl.results.add(r.copy());
        }
        ctbl.compareIndex = compareIndex;
        ctbl.cbShowErrors.setSelected(cbShowErrors.isSelected());
        ctbl.cbShowPercentages.setSelected(cbShowPercentages.isSelected());
        ctbl.cbReverseColors.setSelected(cbReverseColors.isSelected());
        
        ctbl.buildModel();
        ctbl.autoSize();
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

        valueStart = 0;
        
        boolean se = cbShowErrors.isSelected();
        boolean sp = cbShowPercentages.isSelected();
        
        if (!results.isEmpty()) {
            JMHResults r0 = results.get(0);
            for (String cn : r0.parameterNames) {
                columnNames.add(cn);
                columnClasses.add(String.class);
            }
            valueStart = columnNames.size();
            Map<String, Integer> benchmarkMap = new HashMap<>();
            for (JMHResults r1 : results) {
                columnNames.add(r1.name);
                columnClasses.add(String.class);
                if (se) {
                    columnNames.add(r1.name + " error");
                    columnClasses.add(String.class);
                }
                if (sp) {
                    columnNames.add(r1.name + " %");
                    columnClasses.add(String.class);
                }
                
                for (JMHResultLine rl : r1.lines) {
                    String key = rl.benchmark + "\t"
                            + SequenceUtils.join(rl.parameters, "\t"); 
                    benchmarkMap.putIfAbsent(key, benchmarkMap.size());
                }
            }
            for (JMHResultLine rl : r0.lines) {
                JMHRowModel rm = new JMHRowModel();
                
                rm.benchmark = rl.benchmark;
                rm.strings.addAll(rl.parameters);
                rm.strings.add(String.format("%,.3f", rl.value));
                if (se) {
                    rm.strings.add(String.format("%,.3f", rl.error));
                }
                if (sp) {
                    rm.strings.add("");
                }
                
                rl.parameters.forEach(c -> rm.values.add(null));
                
                rm.values.add(rl.value);
                if (se) {
                    rm.values.add(rl.error);
                }
                if (sp) {
                    rm.values.add(-1d);
                }
                
                for (int i = 1; i < results.size(); i++) {
                    rm.strings.add("");
                    rm.values.add(null);
                    if (se) {
                        rm.strings.add("");
                        rm.values.add(null);
                    }
                    
                    if (sp) {
                        rm.strings.add("");
                        rm.values.add(null);
                    }
                }
                
                rows.add(rm);
            }
            while (rows.size() < benchmarkMap.size()) {
                JMHRowModel rm = new JMHRowModel();
                for (int i = 0; i < results.size() + r0.parameterNames.size(); i++) {
                    rm.strings.add("");
                    rm.values.add(null);
                    if (se) {
                        rm.strings.add("");
                        rm.values.add(null);
                    }
                    
                    if (sp) {
                        rm.strings.add("");
                        rm.values.add(null);
                    }
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
                        rm.benchmark = rl.benchmark;
                        for (int k = 0; k < rl.parameters.size(); k++) {
                            rm.strings.set(k, rl.parameters.get(k));
                        }
                        int mul = 1;
                        if (se) {
                            mul++;
                        }
                        if (sp) {
                            mul++;
                        }
                        int i1 = i * mul;
                        int o = valueStart + i1 - 1;
                        rm.strings.set(o, String.format("%,.3f", rl.value));
                        rm.values.set(o, rl.value);
                        int offs = 1;
                        if (se) {
                            rm.strings.set(o + offs, String.format("%,.3f", rl.error));
                            rm.values.set(o + offs, rl.error);
                            offs++;
                        }
                        if (sp) {
                            rm.strings.set(o + offs, "");
                            rm.values.set(o + offs, -1d);
                        }
                    }
                }
                
            }
        } else {
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
            int idxj = 0;
            int div = 1;
            if (cbShowErrors.isSelected()) {
                div++;
            }
            if (cbShowPercentages.isSelected()) {
                div++;
            }
            
            idxj = idx % div;
            idx /= div;

            boolean straightColors = !cbReverseColors.isSelected();
            
            if (!isSelected) {
                if (compareIndex >= 0 && idx >= 0 && idx != compareIndex && column >= valueStart) {
                    int comp = valueStart + compareIndex - 1;
                    if (cbShowErrors.isSelected()) {
                        comp += compareIndex;
                    }
                    if (cbShowPercentages.isSelected()) {
                        comp += compareIndex;
                    }
                    JMHRowModel jmhRowModel = model.get(row);
                    List<Double> values = jmhRowModel.values;
                    Double c0 = values.size() > comp ? values.get(comp) : null;
                    if (c0 != null) {
                        int vidx = column - 1 - idxj;
                        Double c1 = jmhRowModel.values.get(vidx);
                        if (c1 != null) {
                            double ratio = c1 / c0;
                            if (ratio >= 1 + diff.largeDiff / 100) {
                                c.setBackground(straightColors ? diff.largePlus : diff.largeMinus);
                            } else
                            if (ratio <= 1 - diff.largeDiff / 100) {
                                c.setBackground(straightColors ? diff.largeMinus : diff.largePlus);
                            } else
                            if (ratio >= 1 + diff.smallDiff / 100) {
                                c.setBackground(straightColors ? diff.smallPlus : diff.smallMinus);
                            } else
                            if (ratio <= 1 - diff.smallDiff / 100) {
                                c.setBackground(straightColors ? diff.smallMinus : diff.smallPlus);
                            } else {
                                c.setBackground(table.getBackground());
                            }

                            
                            Double pc = jmhRowModel.values.get(column - 1);
                            if (pc != null && pc < 0d) {
                                double percent = ratio - 1;
                                String pstr = percent > 0d 
                                        ? String.format("+%.2f %%", percent * 100d) 
                                        : String.format("%.2f %%", percent * 100d);
                                ((JLabel)c).setText(pstr);
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
    void renameColumn(int idx) {
        JMHResults rs = results.get(idx);
        String name = JOptionPane.showInputDialog(ComparisonTab.this, "Rename result", rs.name != null ? rs.name : "");
        if (name != null) {
            rs.name = name;
            int j = 1;
            if (cbShowErrors.isSelected()) {
                j++;
            }
            if (cbShowPercentages.isSelected()) {
                j++;
            }
            table.getColumnModel().getColumn(valueStart + idx * j).setHeaderValue(name);
            int offs = 1;
            if (cbShowErrors.isSelected()) {
                table.getColumnModel().getColumn(valueStart + idx * j + offs).setHeaderValue(name + " error");
                offs++;
            }
            if (cbShowPercentages.isSelected()) {
                table.getColumnModel().getColumn(valueStart + idx * j + offs).setHeaderValue(name + " %");
            }
            repaint();
        }
    }
    public void save(XElement out) {
        int idx = parent.indexOfComponent(this);
        out.set("title", parent.getTitleAt(idx));
        out.set("compare-index", compareIndex);
        out.set("show-errors", cbShowErrors.isSelected());
        out.set("show-percentages", cbShowPercentages.isSelected());
        out.set("reverse-colors", cbReverseColors.isSelected());
        
        for (JMHResults rs : results) {
            XElement xrs = out.add("results");
            rs.save(xrs);
        }
    }
    public void load(XElement in) {
        int idx = parent.indexOfComponent(this);
        parent.setTitleAt(idx, in.get("title", "New tab"));
        compareIndex = in.getInt("compare-index", -1);
        cbShowErrors.setSelected(in.getBoolean("show-errors", false));
        cbShowPercentages.setSelected(in.getBoolean("show-percentages", false));
        cbReverseColors.setSelected(in.getBoolean("reverse-colors", false));
        
        results.clear();
        for (XElement xrs : in.childrenWithName("results")) {
            JMHResults rs = new JMHResults();
            rs.load(xrs);
            results.add(rs);
        }
        
        buildModel();
    }
    void pasteFromClipboardAndPivot() {
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
                    if (r.parameterNames.size() > 0) {
                        PastePivotDialog dlg = new PastePivotDialog(r);
                        dlg.setLocationRelativeTo(this);
                        dlg.setVisible(true);
                        if (dlg.isApprove()) {
                            results.clear();
                            String param = dlg.getParameter();
                            int pidx = r.parameterNames.indexOf(param);
                            
                            Set<String> values = new LinkedHashSet<>();
                            for (JMHResultLine rl : r.lines) {
                                values.add(rl.parameters.get(pidx));
                            }
                            r.parameterNames.remove(pidx);

                            for (String pvalue : values) {
                                JMHResults r2 = new JMHResults();
                                r2.name = param + " = " + pvalue;
                                r2.parameterNames.addAll(r.parameterNames);
                                
                                for (JMHResultLine rl : r.lines) {
                                    if (rl.parameters.get(pidx).equals(pvalue)) {
                                        r2.lines.add(rl);
                                    }
                                }
                                results.add(r2);
                            }
                            for (JMHResults r2 : results) {
                                for (JMHResultLine rl : r2.lines) {
                                    rl.parameters.remove(pidx);
                                }
                            }
                            
                            buildModel();
                            autoSize();
                        }
                    } else {
                        results.add(r);
                        buildModel();
                        autoSize();
                    }
                }
            } catch (UnsupportedFlavorException | IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Unable to paste the contents of the clipboard", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "The clipboard doesn't contain text", "Information", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
