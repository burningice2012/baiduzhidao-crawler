/**
 * Copyright 2007, NetworkBench Systems Corp.
 */
package cn.edu.buaa.gd1.crawler.util;

import java.util.ArrayList;
import java.util.List;

import org.htmlparser.Node;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.util.NodeList;

/**
 * @author BurningIce
 *
 */
public class HtmlUtils {

	public static Node[] getChildNodesByTagNameAndClassName(TagNode parentNode, String tagName, String className) {
		if(parentNode == null || tagName == null || className == null) {
			return null;
		}
		
		ArrayList<Node> nodesFound = new ArrayList<Node>();
		extractChildNodesByTagNameAndClassNameRecursive(parentNode, tagName.toUpperCase(), className, nodesFound);
		return nodesFound.size() == 0 ? null : nodesFound.toArray(new Node[nodesFound.size()]);
	}
	
	private static void extractChildNodesByTagNameAndClassNameRecursive(TagNode parentNode, String tagName, String className, List<Node> nodesFound) {
		if(parentNode == null || tagName == null || className == null) {
			return;
		}
		
		String myTagName = parentNode.getTagName();
		String myClassName = parentNode.getAttribute("class");
		if(myTagName != null && myClassName != null &&
				myTagName.toUpperCase().equals(tagName)) {
			String[] myClassNames = myClassName.split("\\s+");
			// check className
			for(String cn : myClassNames) {
				if(cn.equals(className)) {
					// matches
					nodesFound.add(parentNode);
					break;
				}
			}
		}
		
		// check child nodes
		NodeList children = parentNode.getChildren();
		if(children != null && children.size() > 0) {
			for(int i = 0; i < children.size(); ++i) {
				Node n = children.elementAt(i);
				if(n instanceof TagNode) {
					extractChildNodesByTagNameAndClassNameRecursive((TagNode)n, tagName, className, nodesFound);
				}
			}
		}
	}
}
