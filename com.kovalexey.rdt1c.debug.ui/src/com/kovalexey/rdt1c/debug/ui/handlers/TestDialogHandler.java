package com.kovalexey.rdt1c.debug.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.ui.handlers.HandlerUtil;

import com._1c.g5.v8.dt.core.platform.IV8Project;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com.google.inject.Inject;
import com.kovalexey.rdt1c.debug.ui.dialog.BslEditorDialog;

public class TestDialogHandler extends AbstractHandler implements IHandler {
	@Inject
	IV8ProjectManager manager;
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		BslEditorDialog dialog = new BslEditorDialog(HandlerUtil.getActiveShell(event), (IV8Project)manager.getProjects().toArray()[0]);
		dialog.open();
		return null;
	}

}
