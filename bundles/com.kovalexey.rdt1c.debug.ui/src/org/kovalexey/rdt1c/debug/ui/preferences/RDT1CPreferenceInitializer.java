package org.kovalexey.rdt1c.debug.ui.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.kovalexey.rdt1c.debug.ui.RDT1CPlugin;

public class RDT1CPreferenceInitializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = RDT1CPlugin.getDefault().getPreferenceStore();
		store.setDefault(RDT1CPreferenceConstants.METHOD_TEMPLATE, "ИрОбщий.Ду");
		store.setDefault(RDT1CPreferenceConstants.DEBUG_METHOD_TEMPLATE, "ИрОбщий.От");
		store.setDefault(RDT1CPreferenceConstants.RESULT_VAR_NAME, "Р");
		store.setDefault(RDT1CPreferenceConstants.RESULT_VAR_USED, true);
		// Format: name:type:enabled;... 
		// Types: C (Code), S (Structure/Context), R (Reassigned)
		String defaultConfig = "Код:C:true;П1:S:true;П2:R:true;П3:R:true;П4:R:true;П5:R:true;П6:R:true;П7:R:true;П8:R:true;П9:R:true";
		store.setDefault(RDT1CPreferenceConstants.PARAMETERS_CONFIG, defaultConfig);
		
		store.setDefault(RDT1CPreferenceConstants.BSLDOC_MODE, "1");
		store.setDefault(RDT1CPreferenceConstants.BSLDOC_METHOD, "ОбщийМодуль.ИмяМетода");
		store.setDefault(RDT1CPreferenceConstants.BSLDOC_PARAM_MAP, "П1;П2;П3;П4;П5;П6;П7;П8;П9;П10");
	}
}

