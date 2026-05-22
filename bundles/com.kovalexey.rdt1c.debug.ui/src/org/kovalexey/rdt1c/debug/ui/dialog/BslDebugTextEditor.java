package org.kovalexey.rdt1c.debug.ui.dialog;

import org.eclipse.emf.common.util.URI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.xtext.resource.EObjectAtOffsetHelper;
import org.eclipse.xtext.scoping.IScopeProvider;
import org.eclipse.xtext.ui.editor.embedded.EmbeddedEditor;
import org.eclipse.xtext.ui.editor.embedded.EmbeddedEditorFactory;
import org.eclipse.xtext.ui.editor.embedded.EmbeddedEditorModelAccess;
import org.eclipse.xtext.ui.editor.embedded.IEditedResourceProvider;
import org.eclipse.xtext.validation.IResourceValidator;
import org.kovalexey.rdt1c.debug.ui.RDT1CPlugin;

import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.debug.core.model.IBslStackFrame;
import com._1c.g5.v8.dt.debug.core.model.IBslVariable;
import com._1c.g5.v8.dt.debug.core.model.values.IBslValue;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Function;
import com._1c.g5.v8.dt.bsl.model.FormalParam;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import com._1c.g5.v8.dt.lcore.ui.editor.embedded.CustomEmbeddedEditorResourceProvider;
import com.google.inject.Inject;
import com.google.inject.Injector;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.EObject;


public class BslDebugTextEditor {
	
	private Composite parent;
	@SuppressWarnings("restriction")
	private EmbeddedEditor editor;
	@SuppressWarnings("restriction")
	private EmbeddedEditorModelAccess editorModelAccess;
	private Injector bslInjector;
	private CustomEmbeddedEditorResourceProvider resourceProvider;
	private ProjectCombo combo; // TODO: Надо переделать
	private URI debugUri;
	private IBslStackFrame bslStackFrame;
	@Inject
	private IV8ProjectManager projectManager;
	@Inject
	private EObjectAtOffsetHelper objectAtOffsetHelper;
	private String suffix = "";
	private String prefix = "";
	
	public BslDebugTextEditor(Composite parent) {
		
		this.parent = parent;
		this.combo = new ProjectCombo(parent);
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		this.bslInjector = RDT1CPlugin.getDefault().getBslInjector();
		this.bslInjector.injectMembers(this);
		
		buildEditor(parent);
	}
	
	public BslDebugTextEditor(Composite parent, URI uri, IBslStackFrame bslStackFrame) {
		this.bslInjector = RDT1CPlugin.getDefault().getBslInjector();
		this.bslInjector.injectMembers(this);
		
		this.parent = parent;
		this.bslStackFrame = bslStackFrame;
		this.combo = new ProjectCombo(parent, projectManager.getProject(uri));
		this.combo.setEnabled(false);
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		
		this.debugUri = uri;
		
		generateContext();
		
		buildEditor(parent);
	}

	private void generateContext() {
		Module module = null;
		try {
			XtextResourceSet resourceSet = bslInjector.getInstance(XtextResourceSet.class);
			Resource resource = resourceSet.getResource(debugUri, true);
			if (resource != null && !resource.getContents().isEmpty()) {
				EObject root = resource.getContents().get(0);
				if (root instanceof Module) {
					module = (Module) root;
				}
			}
		} catch (Exception e) {
			// ignore
		}

		String moduleText = "";
		if (module != null) {
			ICompositeNode node = NodeModelUtils.getNode(module);
			if (node != null) {
				moduleText = node.getText();
			}
		}

		int lineNumber = 1;
		try {
			lineNumber = bslStackFrame.getLineNumber();
		} catch (Exception e) {
			// ignore
		}

		Method activeMethod = null;
		if (module != null) {
			try {
				for (Method method : module.getMethods()) {
					ICompositeNode node = NodeModelUtils.getNode(method);
					if (node != null) {
						int startLine = node.getStartLine();
						int endLine = node.getEndLine();
						if (lineNumber >= startLine && lineNumber <= endLine) {
							activeMethod = method;
							break;
						}
					}
				}
			} catch (Exception e) {
				// ignore
			}
		}

		java.util.Set<String> missingVars = new java.util.LinkedHashSet<>();

		// Check local variables
		try {
			IBslVariable[] variables = bslStackFrame.getVariables();
			if (variables != null) {
				for (IBslVariable variable : variables) {
					String varName = variable.getName();
					if (varName != null && !isVariablePresent(varName, activeMethod, module)) {
						missingVars.add(varName);
					}
				}
			}
		} catch (Exception e) {
			// ignore
		}

		// Check module variables
		try {
			if (bslStackFrame.hasModuleVariables()) {
				IBslVariable[] moduleVars = bslStackFrame.getModuleVariables();
				if (moduleVars != null) {
					for (IBslVariable variable : moduleVars) {
						String varName = variable.getName();
						if (varName != null && !isVariablePresent(varName, activeMethod, module)) {
							missingVars.add(varName);
						}
					}
				}
			}
		} catch (Exception e) {
			// ignore
		}

		// Check module properties
		try {
			if (bslStackFrame.hasModuleProperties()) {
				IBslVariable[] moduleProps = bslStackFrame.getModuleProperties();
				if (moduleProps != null) {
					for (IBslVariable variable : moduleProps) {
						String varName = variable.getName();
						if (varName != null && !isVariablePresent(varName, activeMethod, module)) {
							missingVars.add(varName);
						}
					}
				}
			}
		} catch (Exception e) {
			// ignore
		}

		int splitOffset = getOffsetOfLine(moduleText, lineNumber);
		String originalPrefix = moduleText.substring(0, splitOffset);
		String originalSuffix = moduleText.substring(splitOffset);

		if (activeMethod != null) {
			StringBuilder sbDeclarations = new StringBuilder();
			for (String varName : missingVars) {
				sbDeclarations.append("\n").append(varName).append(" = Неопределено;");
			}
			sbDeclarations.append("\n");
			prefix = originalPrefix + sbDeclarations.toString();
			suffix = originalSuffix;
		} else {
			StringBuilder sbModuleVars = new StringBuilder();
			for (String varName : missingVars) {
				sbModuleVars.append("Перем ").append(varName).append(";\n");
			}
			prefix = sbModuleVars.toString() + originalPrefix;
			suffix = originalSuffix;
		}
	}

	private static final java.util.Set<String> RESERVED_WORDS = java.util.Set.of(
		"истина", "ложь", "неопределено", "этотобъект", "справочники", "документы",
		"регистрысведений", "регистрынакопления", "регистрыбухгалтерии", "планывидовхарактеристик",
		"планысчетов", "планывидоврасчета", "бизнеспроцессы", "задачи", "константы",
		"параметрысеанса", "перечисления"
	);

	private boolean isVariablePresent(String varName, Method activeMethod, Module module) {
		if (varName == null || varName.isBlank() || RESERVED_WORDS.contains(varName.toLowerCase())) {
			return true;
		}
		
		// 1. Check if it's a parameter of the active method
		if (activeMethod != null) {
			for (FormalParam param : activeMethod.getFormalParams()) {
				if (param.getName() != null && param.getName().equalsIgnoreCase(varName)) {
					return true;
				}
			}
		}
		
		// 2. Check if it is already used/declared in the active method text (using regex word boundary)
		if (activeMethod != null) {
			org.eclipse.xtext.nodemodel.ICompositeNode methodNode = org.eclipse.xtext.nodemodel.util.NodeModelUtils.getNode(activeMethod);
			if (methodNode != null) {
				String methodText = methodNode.getText();
				java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\b" + java.util.regex.Pattern.quote(varName) + "\\b", java.util.regex.Pattern.CASE_INSENSITIVE);
				if (p.matcher(methodText).find()) {
					return true;
				}
			}
		}
		
		// 3. Check if it is defined as a module variable in the module text
		if (module != null) {
			org.eclipse.xtext.nodemodel.ICompositeNode moduleNode = org.eclipse.xtext.nodemodel.util.NodeModelUtils.getNode(module);
			if (moduleNode != null) {
				String moduleTextStr = moduleNode.getText();
				java.util.regex.Pattern p = java.util.regex.Pattern.compile("(?i)\\bПерем\\s+[^;]*?\\b" + java.util.regex.Pattern.quote(varName) + "\\b", java.util.regex.Pattern.CASE_INSENSITIVE);
				if (p.matcher(moduleTextStr).find()) {
					return true;
				}
			}
		}
		
		return false;
	}

	private int getOffsetOfLine(String text, int lineNum) {
		if (lineNum <= 1) return 0;
		int currentLine = 1;
		int offset = 0;
		int length = text.length();
		while (offset < length && currentLine < lineNum) {
			char c = text.charAt(offset);
			if (c == '\n') {
				currentLine++;
			} else if (c == '\r') {
				if (offset + 1 < length && text.charAt(offset + 1) == '\n') {
					offset++;
				}
				currentLine++;
			}
			offset++;
		}
		return offset;
	}

	
	private void buildEditor(Composite parent) {
		IResourceValidator resourceValidator = bslInjector.getInstance(IResourceValidator.class);
		
        MyCustomEditorBuilder factory = bslInjector.getInstance(MyCustomEditorBuilder.class);
        
        resourceProvider = getResourceProvider();
        resourceProvider.setPlatformUri(debugUri);
        resourceProvider.setProject(combo.getCurrentProject().getProject());
        
        factory.setResourceProvider(resourceProvider);

        EmbeddedEditor editor = factory
        		.withParentNew(parent);
        
        this.editor = editor;
        
        
	}
	
	public String getFormattedForExecuteText() {
		// Возвращаем редактируемую часть (код пользователя)
		return editorModelAccess.getEditablePart();
	}
	
	
	private CustomEmbeddedEditorResourceProvider getResourceProvider() {
		return (CustomEmbeddedEditorResourceProvider)bslInjector.getInstance(IEditedResourceProvider.class);
	}

	private EmbeddedEditorFactory getEmbeddedFactory() {
		return bslInjector.getInstance(EmbeddedEditorFactory.class);
	}
	
	private IScopeProvider getScopeProvider() {
		return bslInjector.getInstance(IScopeProvider.class);
	}
	
	private EmbeddedEditor getEditor() {
		return this.editor;
	}
	
	public String getEditorText() {
		return editorModelAccess.getEditablePart();
	}
	
	private void setEditorPrefix(String string) {
		this.prefix = prefix;
		editorModelAccess.updatePrefix(string);
	}
	
	private void setEditorSuffix(String suffix) {
		this.suffix = suffix;
		editorModelAccess.updateModel(prefix, editorModelAccess.getEditablePart(), suffix);
	}
	
	public EmbeddedEditorModelAccess createPartialEditor() {
		this.editorModelAccess = getEditor().createPartialEditor(prefix, "", suffix, true);

		return this.editorModelAccess;
	}
	
}
