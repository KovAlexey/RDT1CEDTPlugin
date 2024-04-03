package org.kovalexey.rdt1c.debug.ui.dialog;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.xtext.resource.EObjectAtOffsetHelper;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.scoping.IScopeProvider;
import org.eclipse.xtext.ui.editor.embedded.EmbeddedEditor;
import org.eclipse.xtext.ui.editor.embedded.EmbeddedEditorFactory;
import org.eclipse.xtext.ui.editor.embedded.EmbeddedEditorModelAccess;
import org.eclipse.xtext.ui.editor.embedded.IEditedResourceProvider;
import org.eclipse.xtext.util.concurrent.IUnitOfWork;
import org.eclipse.xtext.validation.IResourceValidator;
import org.kovalexey.rdt1c.debug.ui.RDT1CPlugin;

import com._1c.g5.v8.dt.bsl.model.Expression;
import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.impl.FunctionImpl;
import com._1c.g5.v8.dt.bsl.model.impl.InvocationImpl;
import com._1c.g5.v8.dt.bsl.model.impl.ProcedureImpl;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
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
	
	public BslDebugTextEditor(Composite parent, URI uri) {
		this.bslInjector = RDT1CPlugin.getDefault().getBslInjector();
		this.bslInjector.injectMembers(this);
		
		this.parent = parent;
		this.combo = new ProjectCombo(parent, projectManager.getProject(uri));
		this.combo.setEnabled(false);
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		
		this.debugUri = uri;
		buildEditor(parent);
		
		prefix = "Функция Тест1()\n Возврат Новый Структура(\"Поле\", 1000);\n"
				+ "КонецФункции\n"
				+ "Процедура ОтладкаТест()\n";
		suffix = "КонецПроцедуры";
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
		
		System.out.println(editorModelAccess.getSerializedModel());
		
		editor.getDocument().tryModify(new IUnitOfWork.Void<XtextResource>() {

			@Override
			public void process(XtextResource state) throws Exception {
 				int offset = prefix.length();
				
				var startobj = objectAtOffsetHelper.resolveElementAt(state, offset);
				
				var procedure = getObjectProcedure(startobj);
				if (procedure == null) {
					throw new RuntimeException("модель развалилась");
				}
				printObjects(procedure, 0);

			}
		});
		// Заглушка
		return editorModelAccess.getEditablePart();
	}
	
	
	private ProcedureImpl getObjectProcedure(EObject object) {
		EObject parent = object.eContainer();
		if (parent == null) {
			return null;
		} else if (parent instanceof ProcedureImpl) {
			return (ProcedureImpl)parent;
		} else {
			return getObjectProcedure(parent);
		}
	}
	
	private void printObjects(EObject object, int len) {
		if (len == 10 ) {
			return;
		}
		var treeiterator = object.eContents();
		for (EObject eObject : treeiterator) {
			
			if (eObject instanceof FeatureAccess) {
				System.out.print(((FeatureAccess)eObject).getName());
			} else if (eObject instanceof InvocationImpl) {
				InvocationImpl invocationObject = (InvocationImpl)eObject;
				for (Expression param : invocationObject.getParams()) {
					
				}
				System.out.print(((InvocationImpl)eObject).getParams());
			} else if (eObject instanceof FunctionImpl) {
				FunctionImpl methodObject = (FunctionImpl)eObject;
				System.out.print(methodObject.getName());
				for (var eObject2 : methodObject.getFormalParams()) {
					
				}
				System.out.print(methodObject.getFormalParams());
				
			}
			
			
			for (int i = 0; i < len; i ++) {
				System.out.print("	");	
			}
			
			System.out.println(eObject);
			printObjects(eObject, len + 1);
		}
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
