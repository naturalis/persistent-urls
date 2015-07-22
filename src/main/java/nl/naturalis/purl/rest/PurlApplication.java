package nl.naturalis.purl.rest;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import nl.naturalis.purl.Registry;

/**
 * JAX-RS framework class.
 * 
 * @author Ayco Holleman
 * @created Jul 22, 2015
 *
 */
@ApplicationPath("/")
public class PurlApplication extends Application {

	public PurlApplication()
	{
		super();
		Registry.initialize();
	}
}
