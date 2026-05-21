package org.kovalexey.rdt1c.debug.ui.handlers;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.EObjectAtOffsetHelper;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.ui.editor.XtextEditor;
import org.eclipse.xtext.ui.editor.model.IXtextDocument;
import org.eclipse.xtext.util.concurrent.IUnitOfWork;
import org.kovalexey.rdt1c.debug.ui.utils.BslDocGenerator;
import org.kovalexey.rdt1c.debug.ui.utils.Notification;

import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.ui.menu.BslHandlerUtil;
import com._1c.g5.v8.dt.debug.core.model.IBslStackFrame;
import com._1c.g5.v8.dt.debug.core.model.IBslVariable;
import com._1c.g5.v8.dt.debug.core.model.values.IBslValue;
import com.google.inject.Inject;

public class GenerateBslDocCommandHandler extends AbstractHandler {

	@Inject
	private EObjectAtOffsetHelper objectAtOffsetHelper;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchPart part = HandlerUtil.getActivePart(event);
		XtextEditor editor = BslHandlerUtil.extractXtextEditor(part);
		if (editor == null) {
			return null;
		}

		IAdaptable debugContext = DebugUITools.getDebugContext();
		if (debugContext == null) {
			Notification.showMessage("Отладка не запущена или не приостановлена на точке останова.");
			return null;
		}
		IBslStackFrame bslStackFrame = debugContext.getAdapter(IBslStackFrame.class);
		if (bslStackFrame == null) {
			Notification.showMessage("Отладка не запущена или не приостановлена на точке останова.");
			return null;
		}

		IXtextDocument doc = editor.getDocument();
		ITextSelection selection = (ITextSelection) editor.getSelectionProvider().getSelection();
		int offset = selection.getOffset();

		// Obtain the method AST node
		Method method = doc.readOnly(new IUnitOfWork<Method, XtextResource>() {
			@Override
			public Method exec(XtextResource state) throws Exception {
				EObject current = objectAtOffsetHelper.resolveElementAt(state, offset);
				while (current != null && !(current instanceof Method)) {
					current = current.eContainer();
				}
				return (Method) current;
			}
		});

		if (method == null) {
			Notification.showMessage("Не найден метод под курсором.");
			return null;
		}

		// Find the node to locate the method's start offset
		Integer methodOffset = doc.readOnly(new IUnitOfWork<Integer, XtextResource>() {
			@Override
			public Integer exec(XtextResource state) throws Exception {
				INode node = NodeModelUtils.findActualNodeFor(method);
				return node != null ? node.getOffset() : null;
			}
		});

		if (methodOffset == null) {
			return null;
		}

		// Extract variables from stack frame
		Map<String, IBslValue> paramValues = new HashMap<>();
		try {
			org.eclipse.debug.core.model.IVariable[] variables = bslStackFrame.getVariables();
			if (variables != null) {
				for (org.eclipse.debug.core.model.IVariable var : variables) {
					if (var instanceof IBslVariable) {
						IBslVariable bslVar = (IBslVariable) var;
						String varName = bslVar.getName();
						if (varName != null) {
							paramValues.put(varName, bslVar.getValue());
							paramValues.put(varName.toLowerCase(), bslVar.getValue());
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Generate BSL-Doc comment block
		String comment = BslDocGenerator.generateDoc(method, paramValues);
		if (comment == null || comment.isEmpty()) {
			return null;
		}

		// Update the document: replace or insert the comment block
		try {
			int methodLine = doc.getLineOfOffset(methodOffset);
			int insertOffset = doc.getLineOffset(methodLine);

			int currentLine = methodLine - 1;
			int commentStartOffset = insertOffset;
			boolean foundComment = false;

			while (currentLine >= 0) {
				int lineOffset = doc.getLineOffset(currentLine);
				int lineLen = doc.getLineLength(currentLine);
				String lineText = doc.get(lineOffset, lineLen).trim();
				if (lineText.startsWith("//")) {
					commentStartOffset = lineOffset;
					foundComment = true;
					currentLine--;
				} else if (lineText.isEmpty()) {
					if (foundComment) {
						commentStartOffset = lineOffset;
						currentLine--;
					} else {
						currentLine--;
					}
				} else {
					break;
				}
			}

			int replaceOffset;
			int replaceLength;
			if (foundComment) {
				replaceOffset = commentStartOffset;
				replaceLength = insertOffset - commentStartOffset;
			} else {
				replaceOffset = insertOffset;
				replaceLength = 0;
			}

			doc.replace(replaceOffset, replaceLength, comment);
		} catch (Exception e) {
			e.printStackTrace();
			Notification.showMessage("Ошибка при вставке документирующего комментария: " + e.getMessage());
		}

		return null;
	}
}
