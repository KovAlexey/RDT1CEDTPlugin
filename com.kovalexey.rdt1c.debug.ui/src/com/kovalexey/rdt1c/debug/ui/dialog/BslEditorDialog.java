package com.kovalexey.rdt1c.debug.ui.dialog;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.internal.filesystem.FileStoreUtil;
import org.eclipse.core.internal.utils.FileUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.registry.EditorDescriptor;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.IResourceServiceProvider.Registry;
import org.eclipse.xtext.ui.editor.embedded.EmbeddedEditorFactory;
import org.eclipse.xtext.ui.editor.embedded.IEditedResourceProvider;
import org.eclipse.xtext.util.concurrent.IUnitOfWork;
import org.eclipse.xtext.validation.IResourceValidator;

import com._1c.g5.ides.ui.texteditor.xtext.embedded.CustomEmbeddedEditorBuilder;
import com._1c.g5.v8.dt.bsl.resource.BslResource;
import com._1c.g5.v8.dt.bsl.ui.editor.BslXtextEditor;
import com._1c.g5.v8.dt.core.platform.IV8Project;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.core.xtext.resource.ExtendedResourceServiceProvider;
import com._1c.g5.v8.dt.lcore.ui.editor.embedded.CustomEmbeddedEditorResourceProvider;
import com._1c.g5.v8.dt.lcore.ui.editor.embedded.CustomModelAccessAwareEmbeddedEditorBuilder.CustomModelAccessAwareEmbeddedEditor;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.kovalexey.rdt1c.debug.ui.Activator;

public class BslEditorDialog extends Dialog {

	private IV8Project project;
	private BslTestEditor editor;

	@Override
	protected Control createDialogArea(Composite parent) {
		
		Composite composite = new Composite(parent, 2048);
		composite.setDragDetect(true); 
		composite.setLayoutData(new GridData(4, 4, true, true, 1, 1));
		
		GridLayoutFactory.fillDefaults().applyTo(composite);
		
		editor = new BslTestEditor(composite, project.getProject());

        editor.getEditor().getViewer().getControl().setLayoutData(new GridData(4, 4, true, true, 1, 1));
        editor.getEditor().createPartialEditor(true);
        StringBuilder prefix = new StringBuilder();
        
        
        return composite;
		
	}

	
	
	@Override
	protected boolean isResizable() {
		// TODO Auto-generated method stub
		return true;
	}



	@Override
	protected Point getInitialSize() {
		// TODO Auto-generated method stub
		return new Point(500, 500);
	}

	
	public BslEditorDialog(Shell parentShell, IV8Project project) {
		super(parentShell);
		this.project = project;
	}
	
	
}
