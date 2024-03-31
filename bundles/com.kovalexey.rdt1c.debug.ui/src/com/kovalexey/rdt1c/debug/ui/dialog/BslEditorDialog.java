package com.kovalexey.rdt1c.debug.ui.dialog;

import org.eclipse.emf.common.util.URI;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import com._1c.g5.v8.dt.bsl.resource.BslResource;
import com._1c.g5.v8.dt.debug.core.model.IBslStackFrame;
import com.kovalexey.rdt1c.debug.ui.utils.DebugCommandExecutor;

public class BslEditorDialog extends Dialog {

	private BslDebugTextEditor editor;
	private URI resourceUri;
	private IBslStackFrame bslStackFrame;

	public BslEditorDialog(Shell parentShell, URI resourceURI, IBslStackFrame bslStackFrame) {
		super(parentShell);
		this.resourceUri = resourceURI;
		this.bslStackFrame = bslStackFrame;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		
		Composite composite = new Composite(parent, 2048);
		composite.setDragDetect(true); 
		composite.setLayoutData(new GridData(4, 4, true, true, 1, 1));
		
		GridLayoutFactory.fillDefaults().applyTo(composite);
		
		editor = new BslDebugTextEditor(composite, this.resourceUri);
        editor.createPartialEditor();      
        
        return composite;
		
	}
	
	@Override
	protected void okPressed() {
		String text = editor.getEditorText();
		DebugCommandExecutor.ExecuteCode(bslStackFrame, text);
		super.okPressed();
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected Point getInitialSize() {
		return new Point(500, 500);
	}
	
	
	
	
}
