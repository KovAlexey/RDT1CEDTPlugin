package org.kovalexey.rdt1c.debug.ui.utils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
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

import org.eclipse.jface.preference.IPreferenceStore;
import org.kovalexey.rdt1c.debug.ui.RDT1CPlugin;
import org.kovalexey.rdt1c.debug.ui.preferences.RDT1CPreferenceConstants;

public class DebugCommandExecutor {
	public static final String TYPE_DATA_COMPOSITION_SCHEME_RU = "СхемаКомпоновкиДанных";
	public static final String DATA_COMPOSITION_SETTINGS_RU = "НастройкиКомпоновкиДанных";
	
	private static class VariableAnalysis {
		public java.util.List<String> structureVars = new java.util.ArrayList<>();
		public java.util.List<String> assignedVars = new java.util.ArrayList<>();
	}

	private static VariableAnalysis analyzeVariables(IBslStackFrame stackFrame, String code) throws DebugException {
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
		
		java.util.List<IVariable> variablesList = new java.util.ArrayList<>();
		try {
			Collections.addAll(variablesList, stackFrame.getVariables());
		} catch (DebugException e) {
			throw new DebugException(new Status(IStatus.ERROR, RDT1CPlugin.PLUGIN_ID, "Не удалось получить локальные переменные стека."));
		}
		
		// Попытка получить переменные модуля с механизмом повтора (для обхода багов инициализации EDT)
		if (stackFrame.hasModuleVariables()) {
			IVariable[] vars = null;
			Exception lastEx = null;
			for (int retry = 0; retry < 2; retry++) {
				try {
					vars = stackFrame.getModuleVariables();
					break;
				} catch (Exception e) {
					lastEx = e;
					if (retry == 0) {
						try { Thread.sleep(100); } catch (InterruptedException ie) {}
					}
				}
			}
			if (vars == null) {
				throw new DebugException(new Status(IStatus.ERROR, RDT1CPlugin.PLUGIN_ID, 
						"Не удалось получить переменные модуля после повторной попытки. Ошибка: " + (lastEx != null ? lastEx.getMessage() : "неизвестна")));
			}
			Collections.addAll(variablesList, vars);
		}
		
		// Попытка получить свойства модуля (ЭтотОбъект, Элементы и т.д.)
		if (stackFrame.hasModuleProperties()) {
			IVariable[] props = null;
			Exception lastEx = null;
			for (int retry = 0; retry < 2; retry++) {
				try {
					props = stackFrame.getModuleProperties();
					break;
				} catch (Exception e) {
					lastEx = e;
					if (retry == 0) {
						try { Thread.sleep(100); } catch (InterruptedException ie) {}
					}
				}
			}
			if (props == null) {
				throw new DebugException(new Status(IStatus.ERROR, RDT1CPlugin.PLUGIN_ID, 
						"Не удалось получить свойства модуля (ЭтотОбъект, Элементы) после повторной попытки. Ошибка: " + (lastEx != null ? lastEx.getMessage() : "неизвестна")));
			}
			Collections.addAll(variablesList, props);
		}
		
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

	private static class ParamConfig {
		public String name;
		public String type;
		public boolean enabled;
		public int index;
	}

	public static void ExecuteCode(IBslStackFrame stackFrame, String code) {
		IPreferenceStore store = RDT1CPlugin.getDefault().getPreferenceStore();
		String methodTemplate = store.getString(RDT1CPreferenceConstants.METHOD_TEMPLATE);
		boolean resultVarUsed = store.getBoolean(RDT1CPreferenceConstants.RESULT_VAR_USED);
		String resultVarName = store.getString(RDT1CPreferenceConstants.RESULT_VAR_NAME);
		String configStr = store.getString(RDT1CPreferenceConstants.PARAMETERS_CONFIG);

		java.util.List<ParamConfig> allConfigs = new java.util.ArrayList<>();
		ParamConfig codeParam = null;
		ParamConfig ctxParam = null;
		java.util.List<ParamConfig> reassignedParams = new java.util.ArrayList<>();

		String[] parts = configStr.split(";");
		for (int i = 0; i < parts.length; i++) {
			String[] sub = parts[i].split(":");
			if (sub.length == 3) {
				ParamConfig pc = new ParamConfig();
				pc.name = sub[0];
				pc.type = sub[1];
				pc.enabled = Boolean.parseBoolean(sub[2]);
				pc.index = i + 1;
				if (pc.enabled) {
					if (pc.type.equals("C")) codeParam = pc;
					else if (pc.type.equals("S")) ctxParam = pc;
					else if (pc.type.equals("R")) reassignedParams.add(pc);
					allConfigs.add(pc);
				}
			}
		}

		if (codeParam == null || ctxParam == null) {
			Notification.showMessage("Ошибка конфигурации: не задан параметр для Кода или Контекста.");
			return;
		}

		VariableAnalysis analysis;
		try {
			analysis = analyzeVariables(stackFrame, code);
		} catch (DebugException e) {
			String message = "Ошибка подготовки контекста: " + e.getMessage();
			Notification.showMessage(message);
			logErrorToStandardLog(message, "Команда не была сформирована из-за ошибки контекста", "", e);
			return;
		}

		if (analysis.assignedVars.size() > reassignedParams.size()) {
			Notification.showMessage("Превышено ограничение (макс. " + reassignedParams.size() + ") на количество присваиваний переменным из контекста.");
			return;
		}

		StringBuilder wrappedCode = new StringBuilder();
		
		// Эта часть разбирает параметр
		for (String v : analysis.structureVars) {
			wrappedCode.append(v).append(" = ").append(ctxParam.name).append(".").append(v).append("; ");
		}
		for (int i = 0; i < analysis.assignedVars.size(); i++) {
			wrappedCode.append(analysis.assignedVars.get(i)).append(" = ").append(reassignedParams.get(i).name).append("; ");
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
		if (resultVarUsed) {
			wrappedCode.append(resultVarName).append(" = Новый Структура(); ");
			for (String v : analysis.structureVars) {
				wrappedCode.append(resultVarName).append(".Вставить(\"").append(v).append("\", ").append(v).append("); ");
			}
			for (String v : analysis.assignedVars) {
				wrappedCode.append(resultVarName).append(".Вставить(\"").append(v).append("\", ").append(v).append("); ");
			}
		}
		
		for (int i = 0; i < analysis.assignedVars.size(); i++) {
			wrappedCode.append(reassignedParams.get(i).name).append(" = ").append(analysis.assignedVars.get(i)).append("; ");
		}

		java.util.TreeMap<Integer, String> params = new java.util.TreeMap<>();
		params.put(codeParam.index, CreateTextForExecure(wrappedCode.toString()));
		params.put(ctxParam.index, ""); // placeholder for now
		
		// Структура контекста
		StringBuilder ctxPValue = new StringBuilder();
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
			ctxPValue.append("Новый Структура(\"").append(names.toString()).append("\", ").append(values.toString()).append(")");
		} else {
			ctxPValue.append("Новый Структура()");
		}
		params.put(ctxParam.index, ctxPValue.toString());
		
		// Присваиваемые параметры
		for (int i = 0; i < analysis.assignedVars.size(); i++) {
			params.put(reassignedParams.get(i).index, analysis.assignedVars.get(i));
		}
		
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(methodTemplate).append("(");
		
		int actualMaxIndex = Math.max(codeParam.index, ctxParam.index);
		for (int i = 0; i < analysis.assignedVars.size(); i++) {
			actualMaxIndex = Math.max(actualMaxIndex, reassignedParams.get(i).index);
		}
		
		for (int i = 1; i <= actualMaxIndex; i++) {
			String val = params.get(i);
			if (val != null) {
				stringBuilder.append(val);
			} else {
				stringBuilder.append("Неопределено");
			}
			if (i < actualMaxIndex) {
				stringBuilder.append(", ");
			}
		}
		
		stringBuilder.append(")");
		
		String executionCommand = stringBuilder.toString();
		try {
			EvaluateExpression(stackFrame, executionCommand, wrappedCode.toString());
		} catch(DebugException e) {
			String message = "Ошибка при выполнении команды: " + e.getMessage();
			Notification.showMessage(message);
			logErrorToStandardLog(message, executionCommand, wrappedCode.toString(), e);
		}
	}
	
	private static void logErrorToStandardLog(String message, String command, String wrappedCode, Throwable e) {
		StringBuilder sb = new StringBuilder();
		sb.append(message).append("\n\n");
		sb.append("--- ВЫПОЛНЯЕМАЯ КОМАНДА ---\n").append(command).append("\n\n");
		sb.append("--- ОБОРАЧИВАЕМЫЙ КОД (BSL) ---\n").append(wrappedCode).append("\n");
		if (e != null) {
			sb.append("\n--- СТЕК ВЫЗОВОВ JAVA ---\n");
		}
		
		RDT1CPlugin.getDefault().getLog().log(new Status(IStatus.ERROR, RDT1CPlugin.PLUGIN_ID, sb.toString(), e));
	}
	
	public static String CreateTextForExecure(String text) {
		StringBuilder builder = new StringBuilder();
		builder.append("\"");
		builder.append(text.replace("\"", "\"\""));
		builder.append("\"");
		
		return builder.toString();
	}
	
	public static void DebugThisVariable(IBslStackFrame stackframe, String variable) {
		String debugMethod = RDT1CPlugin.getDefault().getPreferenceStore().getString(RDT1CPreferenceConstants.DEBUG_METHOD_TEMPLATE);
		if (debugMethod == null || debugMethod.isEmpty()) {
			debugMethod = "ИрОбщий.От";
		}
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(debugMethod);
		stringBuilder.append("(");
		stringBuilder.append(variable);
		stringBuilder.append(")");
		
		String executionCommand = stringBuilder.toString();
		try {
			EvaluateExpression(stackframe, executionCommand, "");
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
		String debugMethod = RDT1CPlugin.getDefault().getPreferenceStore().getString(RDT1CPreferenceConstants.DEBUG_METHOD_TEMPLATE);
		if (debugMethod == null || debugMethod.isEmpty()) {
			debugMethod = "ИрОбщий.От";
		}
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(debugMethod);
		stringBuilder.append("(");
		stringBuilder.append(scheme_expression);
		stringBuilder.append(",");
		stringBuilder.append(settings_expression);
		stringBuilder.append(")");
		
		String executionCommand = stringBuilder.toString();
		try {
			EvaluateExpression(stackFrame, executionCommand, "");
		} catch(DebugException e) {
			Notification.showMessage("Ошибка при выполнении команды: " + e.getMessage());
		}
		
	}
	
	static void EvaluateExpression(IBslStackFrame stackFrame, String exression, String wrappedCode) throws DebugException {
		BslValuePath path = new BslValuePath(exression);
		List<ViewInterface> evaluationInterfaces = Collections.singletonList(ViewInterface.CONTEXT);
		UUID uuid = UUID.randomUUID();
		IEvaluationRequest request = EvaluationRequest.builder(path)
				.setStackFrame(stackFrame)
				.setExpressionUuid(uuid)
				.setInterfaces(evaluationInterfaces)
				.setMaxTestSize(4096)
				.setMultiLine(true)
				.setEvaluationListener(new DebugCommandExecutorListener(exression, wrappedCode))
				.build();
		stackFrame.getDebugTarget().getEvaluationEngine().evaluateExpression(request);
	}
	
	static class DebugCommandExecutorListener implements IEvaluationListener {
		
		String command;
		String wrappedCode;
		
		public DebugCommandExecutorListener(String command, String wrappedCode) {
			super();
			this.command = command;
			this.wrappedCode = wrappedCode;
		}

		@Override
		public void evaluationComplete(IEvaluationResult execution_result) throws DebugException {
			if (!execution_result.isSuccess()) {
				logErrorToStandardLog("Результат выполнения EvaluateExpression: Success=false", command, wrappedCode, null);
				return;
			}
			CalculationResultBaseData resultBaseData = execution_result.getResult();
			if (resultBaseData.getErrorOccurred()) {
				byte[] exception = resultBaseData.getExceptionStr();
				String result = new String(exception, StandardCharsets.UTF_8);
				Notification.showMessage(command, result);
				
				logErrorToStandardLog("Исключение в рантайме 1С: " + result, command, wrappedCode, null);
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
			
			Notification.showMessage(command, builder.toString());
		}
		
	}
}
