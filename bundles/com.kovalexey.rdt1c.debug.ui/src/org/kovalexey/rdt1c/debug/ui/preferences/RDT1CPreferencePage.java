package org.kovalexey.rdt1c.debug.ui.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.kovalexey.rdt1c.debug.ui.RDT1CPlugin;

public class RDT1CPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private BooleanFieldEditor resultUsedEditor;
	private StringFieldEditor resultVarNameEditor;

	public RDT1CPreferencePage() {
		super(GRID);
		setPreferenceStore(RDT1CPlugin.getDefault().getPreferenceStore());
		setDescription("Настройки выполнения выражений RDT1C");
	}

	@Override
	public void createFieldEditors() {
		addField(new StringFieldEditor(RDT1CPreferenceConstants.METHOD_TEMPLATE, 
				"Шаблон метода:", getFieldEditorParent()));

		resultUsedEditor = new BooleanFieldEditor(RDT1CPreferenceConstants.RESULT_VAR_USED,
				"Использовать переменную результата", getFieldEditorParent()) {
			@Override
			protected void valueChanged(boolean oldValue, boolean newValue) {
				super.valueChanged(oldValue, newValue);
				resultVarNameEditor.setEnabled(newValue, getFieldEditorParent());
			}
		};
		addField(resultUsedEditor);

		resultVarNameEditor = new StringFieldEditor(RDT1CPreferenceConstants.RESULT_VAR_NAME, 
				"Имя переменной результата:", getFieldEditorParent());
		addField(resultVarNameEditor);

		addField(new RDT1CParameterTableEditor(RDT1CPreferenceConstants.PARAMETERS_CONFIG,
				"Конфигурация параметров:", getFieldEditorParent()));
	}

	@Override
	protected void initialize() {
		super.initialize();
		resultVarNameEditor.setEnabled(getPreferenceStore().getBoolean(RDT1CPreferenceConstants.RESULT_VAR_USED), getFieldEditorParent());
	}

	@Override
	public void init(IWorkbench workbench) {
	}
}
