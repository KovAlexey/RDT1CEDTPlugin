package com.kovalexey.rdt1c.debug.ui;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

import com._1c.g5.v8.dt.debug.core.model.IBslVariable;

public class DebugElementAction implements IViewActionDelegate {
	ISelection selection;
	public DebugElementAction() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run(IAction action) {
		if (!selection.isEmpty()
				&& selection instanceof TreeSelection)
		{
			TreeSelection var_selection = (TreeSelection)selection;
			Object obj = var_selection.getFirstElement();
			if (obj instanceof IBslVariable)
			{
				final IBslVariable variable = (IBslVariable)obj;
				DebugCommandExecutor.DebugThisVariable(variable);
			}
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
