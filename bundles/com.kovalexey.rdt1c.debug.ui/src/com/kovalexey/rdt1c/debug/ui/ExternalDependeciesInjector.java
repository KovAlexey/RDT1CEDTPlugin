package com.kovalexey.rdt1c.debug.ui;

import org.eclipse.core.runtime.Plugin;

import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
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
	}

}
