package org.kovalexey.rdt1c.debug.ui.utils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IVariable;

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
	
	private static class VariableAnalysis {
		public java.util.List<String> structureVars = new java.util.ArrayList<>();
		public java.util.List<String> assignedVars = new java.util.ArrayList<>();
	}

	private static VariableAnalysis analyzeVariables(IBslStackFrame stackFrame, String code) {
		VariableAnalysis result = new VariableAnalysis();
		String cleanCode = stripStringsAndComments(code);
		
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("[_a-zA-Zа-яА-ЯёЁ][_a-zA-Zа-яА-ЯёЁ0-9]*|[^\\s_a-zA-Zа-яА-ЯёЁ0-9]").matcher(cleanCode);
		java.util.List<String> tokens = new java.util.ArrayList<>();
		while (m.find()) {
			tokens.add(m.group());
		}
		
		java.util.Set<String> allUsedLower = new java.util.HashSet<>();
		java.util.Set<String> assignedLower = new java.util.HashSet<>();
		
		for (int i = 0; i < tokens.size(); i++) {
			String t = tokens.get(i);
			if (t.matches("[_a-zA-Zа-яА-ЯёЁ][_a-zA-Zа-яА-ЯёЁ0-9]*")) {
				allUsedLower.add(t.toLowerCase());
				if (i + 1 < tokens.size() && tokens.get(i + 1).equals("=")) {
					if (i == 0 || !tokens.get(i - 1).equals(".")) {
						assignedLower.add(t.toLowerCase());
					}
				}
			}
		}
		
		try {
			java.util.List<IVariable> variablesList = new java.util.ArrayList<>();
			Collections.addAll(variablesList, stackFrame.getVariables());
			Collections.addAll(variablesList, stackFrame.getModuleVariables());
			Collections.addAll(variablesList, stackFrame.getModuleProperties());
			
			for (IVariable v : variablesList) {
				String name = v.getName();
				String nameLower = name.toLowerCase();
				if (allUsedLower.contains(nameLower)) {
					if (assignedLower.contains(nameLower)) {
						if (!result.assignedVars.contains(name)) {
							result.assignedVars.add(name);
						}
					} else {
						if (!result.structureVars.contains(name)) {
							result.structureVars.add(name);
						}
					}
				}
			}
		} catch (DebugException e) {
			// ignore
		}
		
		return result;
	}

	private static String stripStringsAndComments(String code) {
		StringBuilder sb = new StringBuilder();
		boolean inString = false;
		for (int i = 0; i < code.length(); i++) {
			char c = code.charAt(i);
			if (inString) {
				if (c == '"') {
					if (i + 1 < code.length() && code.charAt(i + 1) == '"') {
						i++; // skip escaped quote
					} else {
						inString = false;
					}
				}
			} else {
				if (c == '"') {
					inString = true;
				} else if (c == '/' && i + 1 < code.length() && code.charAt(i + 1) == '/') {
					while (i < code.length() && code.charAt(i) != '\n' && code.charAt(i) != '\r') {
						i++;
					}
				} else {
					sb.append(c);
				}
			}
		}
		return sb.toString();
	}

	private static String prepareSingleLineCode(String code) {
		StringBuilder sb = new StringBuilder();
		boolean inString = false;
		for (int i = 0; i < code.length(); i++) {
			char c = code.charAt(i);
			if (inString) {
				if (c == '"') {
					if (i + 1 < code.length() && code.charAt(i + 1) == '"') {
						sb.append("\"\"");
						i++;
					} else {
						inString = false;
						sb.append('"');
					}
				} else if (c == '\n' || c == '\r') {
					sb.append(' ');
					while (i + 1 < code.length() && (code.charAt(i + 1) == '\n' || code.charAt(i + 1) == '\r')) {
						i++;
					}
					if (i + 1 < code.length() && code.charAt(i + 1) == '|') {
						i++;
					}
				} else {
					sb.append(c);
				}
			} else {
				if (c == '"') {
					inString = true;
					sb.append('"');
				} else if (c == '/' && i + 1 < code.length() && code.charAt(i + 1) == '/') {
					while (i < code.length() && code.charAt(i) != '\n' && code.charAt(i) != '\r') {
						i++;
					}
				} else if (c == '\n' || c == '\r') {
					sb.append(' ');
				} else {
					sb.append(c);
				}
			}
		}
		return sb.toString().trim();
	}

	public static void ExecuteCode(IBslStackFrame stackFrame, String code) {
		VariableAnalysis analysis = analyzeVariables(stackFrame, code);
		if (analysis.assignedVars.size() > 8) {
			Notification.showMessage("Превышено ограничение (макс. 8) на количество присваиваний переменным из контекста.");
			return;
		}

		StringBuilder wrappedCode = new StringBuilder();
		
		// Эта часть разбирает параметр
		for (String v : analysis.structureVars) {
			wrappedCode.append(v).append(" = П1.").append(v).append("; ");
		}
		for (int i = 0; i < analysis.assignedVars.size(); i++) {
			wrappedCode.append(analysis.assignedVars.get(i)).append(" = П").append(i + 2).append("; ");
		}
		
		// Это, Собственно, выполняемый код
		String singleLineCode = prepareSingleLineCode(code);
		if (!singleLineCode.isEmpty()) {
			wrappedCode.append(singleLineCode);
			if (!singleLineCode.endsWith(";")) {
				wrappedCode.append(";");
			}
			wrappedCode.append(" ");
		}
		
		// Это запаковка результата
		wrappedCode.append("Р = Новый Структура(); ");
		for (String v : analysis.structureVars) {
			wrappedCode.append("Р.Вставить(\"").append(v).append("\", ").append(v).append("); ");
		}
		for (String v : analysis.assignedVars) {
			wrappedCode.append("Р.Вставить(\"").append(v).append("\", ").append(v).append("); ");
		}
		
		for (int i = 0; i < analysis.assignedVars.size(); i++) {
			wrappedCode.append("П").append(i + 2).append(" = ").append(analysis.assignedVars.get(i)).append("; ");
		}

		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("ИрОбщий.Ду(");
		stringBuilder.append(CreateTextForExecure(wrappedCode.toString()));
		
		// П1 - Структура
		if (!analysis.structureVars.isEmpty()) {
			StringBuilder names = new StringBuilder();
			StringBuilder values = new StringBuilder();
			for (int i = 0; i < analysis.structureVars.size(); i++) {
				String name = analysis.structureVars.get(i);
				names.append(name);
				values.append(name);
				if (i < analysis.structureVars.size() - 1) {
					names.append(",");
					values.append(",");
				}
			}
			stringBuilder.append(", Новый Структура(\"").append(names.toString()).append("\", ").append(values.toString()).append(")");
		} else {
			stringBuilder.append(", Новый Структура()");
		}
		
		// П2..П9 - Присваиваемые параметры
		for (String v : analysis.assignedVars) {
			stringBuilder.append(", ").append(v);
		}
		
		stringBuilder.append(")");
		
		String executionCommand = stringBuilder.toString();
		try {
			EvaluateExpression(stackFrame, executionCommand);
		} catch(DebugException e) {
			Notification.showMessage("Ошибка при выполнении команды: " + e.getMessage());
		}
	}
	
	public static String CreateTextForExecure(String text) {
		StringBuilder builder = new StringBuilder();
		builder.append("\"");
		builder.append(text.replace("\"", "\"\""));
		builder.append("\"");
		
		return builder.toString();
	}
	
	public static void DebugThisVariable(IBslStackFrame stackframe, String variable) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("ИрОбщий.От(");
		stringBuilder.append(variable);
		stringBuilder.append(")");
		
		String executionCommand = stringBuilder.toString();
		try {
			EvaluateExpression(stackframe, executionCommand);
		} catch(DebugException e) {
			Notification.showMessage("Ошибка при выполнении команды: " + e.getMessage());
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
			Notification.showMessage("Ошибка при выполнении команды: " + e.getMessage());
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
