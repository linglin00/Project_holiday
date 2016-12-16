package WebScraper;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.LowerCaseTokenizer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.miscellaneous.LengthFilter;
import org.apache.lucene.analysis.pattern.PatternReplaceFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;


public class MyAnalyzer extends Analyzer {
	@Override
	public TokenStreamComponents createComponents(String fieldName, Reader reader) {
		Tokenizer source = new LowerCaseTokenizer(Version.LUCENE_CURRENT, reader);
		TokenStream result = new StopFilter(Version.LUCENE_CURRENT, source, StandardAnalyzer.STOP_WORDS_SET);
		result = new PatternReplaceFilter(result, Pattern.compile("\\d"), "", true);
		result = new LengthFilter(Version.LUCENE_CURRENT, result, 4, Integer.MAX_VALUE);
		/*CharTermAttribute termAtt = (CharTermAttribute) result.addAttribute(CharTermAttribute.class);
		StringBuilder buf = new StringBuilder();
		try {
			result.reset();
			while (result.incrementToken()) {
				if (termAtt.length() <= 3) continue;
				String word = new String(termAtt.buffer(), 0, termAtt.length());
				Matcher m = alphabets.matcher(word);
				if (m.matches()) {
					buf.append(word).append(" ");
				}
			}
		}catch (IOException e) {
			e.printStackTrace();
		}*/
		return new TokenStreamComponents(source, result);
	}
}		
		
