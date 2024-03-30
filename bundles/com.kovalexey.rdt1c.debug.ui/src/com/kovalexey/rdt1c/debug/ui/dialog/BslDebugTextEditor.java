package com.kovalexey.rdt1c.debug.ui.dialog;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.scoping.IScopeProvider;
import org.eclipse.xtext.ui.editor.embedded.EmbeddedEditor;
import org.eclipse.xtext.ui.editor.embedded.EmbeddedEditorFactory;
import org.eclipse.xtext.ui.editor.embedded.EmbeddedEditorModelAccess;
import org.eclipse.xtext.ui.editor.embedded.IEditedResourceProvider;
import org.eclipse.xtext.ui.editor.model.XtextDocument;
import org.eclipse.xtext.ui.editor.model.XtextDocumentUtil;
import org.eclipse.xtext.ui.editor.validation.ValidationJob;
import org.eclipse.xtext.util.concurrent.IUnitOfWork;
import org.eclipse.xtext.validation.IResourceValidator;

import com._1c.g5.v8.dt.bsl.contextdef.IBslModuleContextDefService;
import com._1c.g5.v8.dt.bsl.model.BslPackage;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.Procedure;
import com._1c.g5.v8.dt.bsl.resource.BslResource;
import com._1c.g5.v8.dt.lcore.ui.editor.embedded.CustomEmbeddedEditorResourceProvider;
import com._1c.g5.v8.dt.lcore.ui.editor.embedded.CustomModelAccessAwareEmbeddedEditorBuilder.CustomModelAccessAwareEmbeddedEditor;
import com._1c.g5.v8.dt.lcore.ui.editor.util.XtextEditorUtil;
import com._1c.g5.v8.dt.lcore.ui.editor.embedded.CustomModelAccessAwareEmbeddedEditorBuilder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.kovalexey.rdt1c.debug.ui.RDT1CPlugin;

public class BslDebugTextEditor {
	
	private Composite parent;
	private EmbeddedEditor editor;
	private EmbeddedEditorModelAccess editorModelAccess;
	private Injector bslInjector;
	private CustomEmbeddedEditorResourceProvider resourceProvider;
	private ProjectCombo combo; // TODO: Надо переделать
	private URI debugUri;
	@Inject
	private IScopeProvider scopeProvider;
	@Inject
	private IBslModuleContextDefService bslModuleContextDefService;
	
	public BslDebugTextEditor(Composite parent) {
		
		this.parent = parent;
		this.combo = new ProjectCombo(parent);
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		this.bslInjector = RDT1CPlugin.getDefault().getBslInjector();
		this.bslInjector.injectMembers(this);
		buildEditor(parent);
	}
	
	public BslDebugTextEditor(Composite parent, URI uri) {
		this.parent = parent;
		this.combo = new ProjectCombo(parent);
		this.combo.setEnabled(false);
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		this.bslInjector = RDT1CPlugin.getDefault().getBslInjector();
		this.bslInjector.injectMembers(this);
		buildEditor(parent);
		this.debugUri = uri;
		
		this.combo.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				
				buildEditor(parent);
				createPartialEditor();
			}
		});
	}
	
	private void buildEditor(Composite parent) {
		IResourceValidator resourceValidator = bslInjector.getInstance(IResourceValidator.class);
        		
        EmbeddedEditorFactory factory = getEmbeddedFactory();
        resourceProvider = getResourceProvider();
        
        resourceProvider.setUriSegment(debugUri);
        resourceProvider.setProject(combo.getCurrentProject().getProject());
        
        EmbeddedEditor editor = factory
        		.newEditor(resourceProvider)
        		.withResourceValidator(resourceValidator)
        		.showLineNumbers()
        		.withParent(parent);
        
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
	
	public void setResourceSet(BslResource resourceSet) {
		getEditor().getDocument().tryModify(new IUnitOfWork.Void<XtextResource>() {

			@Override
			public void process(XtextResource state) throws Exception {
				((BslResource)state).setDeepAnalysis(true);
				System.out.print(false);
			}
		});
	}
	
	private void setEditorPrefix(String string) {
		
	}
	
	public EmbeddedEditorModelAccess createPartialEditor() {
		this.editorModelAccess = getEditor().createPartialEditor();
		
		return this.editorModelAccess;
	}
	
}
