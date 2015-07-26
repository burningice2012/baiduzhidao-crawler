/**
 * Copyright 2007, NetworkBench Systems Corp.
 */
package cn.edu.buaa.gd1.crawler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.edu.buaa.gd1.crawler.util.HtmlUtils;
import cn.edu.buaa.gd1.crawler.util.HttpUtils;

import com.networkbench.sequence.SequenceFailedException;
import com.networkbench.sequence.SequenceGenerator;

/**
 * @author BurningIce
 *
 */
public class BaiduZhidaoCrawlerWorker implements Runnable {
	private Logger logger = LoggerFactory.getLogger(BaiduZhidaoCrawlerWorker.class);
	private final static String BAIDU_ZHIDAO_QUESTION_URL = "http://zhidao.baidu.com/question/";
	private int sleepTime;
	private String savePath;
	private SequenceGenerator sequenceGenerator;
	
	public BaiduZhidaoCrawlerWorker(String savePath, int sleepTime, SequenceGenerator sequenceGenerator) {
		this.sleepTime = sleepTime;
		this.savePath = savePath;
		this.sequenceGenerator = sequenceGenerator;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		for( ; ; ) {
			try {
				long nextQuestionId = this.sequenceGenerator.nextValue();
				getQuestion(nextQuestionId);
				Thread.sleep(sleepTime);
			} catch (SequenceFailedException e) {
				logger.error("failed to get sequence from zookeeper: " + e.getMessage(), e);
			} catch (InterruptedException e) {
				
			}
		}
	}
	
	private void getQuestion(long questionId) {
		String response = null;
		try {
			logger.debug("get question#{}...", questionId);
			response = HttpUtils.get(BAIDU_ZHIDAO_QUESTION_URL + questionId + ".html", null, "GBK");
			if(response != null) {
				if(response.indexOf("知道宝贝找不到问题了") != -1) {
					// not-found
					logger.debug("question#{} not found.", questionId);
				} else {
					// found
					Parser parser = Parser.createParser(response, "GBK");
					AndFilter qbContentNodeFilter = new AndFilter(new TagNameFilter("DIV"), new HasAttributeFilter("id", "body"));
					NodeList qbNodes = parser.extractAllNodesThatMatch(qbContentNodeFilter);
					if(qbNodes != null && qbNodes.size() > 0) {
						TagNode qbContentNode = (TagNode)qbNodes.elementAt(0);
						String questionText = getQuestionText(qbContentNode);
						if(questionText != null && (questionText = questionText.trim()).length() > 0) {
							String bestAnswerText = getBestAnswerText(qbContentNode);
							if(bestAnswerText != null && (bestAnswerText = bestAnswerText.trim()).length() > 0) {
								saveQuestionAndAnswer(questionText, bestAnswerText);
								logger.debug("best answer for question#{} found successfully.", questionId);
							} else {
								// best answer not found, try recommand answer
								String recommandAnswerText = getRecommandAnswerText(qbContentNode);
								if(recommandAnswerText != null && (recommandAnswerText = recommandAnswerText.trim()).length() > 0) {
									saveQuestionAndAnswer(questionText, recommandAnswerText);
									logger.debug("recommand answer for question#{} found successfully.", questionId);									
								} else {
									logger.warn("failed to extract best-answer or recommand-answer for question#{}.", questionId);
								}
							}	
						} else {
							logger.warn("failed to extract ask-title from question#{}.", questionId);
						}	
					} else {
						logger.warn("failed to extract qb-content from question#{}.", questionId);
					}
				}
			}
		} catch (IOException e) {
			logger.error("I/O error to get question#{}： {}", questionId, e.getMessage());
		} catch (ParserException pe) {
			logger.error("failed to parse html: {}, html: {}", pe.getMessage(), response);
		} catch (Throwable t) {
			logger.error("unexpected exception to get question#" + questionId + ": " + t.getMessage(), t);
		}
	}
	
	private void saveQuestionAndAnswer(String questionText, String answerText) {
		/// TODO to be implemented
		logger.debug("q:" + questionText + ", a:" + answerText);
	}
	
	private static String getQuestionText(TagNode qbContent) throws ParserException {
		Node[] questionNodes = HtmlUtils.getChildNodesByTagNameAndClassName(qbContent, "SPAN", "ask-title");
		if(questionNodes == null || questionNodes.length == 0)
			return null;
		
		String questionText = questionNodes[0].toPlainTextString();
		return questionText;
	}
	
	private static String getBestAnswerText(TagNode qbContent) throws ParserException {
		Node[] bestAnswerNodes = HtmlUtils.getChildNodesByTagNameAndClassName(qbContent, "PRE", "best-text");
		if(bestAnswerNodes == null || bestAnswerNodes.length == 0)
			return null;
		
		return bestAnswerNodes == null ? null : bestAnswerNodes[0].getParent().toPlainTextString();
	}
	
	private static String getRecommandAnswerText(TagNode qbContent) throws ParserException {
		Node[] bestAnswerNodes = HtmlUtils.getChildNodesByTagNameAndClassName(qbContent, "PRE", "recommend-text");
		if(bestAnswerNodes == null || bestAnswerNodes.length == 0)
			return null;
		
		return bestAnswerNodes == null ? null : bestAnswerNodes[0].getParent().toPlainTextString();
	}
	
	private static String toPlainTextString(NodeList nodes) {
		if(nodes == null || nodes.size() == 0)
			return null;
		
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < nodes.size(); ++i) {
			Node n = nodes.elementAt(i);
			if(n != null) {
				sb.append(n.toPlainTextString());
			}
		}
		
		return sb.toString();
	}
	
	
}
