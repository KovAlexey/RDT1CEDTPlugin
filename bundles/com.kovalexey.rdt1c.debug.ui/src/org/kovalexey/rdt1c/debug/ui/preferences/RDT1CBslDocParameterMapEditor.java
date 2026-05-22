package org.kovalexey.rdt1c.debug.ui.preferences;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

public class RDT1CBslDocParameterMapEditor extends FieldEditor {

	private TableViewer tableViewer;
	private List<MapEntry> entries = new ArrayList<>();

	public static class MapEntry {
		public int number;
		public String keyName;

		public MapEntry(int number, String keyName) {
			this.number = number;
			this.keyName = keyName;
		}
	}

	public RDT1CBslDocParameterMapEditor(String name, String labelText, Composite parent) {
		super(name, labelText, parent);
	}

	@Override
	protected void adjustForNumColumns(int numColumns) {
		GridData gd = (GridData) tableViewer.getControl().getLayoutData();
		gd.horizontalSpan = numColumns;
	}

	@Override
	protected void doFillIntoGrid(Composite parent, int numColumns) {
		tableViewer = new TableViewer(parent, SWT.BORDER | SWT.FULL_SELECTION);
		Table table = tableViewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = numColumns;
		gd.heightHint = 200;
		table.setLayoutData(gd);

		createColumns();

		tableViewer.setContentProvider(ArrayContentProvider.getInstance());
		tableViewer.setLabelProvider(new MapLabelProvider());
	}

	private void createColumns() {
		String[] titles = { "№", "Имя ключа в структуре/соответствии BSL" };
		int[] bounds = { 50, 250 };

		// Number column (№)
		TableViewerColumn colNum = new TableViewerColumn(tableViewer, SWT.NONE);
		colNum.getColumn().setWidth(bounds[0]);
		colNum.getColumn().setText(titles[0]);

		// KeyName column
		TableViewerColumn colKey = new TableViewerColumn(tableViewer, SWT.NONE);
		colKey.getColumn().setWidth(bounds[1]);
		colKey.getColumn().setText(titles[1]);
		colKey.setEditingSupport(new EditingSupport(tableViewer) {
			@Override
			protected boolean canEdit(Object element) {
				return true;
			}
			@Override
			protected CellEditor getCellEditor(Object element) {
				return new TextCellEditor(tableViewer.getTable());
			}
			@Override
			protected Object getValue(Object element) {
				return ((MapEntry) element).keyName;
			}
			@Override
			protected void setValue(Object element, Object value) {
				((MapEntry) element).keyName = (String) value;
				tableViewer.update(element, null);
			}
		});
	}

	@Override
	protected void doLoad() {
		String value = getPreferenceStore().getString(getPreferenceName());
		loadFromString(value);
	}

	@Override
	protected void doLoadDefault() {
		String value = getPreferenceStore().getDefaultString(getPreferenceName());
		loadFromString(value);
	}

	private void loadFromString(String value) {
		entries.clear();
		String[] parts = (value != null && !value.isEmpty()) ? value.split(";") : new String[0];
		for (int i = 0; i < 10; i++) {
			String keyName = (i < parts.length && parts[i] != null && !parts[i].isEmpty()) ? parts[i] : "П" + (i + 1);
			entries.add(new MapEntry(i + 1, keyName));
		}
		tableViewer.setInput(entries);
	}

	@Override
	protected void doStore() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < entries.size(); i++) {
			sb.append(entries.get(i).keyName);
			if (i < entries.size() - 1) {
				sb.append(";");
			}
		}
		getPreferenceStore().setValue(getPreferenceName(), sb.toString());
	}

	@Override
	public int getNumberOfControls() {
		return 1;
	}

	public TableViewer getTableViewer() {
		return tableViewer;
	}

	private class MapLabelProvider extends LabelProvider implements ITableLabelProvider {
		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}
		@Override
		public String getColumnText(Object element, int columnIndex) {
			MapEntry entry = (MapEntry) element;
			switch (columnIndex) {
			case 0:
				return String.valueOf(entry.number);
			case 1:
				return entry.keyName;
			default:
				return "";
			}
		}
	}
}
