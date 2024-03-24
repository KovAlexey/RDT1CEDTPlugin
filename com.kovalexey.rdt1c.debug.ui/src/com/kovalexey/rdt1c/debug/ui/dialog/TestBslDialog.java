package com.kovalexey.rdt1c.debug.ui.dialog;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.internal.filesystem.FileStoreUtil;
import org.eclipse.core.internal.utils.FileUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.registry.EditorDescriptor;

import com._1c.g5.v8.dt.bsl.ui.editor.BslXtextEditor;

public class TestBslDialog extends Dialog {

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = new Composite(parent, 2048);
		composite.setDragDetect(true); 
		composite.setLayoutData(new GridData());
		
		GridLayoutFactory.fillDefaults().applyTo(composite);
		
		try {
			File tempFile = com._1c.g5.v8.dt.common.FileUtil.createTempFile("debug", ".bsl");
			IFileStore filestore = EFS.getLocalFileSystem().getStore(new Path(tempFile.getPath()));
			try {
				//
				
				var descriptor = IDE.getEditorDescriptorForFileStore(filestore, false);
				EditorDescriptor editorDescriptor = (EditorDescriptor)descriptor;
				BslXtextEditor editor = (BslXtextEditor)editorDescriptor.createEditor();

				editor.createPartControl(parent);
				//IDE.openEditorOnFileStore(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), filestore);
				
			} catch (PartInitException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return super.createDialogArea(parent);
	}

	public TestBslDialog(Shell parentShell) {
		super(parentShell);
	}

}
