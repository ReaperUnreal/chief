package ca.caseybanner.chief.util;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Helper class to expose the protected addURL method of a standard URLClassLoader
 * <p/>
 * Created by cshankland on 15-02-11.
 */
public class DynamicURLClassLoader extends URLClassLoader {

	/**
	 * Create a new url class loader
	 *
	 * @param loader
	 */
	public DynamicURLClassLoader(URLClassLoader loader) {
		super(loader.getURLs());
	}

	/**
	 * Add a url to the class loader
	 *
	 * @param url
	 */
	public void addURL(URL url) {
		super.addURL(url);
	}
}
