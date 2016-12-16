package WebScraper;

import java.io.IOException;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseTokenizer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.pattern.PatternReplaceFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;

public class ScapeUtil {
	private static ScapeUtil su = null;
	
	public static ScapeUtil getInstance() {
		if (su == null)
			su = new ScapeUtil();
		return su;
	}
	
	private ScapeUtil() {}
	
	public String removeStopwords(String inputText, CharArraySet stopSet) {
		StringBuffer buf = new StringBuffer();
        Pattern pattern = Pattern.compile("[a-z]+");
        Tokenizer source = new LowerCaseTokenizer(Version.LUCENE_CURRENT, new StringReader(inputText));
        TokenStream result = new StopFilter(Version.LUCENE_CURRENT, source, stopSet);
		result = new PatternReplaceFilter(result, Pattern.compile("\\d"), "", true);
		CharTermAttribute termAtt = (CharTermAttribute) result.addAttribute(CharTermAttribute.class);
		try {
			result.reset();
			while (result.incrementToken()) {
				if (termAtt.length() <= 3) continue;
				String word = new String(termAtt.buffer(), 0, termAtt.length()).toLowerCase();
				Matcher m = pattern.matcher(word);
				if (m.matches()) {
					buf.append(word).append(" ");
				}
			}
			result.close();
		}catch (IOException e1) {
			e1.printStackTrace();
		}
		return buf.toString();
	}
}
