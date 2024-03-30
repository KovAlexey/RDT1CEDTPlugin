package com.kovalexey.rdt1c.debug.ui.actions;

import java.util.ArrayList;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

import com._1c.g5.v8.dt.debug.core.model.IBslVariable;
import com.kovalexey.rdt1c.debug.ui.utils.DebugCommandExecutor;

public class VariableViewDebugAction implements IViewActionDelegate {
	ISelection selection;

	
	
	public VariableViewDebugAction() {
	}

	@Override
	public void run(IAction action) {
		if (!selection.isEmpty()
				&& selection instanceof TreeSelection)
		{
			TreeSelection var_selection = (TreeSelection)selection;
			
			Object[] objects = var_selection.toArray();
			ArrayList<IBslVariable> variables = new ArrayList<IBslVariable>();
			for (Object object: objects) {
				if (object instanceof IBslVariable) {
					variables.add((IBslVariable)object);
				}
			}
			
			DebugCommandExecutor.DebugVariables(variables.toArray(size -> new IBslVariable[size]));
		}

	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
	}

	@Override
	public void init(IViewPart view) {
	}

}
