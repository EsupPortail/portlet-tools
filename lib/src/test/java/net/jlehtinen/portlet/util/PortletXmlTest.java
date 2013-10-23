package net.jlehtinen.portlet.util;

import java.io.File;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

public class PortletXmlTest {

	protected File testOutputDirectory;
	
	@Before
	public void setup() throws Exception {
		if (testOutputDirectory == null) {
			Properties props = new Properties();
			InputStream in = getClass().getResource("/paths.properties").openStream();
			props.load(in);
			in.close();
			testOutputDirectory = new File(props.getProperty("testOutputDirectory"));
		}
	}
	
	@Test
	public void testLoadAndParse() throws Exception {
		loadPortletXml();
	}
	
	@Test
	public void testGetPortletNames() throws Exception {
		PortletXml px = loadPortletXml();
		Set<String> pnames = px.getPortletNames();
		Assert.assertTrue("Portlet names contain 'example-portlet'", pnames.contains("example-portlet"));
		Assert.assertTrue("Portlet names contain 'failing-portlet'", pnames.contains("failing-portlet"));
		Assert.assertEquals("Portlet name set size", 2, pnames.size());
	}
	
	@Test
	public void testFilterPortlets() throws Exception {
		
		PortletXml px = loadPortletXml();
		Set<String> pnames = new HashSet<String>();
		pnames.add("example-portlet");
		px.filterPortlets(pnames);
		pnames = px.getPortletNames();
		Assert.assertTrue("Portlet names contain 'example-portlet'", pnames.contains("example-portlet"));
		Assert.assertFalse("Portlet names does not contain 'failing-portlet'", pnames.contains("failing-portlet"));
		Assert.assertEquals("Portlet name set size", 1, pnames.size());
		
		px = loadPortletXml();
		px.filterPortlets(new HashSet<String>());
		pnames = px.getPortletNames();
		Assert.assertEquals("Portlet name set size", 0, pnames.size());
	}
	
	protected PortletXml loadPortletXml() throws Exception {
		return PortletXml.load(new File(testOutputDirectory, "portlet.xml"));		
	}
}
