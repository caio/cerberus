package co.caio.cerberus.lucene;

import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.StopwordAnalyzerBase;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.phonetic.DoubleMetaphoneFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

public class PhoneticEnglishAnalyzer extends StopwordAnalyzerBase {

  public PhoneticEnglishAnalyzer() {
    super();
  }

  @Override
  protected TokenStreamComponents createComponents(String fieldName) {
    final Tokenizer source = new StandardTokenizer();

    TokenStream result = new EnglishPossessiveFilter(source);
    result = new LowerCaseFilter(result);
    result = new StopFilter(result, stopwords);

    result = new PorterStemFilter(result);
    // inject=true so that we emit the original (stemmed) tokens as well
    // since metaphone is very lossy. This is because I want to have a
    // way of preferring documents that actually match the original
    // input, meaning that: if I search for a typo I want to match against
    // things that sound like it, but I still want things that match with
    // the typo to be ranked better
    result = new DoubleMetaphoneFilter(result, 6, true);

    return new TokenStreamComponents(source, result);
  }

  @Override
  protected TokenStream normalize(String fieldName, TokenStream in) {
    return new LowerCaseFilter(in);
  }
}
