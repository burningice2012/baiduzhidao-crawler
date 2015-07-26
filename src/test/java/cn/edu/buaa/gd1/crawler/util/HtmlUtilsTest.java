/**
 * Copyright 2007, NetworkBench Systems Corp.
 */
package cn.edu.buaa.gd1.crawler.util;

import junit.framework.Assert;

import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.util.NodeList;
import org.testng.annotations.Test;

/**
 * @author BurningIce
 *
 */
public class HtmlUtilsTest {
	@Test
	public void testGetChildNodesByTagNameAndClassName() throws Exception {
		String html = "<html><body><div class=\"ask-title ask-title2\"></div>abcd<div><div class=\"ask-title \">xxxxx</div></div><div class=\"line content\"><pre id=\"recommend-content-79804\" accuse=\"aContent\" class=\"recommend-text mb-10\">人身上不是没有毛，只是退化成了汗毛。</pre></body></html>";
		Parser parser = Parser.createParser(html, "GBK");
		Node body = parser.extractAllNodesThatMatch(new TagNameFilter("body")).elementAt(0);
		Node[] nodes = HtmlUtils.getChildNodesByTagNameAndClassName((TagNode)body, "div", "ask-title");
		Assert.assertNotNull(nodes);
		Assert.assertEquals(nodes.length, 2);
		
		Node[] preNodes = HtmlUtils.getChildNodesByTagNameAndClassName((TagNode)body, "pre", "recommend-text");
		Assert.assertNotNull(preNodes);
		Assert.assertEquals(preNodes.length, 1);
		Assert.assertEquals(preNodes[0].getParent().toPlainTextString(), "人身上不是没有毛，只是退化成了汗毛。");
	}
}
