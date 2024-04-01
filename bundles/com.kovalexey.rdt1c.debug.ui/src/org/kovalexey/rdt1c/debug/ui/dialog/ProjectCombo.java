package org.kovalexey.rdt1c.debug.ui.dialog;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.kovalexey.rdt1c.debug.ui.RDT1CPlugin;

import com._1c.g5.v8.dt.core.platform.IV8Project;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com.google.inject.Inject;


public class ProjectCombo extends Combo {
	
	@Inject
	IV8ProjectManager projectsManager;
	private ArrayList<IV8Project> projectsArray = new ArrayList<IV8Project>();

	public ProjectCombo(Composite parent) {
		super(parent, SWT.READ_ONLY);
		init();
		if (projectsArray.size() > 0) {
			this.select(0);
		}
		
	}
	
	public ProjectCombo(Composite parent, IV8Project project) {
		super(parent, SWT.READ_ONLY);

		if (project == null) {
			throw new IllegalArgumentException("Project не может быть null");
		}
		init();
		this.select(projectsArray.indexOf(project));
	}
	
	private void init() {
		RDT1CPlugin.getDefault().getInjector().injectMembers(this);
		
		for (IV8Project v8project: projectsManager.getProjects()) {
			add(v8project);
		}
	}
	
	public IV8Project getCurrentProject() {
		return projectsArray.get(this.getSelectionIndex());
	}
	
	public void add(IV8Project project) {
		projectsArray.add(project);
		super.add(project.getProject().getName());
	}
	
	public void add(String name, IV8Project project, int index) {
		projectsArray.add(index, project);
		super.add(project.getProject().getName(), index);
	}
	
	@Override
	public void add(String string) {
		throw new NoSuchMethodError("This function is diabled.");
	}

	@Override
	public void add(String string, int index) {
		throw new NoSuchMethodError("This function is diabled.");
	}
	
	@Override
	protected void checkSubclass() {
		// TODO Auto-generated method stub
	}

}
