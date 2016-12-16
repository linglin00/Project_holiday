package WebScraper;

import java.util.Set;

import org.apache.lucene.analysis.standard.StandardAnalyzer;

public class HelpUtil {
	public static void main(String[] argv) {
		Object[] stopObjs = StandardAnalyzer.STOP_WORDS_SET.toArray();
		for(Object stopObj : stopObjs) {
			System.out.println();
		}
	}
}
