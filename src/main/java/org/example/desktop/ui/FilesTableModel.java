package org.example.desktop.ui;

import org.example.desktop.model.RemoteFileMetadata;

import javax.swing.table.AbstractTableModel;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FilesTableModel extends AbstractTableModel {
    private final String[] cols = {"Name","Created","Modified","Uploaded By","Edited By","Size"};
    private final List<RemoteFileMetadata> data = new ArrayList<>();
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    public void setFiles(List<RemoteFileMetadata> files) {
        data.clear();
        if (files != null) data.addAll(files);
        fireTableDataChanged();
    }

    public RemoteFileMetadata getAt(int row) { return data.get(row); }

    @Override public int getRowCount() { return data.size(); }
    @Override public int getColumnCount() { return cols.length; }
    @Override public String getColumnName(int c) { return cols[c]; }

    @Override public Object getValueAt(int row, int col) {
        RemoteFileMetadata m = data.get(row);
        switch (col) {
            case 0: return m.getName();
            case 1: return fmt.format(m.getCreatedAt());
            case 2: return fmt.format(m.getModifiedAt());
            case 3: return m.getUploadedBy();
            case 4: return m.getEditedBy();
            case 5: return m.getSize();
            default: return "";
        }
    }
}
