package net.jlehtinen.portlet.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Encapsulates portlet.xml and provides utility methods for accessing and processing it.
 */
public class PortletXml {

	/** The portlet.xml as a DOM document */
	protected Document portletXmlDoc;
	
	/** The portlet DOM elements */
	protected Collection<Element> portletElements;
	
	/**
	 * Constructs a new instance from the specified portlet.xml file.
	 * 
	 * @param file portlet.xml file to load
	 * @throws IOException if an I/O error occurs
	 */
	protected PortletXml(File file) throws IOException {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			DocumentBuilder db = dbf.newDocumentBuilder();
			portletXmlDoc = db.parse(file);
			portletElements = findPortletNodes(portletXmlDoc);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException("Failed to parse portlet descriptor", e);
		}		
	}
	
	/**
	 * Finds and returns the collection of portlet DOM elements in the specified portlet.xml document.
	 * 
	 * @param portletXmlDoc porlet.xml as a DOM document
	 * @return collection found portlet DOM elements
	 */
	protected static Collection<Element> findPortletNodes(Document portletXmlDoc) {
		Node root = portletXmlDoc.getDocumentElement();
		Collection<Element> portlets = new ArrayList<Element>();
		
		// Check that this is a portlet descriptor
		if (root.getLocalName().equals("portlet-app")) {
			
			// Go through all possible portlet elements
			NodeList rootChildren = root.getChildNodes();
			for (int i = 0; i < rootChildren.getLength(); i++) {
				Node child = rootChildren.item(i);
				if (child.getNodeType() == Node.ELEMENT_NODE && child.getLocalName().equals("portlet")) {
					portlets.add((Element) child);
				}
			}
		}
		
		// Return the set of found portlet nodes
		return portlets;
	}
	
	/**
	 * Loads portlet descriptor from the specified file.
	 * 
	 * @param file portlet.xml to be loaded
	 * @return portlet.xml DOM document
	 */
	public static PortletXml load(File file) throws IOException {
		return new PortletXml(file);
	}

	/**
	 * Returns the names of specified portlets.
	 * 
	 * @return names of specified portlets
	 */
	public Set<String> getPortletNames() {
		Set<String> names = new HashSet<String>();
		Iterator<Element> iter = portletElements.iterator();
		while (iter.hasNext()) {
			Element portletElement = iter.next();
			String portletName = getPortletName(portletElement);
			if (portletName != null) {
				names.add(portletName);
			}
		}
		return names;
	}
	
	/**
	 * Filters away all the portlets not contained in the specified set of included
	 * portlets.
	 *  
	 * @param includedPortlets set of portlet names to be included
	 * @throws IOException
	 */
	public void filterPortlets(Set<String> includedPortlets) {
		Iterator<Element> iter = portletElements.iterator();
		while (iter.hasNext()) {
			Element portletElement = iter.next();
			
			// Filter portlet if necessary
			String portletName = getPortletName(portletElement);
			if (portletName != null && !includedPortlets.contains(portletName)) {
				portletElement.getParentNode().removeChild(portletElement);
				iter.remove();
			}
			
		}
	}

	/**
	 * Returns the name of the specified portlet or null if none found.
	 * 
	 * @param portletElement portlet element being examined
	 * @return the named of the portlet or null if none found
	 */
	protected static String getPortletName(Element portletElement) {
		String portletName = null;
		NodeList portletChildren = portletElement.getChildNodes();
		for (int j = 0; j < portletChildren.getLength(); j++) {
			Node c = portletChildren.item(j);
			if (c.getNodeType() == Node.ELEMENT_NODE && c.getLocalName().equals("portlet-name")) {
				portletName = c.getTextContent();
				break;
			}
		}
		return portletName;
	}
	
	/**
	 * Saves portlet descriptor to the specified file.
	 * 
	 * @param file destination file
	 */
	public void save(File file) throws IOException {
		try {
			file.getParentFile().mkdirs();
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer t = tf.newTransformer();
			Source src = new DOMSource(portletXmlDoc);
			Result res = new StreamResult(file);
			t.transform(src, res);
		} catch (Exception e) {
			throw new IOException("Failed to save portlet descriptor", e);
		}
	}
}
