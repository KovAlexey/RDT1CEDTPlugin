package com.kovalexey.rdt1c.debug.ui;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.EObjectAtOffsetHelper;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.ui.editor.XtextEditor;
import org.eclipse.xtext.ui.editor.model.IXtextDocument;
import org.eclipse.xtext.util.concurrent.IUnitOfWork;

import com._1c.g5.v8.dt.bsl.model.DynamicFeatureAccess;
import com._1c.g5.v8.dt.bsl.model.StaticFeatureAccess;
import com._1c.g5.v8.dt.bsl.ui.menu.BslHandlerUtil;
import com._1c.g5.v8.dt.debug.core.model.IBslStackFrame;
import com.google.inject.Inject;

public class DebugBSLTextVariableCommandHandler extends AbstractHandler {
	
	@Inject
	private EObjectAtOffsetHelper objectAtOffsetHelper;
	
	private String getVariableNameFromOffset(IXtextDocument document, int offset) {		
		String variableName = document.readOnly(new IUnitOfWork<String, XtextResource>(){

			@Override
			public String exec(XtextResource state) throws Exception {
				Object semanticObject = objectAtOffsetHelper.resolveElementAt(state, offset);
				return getSemanticFullName(semanticObject);
			}
			
		});
		
		return variableName;
	}
	
	private String getSemanticFullName(Object semanticObject) {
		StringBuilder builder = new StringBuilder();
		String node_str = "";
		
		if (semanticObject instanceof DynamicFeatureAccess) {
			DynamicFeatureAccess dynamicSemanticObject = (DynamicFeatureAccess)semanticObject;
				
			node_str = NodeModelUtils.compactDump(NodeModelUtils.findActualNodeFor(dynamicSemanticObject), true);
		
			builder.insert(0, dynamicSemanticObject.getName());
			builder.insert(0, ".");
			builder.insert(0, getSemanticFullName(dynamicSemanticObject.getSource()));
			
		} else if (semanticObject instanceof StaticFeatureAccess) {
			builder.insert(0, ((StaticFeatureAccess)semanticObject).getName());
			node_str = NodeModelUtils.compactDump(NodeModelUtils.findActualNodeFor((EObject) semanticObject), true);
		}
		
		System.out.println(builder.toString());
		System.out.println(node_str);
		return builder.toString();
	}
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchPart part = HandlerUtil.getActivePart(event);
		XtextEditor editor = BslHandlerUtil.extractXtextEditor(part);
		ITextSelection selection = (ITextSelection)editor.getSelectionProvider().getSelection();
		
		IAdaptable debugContext = DebugUITools.getDebugContext();
		if (debugContext == null) {
			Notification.showMessage("Не запущена отладка.");
			return null;
		}

		if (!(debugContext instanceof IBslStackFrame)) {
			return null;
		}
		
		IBslStackFrame bslStackFrame = (IBslStackFrame) debugContext;
				
		if (editor != null) {
			IXtextDocument doc = editor.getDocument();
			
			if (selection.getLength() == 0) {
				String variable_name = getVariableNameFromOffset(doc, selection.getOffset());
				DebugCommandExecutor.DebugThisVariable(bslStackFrame, variable_name);
			} else {
				String selectedText = selection.getText();
				DebugCommandExecutor.DebugThisVariable(bslStackFrame, selectedText);
			}
		}
		return null;
	}



}
