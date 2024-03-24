package com.kovalexey.rdt1c.debug.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.ui.handlers.HandlerUtil;

import com.kovalexey.rdt1c.debug.ui.dialog.TestBslDialog;

public class TestDialogHandler extends AbstractHandler implements IHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		TestBslDialog dialog = new TestBslDialog(HandlerUtil.getActiveShell(event));
		dialog.open();
		return null;
	}

}
