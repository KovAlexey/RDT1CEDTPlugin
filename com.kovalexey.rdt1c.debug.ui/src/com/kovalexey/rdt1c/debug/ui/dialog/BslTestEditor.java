package com.kovalexey.rdt1c.debug.ui.dialog;

import org.eclipse.core.resources.IProject;
import org.eclipse.emf.common.util.URI;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.xtext.ui.editor.embedded.EmbeddedEditor;
import org.eclipse.xtext.ui.editor.embedded.EmbeddedEditorFactory;
import org.eclipse.xtext.ui.editor.embedded.IEditedResourceProvider;
import org.eclipse.xtext.validation.IResourceValidator;

import com._1c.g5.v8.dt.lcore.ui.editor.embedded.CustomEmbeddedEditorResourceProvider;
import com._1c.g5.v8.dt.lcore.ui.editor.embedded.CustomModelAccessAwareEmbeddedEditorBuilder;
import com.google.inject.Injector;
import com.kovalexey.rdt1c.debug.ui.Activator;

public class BslTestEditor {

	@SuppressWarnings("restriction")
	private EmbeddedEditor editor;
	
	@SuppressWarnings("restriction")
	public BslTestEditor(Composite parent, IProject project) {
		Injector bslInjector = Activator.getDefault().getBslInjector();
		
        IResourceValidator resourceValidator = bslInjector.getInstance(IResourceValidator.class);
        EmbeddedEditorFactory factory = bslInjector.getInstance(EmbeddedEditorFactory.class);
        CustomEmbeddedEditorResourceProvider resourceProvider = (CustomEmbeddedEditorResourceProvider)bslInjector.getInstance(IEditedResourceProvider.class);
        URI uri = URI.createPlatformPluginURI("debug.bsl", true);
        resourceProvider.setUriSegment(uri);
        resourceProvider.setProject(project);
        this.editor = factory.newEditor(resourceProvider)
        		.withResourceValidator(resourceValidator)
        		.withParent(parent);
	}
	
	@SuppressWarnings("restriction")
	public EmbeddedEditor getEditor() {
		return this.editor;
	}
	
}
