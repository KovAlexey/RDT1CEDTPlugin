package com.kovalexey.rdt1c.debug.ui;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
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
import com._1c.g5.v8.dt.debug.model.calculations.BaseValueInfoData;
import com._1c.g5.v8.dt.debug.model.calculations.CalculationResultBaseData;
import com._1c.g5.v8.dt.debug.model.calculations.ViewInterface;

public class DebugCommandExecutor {
	
	public static void DebugThisVariable(IBslVariable var)
	{
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("ИрОбщий.От(");
		stringBuilder.append(var.getName());
		stringBuilder.append(")");
		
		String executionCommand = stringBuilder.toString();
		try {
			EvaluateExpression(var.getStackFrame(), executionCommand);
		}catch(DebugException e)
		{
			// TODO: Обработка исключения
			e.printStackTrace();
		}
		
	}
	
	public static void DebugDataCompostionScheme(IBslVariable scheme, IBslVariable settings)
	{
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("ИрОбщий.От(");
		stringBuilder.append(scheme.getName());
		stringBuilder.append(",");
		stringBuilder.append(settings.getName());
		stringBuilder.append(")");
		
		String executionCommand = stringBuilder.toString();
		try {
			EvaluateExpression(scheme.getStackFrame(), executionCommand);
		}catch(DebugException e)
		{
			// TODO: Обработка исключения
			e.printStackTrace();
		}
		
	}
	
	static void EvaluateExpression(IBslStackFrame stackFrame, String exression) throws DebugException
	{
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
	
	static class DebugCommandExecutorListener implements IEvaluationListener
	{
		
		String expression;
		public DebugCommandExecutorListener(String expression) {
			super();
			this.expression = expression;
		}

		@Override
		public void evaluationComplete(IEvaluationResult execution_result) throws DebugException {
			if (!execution_result.isSuccess())
			{
				return;
			}
			CalculationResultBaseData resultBaseData = execution_result.getResult();
			if (resultBaseData.getErrorOccurred())
			{
				byte[] exception = resultBaseData.getExceptionStr();
				String result = new String(exception, StandardCharsets.UTF_8);
				Notification.showmessage(expression, result);
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
			
			Notification.showmessage(expression, builder.toString());
		}
		
	}
}