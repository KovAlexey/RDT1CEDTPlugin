package com.kovalexey.rdt1c.debug.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.xtext.resource.EObjectAtOffsetHelper;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.ui.editor.XtextEditor;
import org.eclipse.xtext.util.concurrent.IUnitOfWork;

import com._1c.g5.v8.dt.bsl.resource.BslResource;
import com._1c.g5.v8.dt.bsl.ui.menu.BslHandlerUtil;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com.google.inject.Inject;
import com.kovalexey.rdt1c.debug.ui.dialog.BslEditorDialog;

public class OpenBslEditorDialogHandler extends AbstractHandler {
	@Inject
	IV8ProjectManager manager;
	@Inject
	private EObjectAtOffsetHelper objectAtOffsetHelper;
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchPart part = HandlerUtil.getActivePart(event);
		XtextEditor editor = BslHandlerUtil.extractXtextEditor(part);
		BslResource resourceSetCurrent = editor.getDocument().readOnly(new IUnitOfWork<BslResource, XtextResource>() {

			@Override
			public BslResource exec(XtextResource state) throws Exception {
				return (BslResource)state;
			}
		});
		BslEditorDialog dialog = new BslEditorDialog(HandlerUtil.getActiveShell(event), resourceSetCurrent);
		dialog.open();
		return null;
	}

}
