package org.kovalexey.rdt1c.debug.ui.preferences;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
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

public class RDT1CParameterTableEditor extends FieldEditor {

	private TableViewer tableViewer;
	private List<ParameterEntry> parameters = new ArrayList<>();

	public static class ParameterEntry {
		public String name;
		public String type; // C, S, R
		public boolean enabled;

		public ParameterEntry(String name, String type, boolean enabled) {
			this.name = name;
			this.type = type;
			this.enabled = enabled;
		}

		public String toStringValue() {
			return name + ":" + type + ":" + enabled;
		}
	}

	public RDT1CParameterTableEditor(String name, String labelText, Composite parent) {
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
		gd.heightHint = 250;
		table.setLayoutData(gd);

		createColumns();

		tableViewer.setContentProvider(ArrayContentProvider.getInstance());
		tableViewer.setLabelProvider(new ParameterLabelProvider());
	}

	private void createColumns() {
		String[] titles = { "№", "Вкл", "Имя параметра", "Тип" };
		int[] bounds = { 40, 40, 150, 100 };

		// Index column
		TableViewerColumn col = new TableViewerColumn(tableViewer, SWT.NONE);
		col.getColumn().setWidth(bounds[0]);
		col.getColumn().setText(titles[0]);

		// Enabled column
		col = new TableViewerColumn(tableViewer, SWT.NONE);
		col.getColumn().setWidth(bounds[1]);
		col.getColumn().setText(titles[1]);
		col.setEditingSupport(new EditingSupport(tableViewer) {
			@Override
			protected boolean canEdit(Object element) {
				ParameterEntry entry = (ParameterEntry) element;
				return !entry.type.equals("C") && !entry.type.equals("S");
			}
			@Override
			protected CellEditor getCellEditor(Object element) {
				return new CheckboxCellEditor(tableViewer.getTable());
			}
			@Override
			protected Object getValue(Object element) {
				return ((ParameterEntry) element).enabled;
			}
			@Override
			protected void setValue(Object element, Object value) {
				((ParameterEntry) element).enabled = (Boolean) value;
				tableViewer.update(element, null);
			}
		});

		// Name column
		col = new TableViewerColumn(tableViewer, SWT.NONE);
		col.getColumn().setWidth(bounds[2]);
		col.getColumn().setText(titles[2]);
		col.setEditingSupport(new EditingSupport(tableViewer) {
			@Override
			protected boolean canEdit(Object element) { return true; }
			@Override
			protected CellEditor getCellEditor(Object element) {
				return new TextCellEditor(tableViewer.getTable());
			}
			@Override
			protected Object getValue(Object element) {
				return ((ParameterEntry) element).name;
			}
			@Override
			protected void setValue(Object element, Object value) {
				((ParameterEntry) element).name = (String) value;
				tableViewer.update(element, null);
			}
		});

		// Type column
		col = new TableViewerColumn(tableViewer, SWT.NONE);
		col.getColumn().setWidth(bounds[3]);
		col.getColumn().setText(titles[3]);
		col.setEditingSupport(new EditingSupport(tableViewer) {
			@Override
			protected boolean canEdit(Object element) { return true; }
			@Override
			protected CellEditor getCellEditor(Object element) {
				return new ComboBoxCellEditor(tableViewer.getTable(), new String[] { "Код", "Контекст", "Присваиваемое" });
			}
			@Override
			protected Object getValue(Object element) {
				ParameterEntry entry = (ParameterEntry) element;
				if (entry.type.equals("C")) return 0;
				if (entry.type.equals("S")) return 1;
				return 2;
			}
			@Override
			protected void setValue(Object element, Object value) {
				int index = (Integer) value;
				ParameterEntry entry = (ParameterEntry) element;
				String oldType = entry.type;
				String newType = index == 0 ? "C" : (index == 1 ? "S" : "R");
				
				if (!oldType.equals(newType)) {
					// Enforce uniqueness of Code and Context
					if (newType.equals("C") || newType.equals("S")) {
						for (ParameterEntry e : parameters) {
							if (e != entry && e.type.equals(newType)) {
								e.type = "R";
								tableViewer.update(e, null);
							}
						}
						entry.enabled = true;
					}
					entry.type = newType;
					tableViewer.update(element, null);
				}
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
		parameters.clear();
		if (value != null && !value.isEmpty()) {
			String[] parts = value.split(";");
			for (String part : parts) {
				String[] subParts = part.split(":");
				if (subParts.length == 3) {
					parameters.add(new ParameterEntry(subParts[0], subParts[1], Boolean.parseBoolean(subParts[2])));
				}
			}
		}
		// Ensure 10 rows
		while (parameters.size() < 10) {
			parameters.add(new ParameterEntry("П" + (parameters.size()), "R", false));
		}
		tableViewer.setInput(parameters);
	}

	@Override
	protected void doStore() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < parameters.size(); i++) {
			sb.append(parameters.get(i).toStringValue());
			if (i < parameters.size() - 1) {
				sb.append(";");
			}
		}
		getPreferenceStore().setValue(getPreferenceName(), sb.toString());
	}

	public int getNumberOfColumns() {
		return 1;
	}

	private class ParameterLabelProvider extends LabelProvider implements ITableLabelProvider {
		@Override
		public Image getColumnImage(Object element, int columnIndex) { return null; }
		@Override
		public String getColumnText(Object element, int columnIndex) {
			ParameterEntry entry = (ParameterEntry) element;
			switch (columnIndex) {
			case 0: return String.valueOf(parameters.indexOf(entry) + 1);
			case 1: return entry.enabled ? "X" : "";
			case 2: return entry.name;
			case 3:
				if (entry.type.equals("C")) return "Код";
				if (entry.type.equals("S")) return "Контекст";
				return "Присваиваемое";
			default: return "";
			}
		}
	}

	@Override
	public int getNumberOfControls() {
		return 1;
	}
}
