package org.kovalexey.rdt1c.debug.ui.dialog;

import org.eclipse.emf.common.util.URI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.xtext.scoping.IScopeProvider;
import org.eclipse.xtext.ui.editor.embedded.EmbeddedEditor;
import org.eclipse.xtext.ui.editor.embedded.EmbeddedEditorFactory;
import org.eclipse.xtext.ui.editor.embedded.EmbeddedEditorModelAccess;
import org.eclipse.xtext.ui.editor.embedded.IEditedResourceProvider;
import org.eclipse.xtext.validation.IResourceValidator;
import org.kovalexey.rdt1c.debug.ui.RDT1CPlugin;

import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.lcore.ui.editor.embedded.CustomEmbeddedEditorResourceProvider;
import com.google.inject.Inject;
import com.google.inject.Injector;

public class BslDebugTextEditor {
	
	private Composite parent;
	private EmbeddedEditor editor;
	private EmbeddedEditorModelAccess editorModelAccess;
	private Injector bslInjector;
	private CustomEmbeddedEditorResourceProvider resourceProvider;
	private ProjectCombo combo; // TODO: Надо переделать
	private URI debugUri;
	@Inject
	private IV8ProjectManager projectManager;
	
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

	private CustomEmbeddedEditorResourceProvider getResourceProvider() {
		return (CustomEmbeddedEditorResourceProvider)bslInjector.getInstance(IEditedResourceProvider.class);
	}

	private EmbeddedEditorFactory getEmbeddedFactory() {
		return bslInjector.getInstance(EmbeddedEditorFactory.class);
	}
	
	private IScopeProvider getScopeProvider() {
		return bslInjector.getInstance(IScopeProvider.class);
	}
	
	public EmbeddedEditor getEditor() {
		return this.editor;
	}
	
	public String getEditorText() {
		return editorModelAccess.getEditablePart();
	}
	
	private void setEditorPrefix(String string) {
		editorModelAccess.updatePrefix(string);
	}
	
	public EmbeddedEditorModelAccess createPartialEditor() {
		this.editorModelAccess = getEditor().createPartialEditor();
		
		return this.editorModelAccess;
	}
	
}
