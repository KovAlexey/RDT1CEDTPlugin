package org.kovalexey.rdt1c.debug.ui.handlers;

import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.util.UUID;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.EObjectAtOffsetHelper;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.ui.editor.XtextEditor;
import org.eclipse.xtext.ui.editor.model.IXtextDocument;
import org.eclipse.xtext.util.concurrent.IUnitOfWork;

import org.kovalexey.rdt1c.debug.ui.RDT1CPlugin;
import org.kovalexey.rdt1c.debug.ui.preferences.RDT1CPreferenceConstants;
import org.kovalexey.rdt1c.debug.ui.utils.BslDocGenerator;
import org.kovalexey.rdt1c.debug.ui.utils.Notification;

import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.FormalParam;
import com._1c.g5.v8.dt.bsl.model.Function;
import com._1c.g5.v8.dt.bsl.ui.menu.BslHandlerUtil;
import com._1c.g5.v8.dt.debug.core.model.IBslStackFrame;
import com._1c.g5.v8.dt.debug.core.model.IBslVariable;
import com._1c.g5.v8.dt.debug.core.model.values.IBslValue;
import com._1c.g5.v8.dt.debug.core.model.values.IBslValueFactory;
import com._1c.g5.v8.dt.debug.core.model.values.BslValuePath;
import com._1c.g5.v8.dt.debug.core.model.evaluation.EvaluationRequest;
import com._1c.g5.v8.dt.debug.core.model.evaluation.IEvaluationListener;
import com._1c.g5.v8.dt.debug.core.model.evaluation.IEvaluationRequest;
import com._1c.g5.v8.dt.debug.core.model.evaluation.IEvaluationResult;
import com._1c.g5.v8.dt.debug.model.calculations.BaseValueInfoData;
import com._1c.g5.v8.dt.debug.model.calculations.CalculationResultBaseData;
import com._1c.g5.v8.dt.debug.model.calculations.ViewInterface;
import com.google.inject.Inject;

@SuppressWarnings("restriction")
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

		IPreferenceStore store = RDT1CPlugin.getDefault().getPreferenceStore();
		String mode = store.getString(RDT1CPreferenceConstants.BSLDOC_MODE);
		if (mode == null || mode.isEmpty()) {
			mode = "1";
		}
		String methodName = store.getString(RDT1CPreferenceConstants.BSLDOC_METHOD);
		String paramMappingStr = store.getString(RDT1CPreferenceConstants.BSLDOC_PARAM_MAP);

		if ("2".equals(mode)) {
			// Mode 2: Call specified method with all parameters passed
			String expr = BslDocGenerator.generateEvaluationExpression(methodName, method);
			BslValuePath path = new BslValuePath(expr);
			java.util.List<ViewInterface> evaluationInterfaces = java.util.Collections.singletonList(ViewInterface.CONTEXT);
			UUID uuid = UUID.randomUUID();
			IEvaluationRequest request = EvaluationRequest.builder(path)
					.setStackFrame(bslStackFrame)
					.setExpressionUuid(uuid)
					.setInterfaces(evaluationInterfaces)
					.setMaxTestSize(4096)
					.setMultiLine(true)
					.setEvaluationListener(new IEvaluationListener() {
						@Override
						public void evaluationComplete(IEvaluationResult execution_result) throws DebugException {
							if (!execution_result.isSuccess()) {
								Notification.showMessage("Не удалось вычислить выражение: " + expr);
								return;
							}
							CalculationResultBaseData resultBaseData = execution_result.getResult();
							if (resultBaseData.getErrorOccurred()) {
								byte[] exception = resultBaseData.getExceptionStr();
								String result = new String(exception, java.nio.charset.StandardCharsets.UTF_8);
								Notification.showMessage("Исключение 1С при вычислении: " + result);
								return;
							}
							BaseValueInfoData valueInfo = resultBaseData.getResultValueInfo();
							if (valueInfo == null) {
								Notification.showMessage("Выражение не вернуло значения.");
								return;
							}

							IBslValue bslValue = createBslValue(bslStackFrame, path, uuid, valueInfo);

							Map<Integer, String> mapping = BslDocGenerator.parseParamMapping(paramMappingStr);
							String generatedComment = BslDocGenerator.generateDocFromBslValue(method, bslValue, mapping);

							if (generatedComment == null || generatedComment.trim().isEmpty()) {
								Notification.showMessage("Сгенерированный комментарий пуст.");
								return;
							}

							org.eclipse.swt.widgets.Display.getDefault().asyncExec(new Runnable() {
								@Override
								public void run() {
									insertCommentIntoDoc(doc, methodOffset, generatedComment);
								}
							});
						}
					})
					.build();
			try {
				bslStackFrame.getDebugTarget().getEvaluationEngine().evaluateExpression(request);
			} catch (Exception e) {
				e.printStackTrace();
				Notification.showMessage("Ошибка при запуске вычисления: " + e.getMessage());
			}
		} else if ("3".equals(mode)) {
			// Mode 3: Sequential async calls for each parameter
			java.util.List<FormalParam> paramsToEvaluate = new java.util.ArrayList<>();
			for (FormalParam p : method.getFormalParams()) {
				String name = p.getName();
				if (name != null && !name.trim().isEmpty()) {
					paramsToEvaluate.add(p);
				}
			}

			StringBuilder paramComments = new StringBuilder();
			evaluateParamsSequentially(bslStackFrame, method, methodName, paramsToEvaluate, 0, paramComments, doc, methodOffset);
		} else {
			// Mode 1: Default local variables based generation
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

			String comment = BslDocGenerator.generateDoc(method, paramValues);
			if (comment != null && !comment.isEmpty()) {
				insertCommentIntoDoc(doc, methodOffset, comment);
			}
		}

		return null;
	}

	private void insertCommentIntoDoc(IXtextDocument doc, int methodOffset, String comment) {
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
	}

	private void evaluateParamsSequentially(
			IBslStackFrame stackFrame,
			Method method,
			String methodName,
			java.util.List<FormalParam> params,
			int index,
			StringBuilder paramComments,
			IXtextDocument doc,
			int methodOffset) {

		if (index >= params.size()) {
			StringBuilder finalDoc = new StringBuilder();
			finalDoc.append("// <Описание ").append(method instanceof Function ? "функции" : "процедуры").append(">\n");
			finalDoc.append("//\n");
			if (paramComments.length() > 0) {
				finalDoc.append("// Параметры:\n");
				finalDoc.append(paramComments.toString());
			}
			if (method instanceof Function) {
				finalDoc.append("//\n// Возвращаемое значение:\n//  Произвольный - <Описание возвращаемого значения>\n");
			}

			String generatedComment = finalDoc.toString();

			org.eclipse.swt.widgets.Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					insertCommentIntoDoc(doc, methodOffset, generatedComment);
				}
			});
			return;
		}

		FormalParam param = params.get(index);
		String paramName = param.getName();
		String expr = methodName + "(" + paramName + ")";

		BslValuePath path = new BslValuePath(expr);
		java.util.List<ViewInterface> evaluationInterfaces = java.util.Collections.singletonList(ViewInterface.CONTEXT);
		UUID uuid = UUID.randomUUID();
		IEvaluationRequest request = EvaluationRequest.builder(path)
				.setStackFrame(stackFrame)
				.setExpressionUuid(uuid)
				.setInterfaces(evaluationInterfaces)
				.setMaxTestSize(4096)
				.setMultiLine(true)
				.setEvaluationListener(new IEvaluationListener() {
					@Override
					public void evaluationComplete(IEvaluationResult execution_result) throws DebugException {
						IBslValue bslValue = null;
						if (execution_result.isSuccess()) {
							CalculationResultBaseData resultBaseData = execution_result.getResult();
							if (resultBaseData != null && !resultBaseData.getErrorOccurred()) {
								BaseValueInfoData valueInfo = resultBaseData.getResultValueInfo();
								if (valueInfo != null) {
									bslValue = createBslValue(stackFrame, path, uuid, valueInfo);
								}
							}
						}

						String paramDoc = BslDocGenerator.generateParameterDoc(paramName, bslValue);
						paramComments.append(paramDoc);

						evaluateParamsSequentially(stackFrame, method, methodName, params, index + 1, paramComments, doc, methodOffset);
					}
				})
				.build();
		try {
			stackFrame.getDebugTarget().getEvaluationEngine().evaluateExpression(request);
		} catch (Exception e) {
			e.printStackTrace();
			paramComments.append("//  ").append(paramName).append(" - Произвольный\n");
			evaluateParamsSequentially(stackFrame, method, methodName, params, index + 1, paramComments, doc, methodOffset);
		}
	}

	private static IBslValue createBslValue(IBslStackFrame stackFrame, BslValuePath path, UUID uuid, BaseValueInfoData valueInfo) {
		try {
			org.osgi.framework.Bundle bundle = org.eclipse.core.runtime.Platform.getBundle("com._1c.g5.v8.dt.debug.core");
			if (bundle != null) {
				Class<?> factoryClass = bundle.loadClass("com._1c.g5.v8.dt.internal.debug.core.model.values.BslValueFactory");
				IBslValueFactory factory = (IBslValueFactory) factoryClass.getDeclaredConstructor().newInstance();
				return factory.createValue(stackFrame, path, uuid, valueInfo, null);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
