package org.kovalexey.rdt1c.debug.ui.preferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.jface.preference.IPreferenceStore;
import org.kovalexey.rdt1c.debug.ui.RDT1CPlugin;

public class RDT1CBslDocPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private Button radioMode1;
	private Button radioMode2;
	private Button radioMode3;

	private Composite methodContainer;
	private Text txtMethod;

	private Composite tableContainer;
	private RDT1CBslDocParameterMapEditor tableEditor;

	public RDT1CBslDocPreferencePage() {
		setPreferenceStore(RDT1CPlugin.getDefault().getPreferenceStore());
		setDescription("Настройки генерации документирующих комментариев BSL-Doc");
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite mainComposite = new Composite(parent, SWT.NONE);
		mainComposite.setLayout(new GridLayout(1, false));
		mainComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

		// 1. Group for modes
		Group modeGroup = new Group(mainComposite, SWT.NONE);
		modeGroup.setText("Режим генерации BSL-Doc комментариев");
		modeGroup.setLayout(new GridLayout(1, false));
		modeGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		radioMode1 = new Button(modeGroup, SWT.RADIO);
		radioMode1.setText("По данным отладки (Режим 1)");

		radioMode2 = new Button(modeGroup, SWT.RADIO);
		radioMode2.setText("Вызов метода с передачей всех параметров (Режим 2)");

		radioMode3 = new Button(modeGroup, SWT.RADIO);
		radioMode3.setText("Последовательный вызов метода по каждому параметру (Режим 3)");

		// 2. Method container
		methodContainer = new Composite(mainComposite, SWT.NONE);
		GridLayout methodLayout = new GridLayout(2, false);
		methodLayout.marginWidth = 0;
		methodContainer.setLayout(methodLayout);
		methodContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label lblMethod = new Label(methodContainer, SWT.NONE);
		lblMethod.setText("Метод (ОбщийМодуль.ИмяМетода):");
		
		txtMethod = new Text(methodContainer, SWT.BORDER);
		txtMethod.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// 3. Table editor container
		tableContainer = new Composite(mainComposite, SWT.NONE);
		GridLayout tableLayout = new GridLayout(1, false);
		tableLayout.marginWidth = 0;
		tableContainer.setLayout(tableLayout);
		tableContainer.setLayoutData(new GridData(GridData.FILL_BOTH));

		tableEditor = new RDT1CBslDocParameterMapEditor(
				RDT1CPreferenceConstants.BSLDOC_PARAM_MAP,
				"Соответствие параметров (для восстановления имен параметров):",
				tableContainer
		);
		tableEditor.setPreferenceStore(getPreferenceStore());

		// Initialize values from preference store
		loadPreferences();

		// Add selection listeners to radio buttons
		SelectionAdapter radioListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateFieldsVisibility();
				mainComposite.layout(true, true);
			}
		};
		radioMode1.addSelectionListener(radioListener);
		radioMode2.addSelectionListener(radioListener);
		radioMode3.addSelectionListener(radioListener);

		updateFieldsVisibility();

		return mainComposite;
	}

	private void loadPreferences() {
		IPreferenceStore store = getPreferenceStore();
		String mode = store.getString(RDT1CPreferenceConstants.BSLDOC_MODE);
		if ("2".equals(mode)) {
			radioMode2.setSelection(true);
		} else if ("3".equals(mode)) {
			radioMode3.setSelection(true);
		} else {
			radioMode1.setSelection(true);
		}

		txtMethod.setText(store.getString(RDT1CPreferenceConstants.BSLDOC_METHOD));
		tableEditor.load();
	}

	private void updateFieldsVisibility() {
		boolean isMode2 = radioMode2.getSelection();
		boolean isMode3 = radioMode3.getSelection();

		// Method visibility: visible for Mode 2 & 3
		boolean showMethod = isMode2 || isMode3;
		methodContainer.setVisible(showMethod);
		((GridData) methodContainer.getLayoutData()).exclude = !showMethod;

		// Table visibility: visible only for Mode 2
		tableContainer.setVisible(isMode2);
		((GridData) tableContainer.getLayoutData()).exclude = !isMode2;
	}

	@Override
	protected void performDefaults() {
		IPreferenceStore store = getPreferenceStore();
		String mode = store.getDefaultString(RDT1CPreferenceConstants.BSLDOC_MODE);
		radioMode1.setSelection("1".equals(mode));
		radioMode2.setSelection("2".equals(mode));
		radioMode3.setSelection("3".equals(mode));

		txtMethod.setText(store.getDefaultString(RDT1CPreferenceConstants.BSLDOC_METHOD));
		tableEditor.loadDefault();

		updateFieldsVisibility();
		txtMethod.getParent().layout(true, true);
		super.performDefaults();
	}

	@Override
	public boolean performOk() {
		IPreferenceStore store = getPreferenceStore();
		String mode = "1";
		if (radioMode2.getSelection()) {
			mode = "2";
		} else if (radioMode3.getSelection()) {
			mode = "3";
		}
		store.setValue(RDT1CPreferenceConstants.BSLDOC_MODE, mode);
		store.setValue(RDT1CPreferenceConstants.BSLDOC_METHOD, txtMethod.getText());
		tableEditor.store();

		return super.performOk();
	}
}
