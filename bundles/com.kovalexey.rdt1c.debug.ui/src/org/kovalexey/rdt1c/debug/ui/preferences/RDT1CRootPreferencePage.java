package org.kovalexey.rdt1c.debug.ui.preferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class RDT1CRootPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	public RDT1CRootPreferencePage() {
		noDefaultAndApplyButton();
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		Label label = new Label(composite, SWT.NONE);
		label.setText("Настройки плагина RDT1C.\nВыберите подкатегорию слева.");
		return composite;
	}

}
