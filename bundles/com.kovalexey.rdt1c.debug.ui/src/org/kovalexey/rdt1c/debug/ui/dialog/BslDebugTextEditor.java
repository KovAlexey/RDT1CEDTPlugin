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
import com._1c.g5.v8.dt.debug.core.model.values.IBslValue;
import com._1c.g5.v8.dt.lcore.ui.editor.embedded.CustomEmbeddedEditorResourceProvider;
import com.google.inject.Inject;
import com.google.inject.Injector;

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
		StringBuilder sb = new StringBuilder();
		sb.append("Процедура ОтладкаТест()\n");
		try {
			var variables = bslStackFrame.getVariables();
			for (int i = 0; i < variables.length; i++) {
				String name = variables[i].getName();
				IBslValue value = variables[i].getValue();
				
				sb.append("    ");
				sb.append(name);
				sb.append(" = ");
				sb.append(getMockInitializer(value));
				sb.append("; // ");
				sb.append(value.getValueTypeName());
				sb.append("\n");
			}
		} catch (Exception e) {
			// ignore
		}
		prefix = sb.toString();
		suffix = "\nКонецПроцедуры";
	}

	private String getMockInitializer(IBslValue value) {
		return getMockInitializer(value, 0);
	}

	private String getMockInitializer(IBslValue value, int depth) {
		if (depth > 2) return "Неопределено";
		try {
			String typeName = value.getValueTypeName();
			if (typeName.equals("Структура") || typeName.equals("Structure")) {
				var children = value.getVariables();
				if (children.length == 0) return "Новый Структура";
				StringBuilder fields = new StringBuilder();
				StringBuilder values = new StringBuilder();
				for (int i = 0; i < children.length; i++) {
					fields.append(children[i].getName());
					values.append(getMockInitializer(children[i].getValue(), depth + 1));
					if (i < children.length - 1) {
						fields.append(", ");
						values.append(", ");
					}
				}
				return "Новый Структура(\"" + fields.toString() + "\", " + values.toString() + ")";
			} else if (typeName.equals("Соответствие") || typeName.equals("Map")) {
				return "Новый Соответствие";
			} else if (typeName.startsWith("СправочникСсылка.") || typeName.startsWith("CatalogRef.")) {
				String metadataName = typeName.substring(typeName.indexOf('.') + 1);
				return "Справочники." + metadataName + ".ПустаяСсылка()";
			} else if (typeName.startsWith("ДокументСсылка.") || typeName.startsWith("DocumentRef.")) {
				String metadataName = typeName.substring(typeName.indexOf('.') + 1);
				return "Документы." + metadataName + ".ПустаяСсылка()";
			} else if (typeName.startsWith("СправочникОбъект.") || typeName.startsWith("CatalogObject.")) {
				String metadataName = typeName.substring(typeName.indexOf('.') + 1);
				return "Справочники." + metadataName + ".СоздатьЭлемент()";
			} else if (typeName.startsWith("ДокументОбъект.") || typeName.startsWith("DocumentObject.")) {
				String metadataName = typeName.substring(typeName.indexOf('.') + 1);
				return "Документы." + metadataName + ".СоздатьДокумент()";
			} else if (typeName.equals("ТаблицаЗначений") || typeName.equals("ValueTable")) {
				return "Новый ТаблицаЗначений";
			} else if (typeName.equals("Массив") || typeName.equals("Array")) {
				return "Новый Массив";
			}
		} catch (Exception e) {
			// ignore
		}
		return "Неопределено";
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
