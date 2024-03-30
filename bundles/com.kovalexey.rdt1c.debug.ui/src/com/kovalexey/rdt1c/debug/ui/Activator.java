package com.kovalexey.rdt1c.debug.ui;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.kovalexey.rdt1c.debug.ui"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;
	private Injector injector;
	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	
	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}
	
	public synchronized Injector getInjector()
	{
		if (injector == null)
		{
			injector = createInjector();
		}
		return injector;
	}
	
	private Injector createInjector()
	{
		try{
			return Guice.createInjector(new ExternnalDependeciesInjector(this));
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return null;
	}

}
