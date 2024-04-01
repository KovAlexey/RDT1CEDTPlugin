package org.kovalexey.rdt1c.debug.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.emf.common.util.URI;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.xtext.resource.EObjectAtOffsetHelper;
import org.eclipse.xtext.resource.IResourceFactory;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.ui.editor.XtextEditor;
import org.eclipse.xtext.util.concurrent.IUnitOfWork;
import org.kovalexey.rdt1c.debug.ui.dialog.BslEditorDialog;
import org.kovalexey.rdt1c.debug.ui.utils.Notification;

import com._1c.g5.v8.dt.bsl.ui.menu.BslHandlerUtil;
import com._1c.g5.v8.dt.core.platform.IBmModelManager;
import com._1c.g5.v8.dt.core.platform.IConfigurationProvider;
import com._1c.g5.v8.dt.core.platform.IDtProjectManager;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.debug.core.model.IBslStackFrame;
import com.google.inject.Inject;

public class OpenBslEditorDialogHandler extends AbstractHandler {
	@Inject
	IV8ProjectManager manager;
	@Inject
	private EObjectAtOffsetHelper objectAtOffsetHelper;
	@Inject
	private IBmModelManager modelManager;
	@Inject
	private IDtProjectManager dtProjectManager;

    @Inject
    private IConfigurationProvider configurationProvider;
    @Inject
    private IV8ProjectManager projectManager;
    private IResourceFactory resourceFactory;

	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchPart part = HandlerUtil.getActivePart(event);
		XtextEditor editor = BslHandlerUtil.extractXtextEditor(part);
		
		URI uri = editor.getDocument().readOnly(new IUnitOfWork<URI, XtextResource>() {
			@Override
			public URI exec(XtextResource state) throws Exception {
				return state.getURI();
			}
		});
		
		IAdaptable debugContext = DebugUITools.getDebugContext();
		if (debugContext == null) {
			Notification.showMessage("Не запущена отладка.");
			return null;
		}		

		if (!(debugContext instanceof IBslStackFrame)) {
			return null;
		}
		
		IBslStackFrame bslStackFrame = (IBslStackFrame) debugContext;
		BslEditorDialog dialog = new BslEditorDialog(HandlerUtil.getActiveShell(event), uri, bslStackFrame);
		dialog.open();
		
		return null;
	}

}
