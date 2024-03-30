package com.kovalexey.rdt1c.debug.ui.utils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.eclipse.debug.core.DebugException;

import com._1c.g5.v8.dt.debug.core.model.IBslStackFrame;
import com._1c.g5.v8.dt.debug.core.model.IBslVariable;
import com._1c.g5.v8.dt.debug.core.model.evaluation.EvaluationRequest;
import com._1c.g5.v8.dt.debug.core.model.evaluation.IEvaluationListener;
import com._1c.g5.v8.dt.debug.core.model.evaluation.IEvaluationRequest;
import com._1c.g5.v8.dt.debug.core.model.evaluation.IEvaluationResult;
import com._1c.g5.v8.dt.debug.core.model.values.BslValuePath;
import com._1c.g5.v8.dt.debug.core.model.values.IBslValue;
import com._1c.g5.v8.dt.debug.model.calculations.BaseValueInfoData;
import com._1c.g5.v8.dt.debug.model.calculations.CalculationResultBaseData;
import com._1c.g5.v8.dt.debug.model.calculations.ViewInterface;
import com._1c.g5.v8.dt.platform.IEObjectTypeNames;

public class DebugCommandExecutor {
	public static final String TYPE_DATA_COMPOSITION_SCHEME_RU = "СхемаКомпоновкиДанных";
	public static final String DATA_COMPOSITION_SETTINGS_RU = "НастройкиКомпоновкиДанных";
	
	
	
	public static void DebugThisVariable(IBslStackFrame stackframe, String variable) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("ИрОбщий.От(");
		stringBuilder.append(variable);
		stringBuilder.append(")");
		
		String executionCommand = stringBuilder.toString();
		try {
			EvaluateExpression(stackframe, executionCommand);
		} catch(DebugException e) {
			// TODO: Обработка исключения
			e.printStackTrace();
		}
	}
	
	public static void DebugThisVariable(IBslVariable var) {
		DebugThisVariable(var.getStackFrame(), var.toWatchExpression());
	}
	
	public static void DebugVariables(IBslVariable[] variables) {
		if (variables.length > 2) {
			Notification.showMessage("Не поддерживается более двух параметров!");
			return;
		} else if (variables.length == 2) {
			executeDebugDataCompositionSchemeFromSelected(variables);
		} else if (variables.length == 1) {
			DebugThisVariable(variables[0]);
		}
	}
	
	public static void DebugVariables(HashMap<String, IBslValue> variables) {
		if (variables.size() > 2) {
			Notification.showMessage("Не поддерживается более двух параметров!");
			return;
		} else if (variables.size() == 2) {
			executeDebugDataCompositionSchemeFromSelected(variables);
		} else if (variables.size() == 1) {
			String expression = (String)variables.keySet().toArray()[0];
			IBslValue value = variables.get(expression);
			DebugThisVariable(value.getStackFrame(), expression);
		}
	}
	
	public static Boolean IsItBslValueDataComposition(IBslValue value) {
		String typeName = value.getValueTypeName();
		return (typeName.equals(TYPE_DATA_COMPOSITION_SCHEME_RU) 
				|| typeName.equals(IEObjectTypeNames.DATA_COMPOSITION_SCHEMA));
	}
	
	public static Boolean IsItBslValueDataCompositionSettings(IBslValue value) {
		String typeName = value.getValueTypeName();
		return (typeName.equals(DATA_COMPOSITION_SETTINGS_RU) 
				|| typeName.equals(IEObjectTypeNames.DATA_COMPOSITION_SETTINGS));
	}
	
	private static void executeDebugDataCompositionSchemeFromSelected(HashMap<String, IBslValue> variables) {
		String variable_scheme = null, variable_settings = null;
		IBslStackFrame stackFrame = null;
		
		Iterator<String> keys_iterator = variables.keySet().iterator();
		while (keys_iterator.hasNext()) {
			String expression = keys_iterator.next();
			IBslValue value = variables.get(expression);
			if (IsItBslValueDataComposition(value)) {
				variable_scheme = expression;
			} else if (IsItBslValueDataCompositionSettings(value)) {
				variable_settings = expression;
				stackFrame = value.getStackFrame();
			}
		}
		
		if (stackFrame != null && variable_scheme != null && variable_settings != null)
		{
			DebugDataCompostionScheme(stackFrame, variable_scheme, variable_settings);
		} else {
			Notification.showMessage("Выбранные значения не являются схемой компоновки данных с настройками!");
		}
	}
	
	private static void executeDebugDataCompositionSchemeFromSelected(IBslVariable[] variables) {
		IBslVariable variable_scheme = null, variable_settings = null;
		
		for (IBslVariable object : variables) {
			IBslValue value = object.getValue();
			if (IsItBslValueDataComposition(value)) {
				variable_scheme = object;
			} else if (IsItBslValueDataCompositionSettings(value)) {
				variable_settings = object;
			}
		}
		
		if (variable_scheme != null && variable_settings != null)
		{
			DebugDataCompostionScheme(variable_scheme, variable_settings);
		} else {
			Notification.showMessage("Выбранные значения не являются схемой компоновки данных с настройками!");
		}
	}
	
	public static void DebugDataCompostionScheme(IBslVariable scheme, IBslVariable settings) {
		DebugDataCompostionScheme(scheme.getStackFrame(), scheme.toWatchExpression(), settings.toWatchExpression());
	}
	
	public static void DebugDataCompostionScheme(IBslStackFrame stackFrame, String scheme_expression, String settings_expression) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("ИрОбщий.От(");
		stringBuilder.append(scheme_expression);
		stringBuilder.append(",");
		stringBuilder.append(settings_expression);
		stringBuilder.append(")");
		
		String executionCommand = stringBuilder.toString();
		try {
			EvaluateExpression(stackFrame, executionCommand);
		} catch(DebugException e) {
			// TODO: Обработка исключения
			e.printStackTrace();
		}
		
	}
	
	static void EvaluateExpression(IBslStackFrame stackFrame, String exression) throws DebugException {
		BslValuePath path = new BslValuePath(exression);
		List<ViewInterface> evaluationInterfaces = Collections.singletonList(ViewInterface.CONTEXT);
		UUID uuid = UUID.randomUUID();
		IEvaluationRequest request = EvaluationRequest.builder(path)
				.setStackFrame(stackFrame)
				.setExpressionUuid(uuid)
				.setInterfaces(evaluationInterfaces)
				.setMaxTestSize(4096)
				.setMultiLine(true)
				.setEvaluationListener(new DebugCommandExecutorListener(exression))
				.build();
		stackFrame.getDebugTarget().getEvaluationEngine().evaluateExpression(request);
	}
	
	static class DebugCommandExecutorListener implements IEvaluationListener {
		
		String expression;
		public DebugCommandExecutorListener(String expression) {
			super();
			this.expression = expression;
		}

		@Override
		public void evaluationComplete(IEvaluationResult execution_result) throws DebugException {
			if (!execution_result.isSuccess()) {
				return;
			}
			CalculationResultBaseData resultBaseData = execution_result.getResult();
			if (resultBaseData.getErrorOccurred()) {
				byte[] exception = resultBaseData.getExceptionStr();
				String result = new String(exception, StandardCharsets.UTF_8);
				Notification.showMessage(expression, result);
				return;
			}
			BaseValueInfoData valueInfo = resultBaseData.getResultValueInfo();
			
			byte[] val_b = valueInfo.getValueString();
			if (val_b == null) {
				return;
			}
			String result = new String(val_b, StandardCharsets.UTF_8);
			System.out.print(result);
			
			Notification.copyClipboard(result);
			
			StringBuilder builder = new StringBuilder();
			builder.append("\"");
			builder.append(result);
			builder.append("\"");
			builder.append("\r\n");
			builder.append("Значение было скопировано в буфер обмена.");
			
			Notification.showMessage(expression, builder.toString());
		}
		
	}
}
