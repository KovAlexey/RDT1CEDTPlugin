package org.kovalexey.rdt1c.debug.ui;

import org.eclipse.core.runtime.Plugin;
import org.eclipse.xtext.resource.IResourceServiceProvider;

import com._1c.g5.v8.dt.core.platform.IBmModelManager;
import com._1c.g5.v8.dt.core.platform.IConfigurationProvider;
import com._1c.g5.v8.dt.core.platform.IDtProjectManager;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.core.xtext.resource.ExtendedResourceServiceProvider;
import com._1c.g5.wiring.AbstractServiceAwareModule;

public class ExternalDependeciesInjector extends AbstractServiceAwareModule {

	public ExternalDependeciesInjector(Plugin bundle) {
		super(bundle);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void doConfigure() {
		// TODO Auto-generated method stub
		bind(IV8ProjectManager.class).toService();
		bind(IBmModelManager.class).toService();
		bind(IDtProjectManager.class).toService();
		bind(IConfigurationProvider.class).toService();
		
	}

}
