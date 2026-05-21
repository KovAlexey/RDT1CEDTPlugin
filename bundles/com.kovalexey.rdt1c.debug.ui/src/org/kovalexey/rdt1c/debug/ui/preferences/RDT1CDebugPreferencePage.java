package org.kovalexey.rdt1c.debug.ui.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.kovalexey.rdt1c.debug.ui.RDT1CPlugin;

public class RDT1CDebugPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public RDT1CDebugPreferencePage() {
		super(GRID);
		setPreferenceStore(RDT1CPlugin.getDefault().getPreferenceStore());
		setDescription("Настройки отладки выражений RDT1C");
	}

	@Override
	public void createFieldEditors() {
		addField(new StringFieldEditor(RDT1CPreferenceConstants.DEBUG_METHOD_TEMPLATE, 
				"Шаблон метода отладки (вместо ИрОбщий.От):", getFieldEditorParent()));
	}

	@Override
	public void init(IWorkbench workbench) {
	}
}
