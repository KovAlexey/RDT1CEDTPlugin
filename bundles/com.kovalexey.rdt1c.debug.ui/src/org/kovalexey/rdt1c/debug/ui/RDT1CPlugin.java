package org.kovalexey.rdt1c.debug.ui;

import org.eclipse.emf.common.util.URI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.resource.IResourceServiceProvider.Registry;
import org.osgi.framework.BundleContext;

import com._1c.g5.v8.dt.core.xtext.resource.ExtendedResourceServiceProvider;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * The activator class controls the plug-in life cycle
 */
public class RDT1CPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.kovalexey.rdt1c.debug.ui"; //$NON-NLS-1$

	// The shared instance
	private static RDT1CPlugin plugin;
	private Injector injector;
	private Injector bslInjector;

	
	/**
	 * The constructor
	 */
	public RDT1CPlugin() {
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
	public static RDT1CPlugin getDefault() {
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
			return Guice.createInjector(new ExternalDependeciesInjector(this));
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return null;
	}
	
	public synchronized Injector getBslInjector() {
		
        try {
            URI uri = URI.createURI("*.bsl");
            IResourceServiceProvider baseRsp = Registry.INSTANCE.getResourceServiceProvider(uri);
            if (baseRsp instanceof ExtendedResourceServiceProvider) {
                this.bslInjector = ((ExtendedResourceServiceProvider)baseRsp).getInjector();
            } else {
                throw new RuntimeException("Cannot obtain base injector form BSL IResourceServiceProvider: " + baseRsp);
            }
            
        } catch (Exception var4) {

            throw new RuntimeException("Failed to create injector for BSL in" + getBundle().getSymbolicName());
        }
        return this.bslInjector;
	}

}
