package com.kovalexey.rdt1c.debug.ui.dialog;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import com._1c.g5.v8.dt.bsl.resource.BslResource;

public class BslEditorDialog extends Dialog {

	private BslDebugTextEditor editor;
	private BslResource externalResourceSet;

	@Override
	protected Control createDialogArea(Composite parent) {
		
		Composite composite = new Composite(parent, 2048);
		composite.setDragDetect(true); 
		composite.setLayoutData(new GridData(4, 4, true, true, 1, 1));
		
		GridLayoutFactory.fillDefaults().applyTo(composite);
		
		editor = new BslDebugTextEditor(composite, this.externalResourceSet.getURI());
        editor.createPartialEditor();      
        editor.setResourceSet(this.externalResourceSet);
        
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
	
	
	public BslEditorDialog(Shell parentShell, BslResource resourceSet) {
		super(parentShell);
		this.externalResourceSet = resourceSet;
	}
	
	
}
