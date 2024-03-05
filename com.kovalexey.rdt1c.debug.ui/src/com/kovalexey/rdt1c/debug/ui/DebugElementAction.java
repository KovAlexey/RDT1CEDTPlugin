package com.kovalexey.rdt1c.debug.ui;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

import com._1c.g5.v8.dt.debug.core.model.IBslVariable;
import com._1c.g5.v8.dt.platform.IEObjectTypeNames;

public class DebugElementAction implements IViewActionDelegate {
	ISelection selection;
	public static final String TYPE_DATA_COMPOSITION_SCHEME_RU = "СхемаКомпоновкиДанных";
	public static final String DATA_COMPOSITION_SETTINGS_RU = "НастройкиКомпоновкиДанных";
	
	public DebugElementAction() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run(IAction action) {
		if (!selection.isEmpty()
				&& selection instanceof TreeSelection)
		{
			TreeSelection var_selection = (TreeSelection)selection;
			
			Object[] objects = var_selection.toArray();
			if (objects.length > 2) {
				Notification.showmessage("Не поддерживается более двух параметров!");
				return;
			} else if (objects.length == 2) {
				executeDebugDataCompositionSchemeFromSelected(objects);
			} else if (objects.length == 1) {
				Object obj = objects[0];
				if (obj instanceof IBslVariable)
				{
					final IBslVariable variable = (IBslVariable)obj;
					DebugCommandExecutor.DebugThisVariable(variable);
				}
			}
		}

	}

	private void executeDebugDataCompositionSchemeFromSelected(Object[] objects) {
		IBslVariable variable_scheme = null, variable_settings = null;
		for (Object object : objects) {
			if (object instanceof IBslVariable)
			{
				final IBslVariable variable = (IBslVariable)object;
				
				String type = variable.getValue().getValueTypeName();
				if (type.equals(TYPE_DATA_COMPOSITION_SCHEME_RU) 
						|| type.equals(IEObjectTypeNames.DATA_COMPOSITION_SCHEMA)) {
					variable_scheme = variable;
				}
				else if (type.equals(DATA_COMPOSITION_SETTINGS_RU)
						|| type.equals(IEObjectTypeNames.DATA_COMPOSITION_SETTINGS)) {
					variable_settings = variable;
				}
			}
		}
		
		if (variable_scheme != null && variable_settings != null)
		{
			DebugCommandExecutor.DebugDataCompostionScheme(variable_scheme, variable_settings);
		} else 
		{
			Notification.showmessage("Выбранные значения не являются схемой компоновки данных с настройками!");
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;

	}

	@Override
	public void init(IViewPart view) {
		// TODO Auto-generated method stub

	}

}
