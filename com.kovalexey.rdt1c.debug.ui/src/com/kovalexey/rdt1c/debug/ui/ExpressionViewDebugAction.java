package com.kovalexey.rdt1c.debug.ui;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IWatchExpression;
import org.eclipse.debug.core.model.IWatchExpressionResult;
import org.eclipse.debug.internal.core.WatchExpression;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

import com._1c.g5.v8.dt.debug.core.model.IBslVariable;
import com._1c.g5.v8.dt.debug.core.model.values.IBslArrayValue;
import com._1c.g5.v8.dt.debug.core.model.values.IBslValue;


public class ExpressionViewDebugAction implements IViewActionDelegate {
	private ISelection selection;
	
	public ExpressionViewDebugAction() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run(IAction action) {
		if (!selection.isEmpty()
				&& selection instanceof TreeSelection) {
			TreeSelection treeSelection = (TreeSelection)selection;
			
			Object[] objects = treeSelection.toArray();
			if (objects.length > 2) {
				Notification.showMessage("");
				return;
			}
			
			HashMap<String, IBslValue> variables = new HashMap<String, IBslValue>();
			for (Object object : objects) {
				if (object instanceof IWatchExpression) {
					IWatchExpression selected_expression = (IWatchExpression)object;
					IValue value = selected_expression.getValue();
					if (value instanceof IBslValue) {
						IBslValue expression_result = (IBslValue) value;
						variables.put(selected_expression.getExpressionText(), expression_result);
					} 
				}
			}
			DebugCommandExecutor.DebugVariables(variables);
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
