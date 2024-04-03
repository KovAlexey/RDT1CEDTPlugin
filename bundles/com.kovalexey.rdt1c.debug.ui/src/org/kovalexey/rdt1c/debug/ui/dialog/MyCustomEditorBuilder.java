package org.kovalexey.rdt1c.debug.ui.dialog;

import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.xtext.ui.editor.XtextSourceViewer;
import org.eclipse.xtext.ui.editor.XtextSourceViewerConfiguration;
import org.eclipse.xtext.ui.editor.embedded.EmbeddedEditor;
import org.eclipse.xtext.ui.editor.embedded.EmbeddedEditorActions;
import org.eclipse.xtext.ui.editor.embedded.IEditedResourceProvider;
import org.eclipse.xtext.ui.editor.model.XtextDocument;
import org.eclipse.xtext.ui.editor.syntaxcoloring.HighlightingHelper;
import org.eclipse.xtext.ui.editor.validation.ValidationJob;
import org.eclipse.xtext.validation.CheckMode;

import com._1c.g5.ides.ui.texteditor.xtext.embedded.CustomEmbeddedEditorBuilder;
import com._1c.g5.ides.ui.texteditor.xtext.embedded.EmbeddedEditorValidationIssueProcessor;

@SuppressWarnings("restriction")
public class MyCustomEditorBuilder extends CustomEmbeddedEditorBuilder {
	
	protected HighlightingHelper highlightingHelper;

	@Override
	protected XtextDocument createDocument() {
		XtextDocument document = (XtextDocument) this.documentProvider.get();

		IDocumentPartitioner partitioner = (IDocumentPartitioner) this.documentPartitionerProvider.get();
		
		partitioner.connect(document);
		document.setDocumentPartitioner(partitioner);
		
		return document;
	}


	protected void installValidationSupport(XtextDocument document, XtextSourceViewer viewer) {
		
		ValidationJob job = new ValidationJob(this.resourceValidator, document,
				new EmbeddedEditorValidationIssueProcessor(this.issueResolutionProvider, this.issueProcessor, document,
						viewer),
				CheckMode.FAST_ONLY);

		document.setValidationJob(job);
	}

	@Override
	protected void setResourceProvider(IEditedResourceProvider resourceProvider) {
		// TODO Auto-generated method stub
		super.setResourceProvider(resourceProvider);
	}
	
	@Override
	public EmbeddedEditor withParent(Composite parent) {
		// TODO Auto-generated method stub
		return super.withParent(parent);
	}

	public EmbeddedEditor withParentNew(Composite parent) {
		this.enableFolding();
		CompositeRuler annotationRuler = this.createAnnotationRuler();
		IOverviewRuler overviewRuler = this.createOverviewRuler();
		XtextSourceViewerConfiguration viewerConfiguration = this.createSourceViewerConfiguration();
		XtextSourceViewer viewer = this.createSourceViewer(parent, annotationRuler, overviewRuler, viewerConfiguration);
		
		this.installSourceViewerDecorationSupport(parent, viewer, overviewRuler);
		XtextDocument document = this.createDocument();
		
		EmbeddedEditor editor = this.createEmbeddedEditor(annotationRuler, viewer, viewerConfiguration, document,
				new EmbeddedEditorActions(viewer, PlatformUI.getWorkbench())
				);
		this.installValidationSupport(document, viewer);

		viewer.getControl().setLayoutData(new GridData(4, 4, true, true));
		return editor;
	}

}
