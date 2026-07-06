package org.kovalexey.rdt1c.debug.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.ui.editor.XtextEditor;
import org.eclipse.xtext.util.concurrent.IUnitOfWork;

import com._1c.g5.modeling.xtext.resource.IXtextResourceCleanerService;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.ui.menu.BslHandlerUtil;
import org.kovalexey.rdt1c.debug.ui.RDT1CPlugin;
import org.kovalexey.rdt1c.debug.ui.utils.Notification;

public class ForceRefreshModuleHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchPart part = HandlerUtil.getActivePart(event);
		XtextEditor editor = BslHandlerUtil.extractXtextEditor(part);

		if (editor != null) {
			IXtextResourceCleanerService xtextResourceCleanerService = RDT1CPlugin.getDefault().getBslInjector()
					.getInstance(IXtextResourceCleanerService.class);
			URI resourceUri = editor.getDocument().modify(new IUnitOfWork<URI, XtextResource>() {
				@Override
				public URI exec(XtextResource state) throws Exception {
					if (state.getContents().isEmpty()) {
						return null;
					}
					EObject root = state.getContents().get(0);
					if (root instanceof Module) {
						xtextResourceCleanerService.cleanUp(state);
						return state.getURI();
					}
					return null;
				}
			});
			if (resourceUri != null) {
				try {
					buildProject(resourceUri);
					Notification.showMessage("Контекст модуля принудительно обновлен.");
				} catch (CoreException e) {
					throw new ExecutionException("Не удалось запустить пересборку проекта.", e);
				}
			}
		}
		return null;
	}

	private void buildProject(URI resourceUri) throws CoreException {
		String platformPath = resourceUri.toPlatformString(true);
		if (platformPath == null) {
			return;
		}
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(platformPath));
		if (file != null && file.getProject() != null) {
			file.getProject().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, new NullProgressMonitor());
		}
	}

}
