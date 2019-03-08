package co.caio.cerberus.lucene;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.jupiter.api.Test;

class PhoneticEnglishAnalyzerTest {

  private final Analyzer analyzer = new PhoneticEnglishAnalyzer();

  @Test
  void cinnamonTypoMatches() {
    assertFalse(intersection(extractTokens("cinamon"), extractTokens("cinnamon")).isEmpty());
  }

  private Set<String> intersection(final Set<String> s1, final Collection<String> s2) {
    var result = new HashSet<>(s1);
    result.retainAll(s2);
    return result;
  }

  private Set<String> extractTokens(String text) {
    var tokenStream = analyzer.tokenStream("unused", new StringReader(text));
    var charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);

    Set<String> tokens = new HashSet<>();

    try {
      tokenStream.reset();
      while (tokenStream.incrementToken()) {
        tokens.add(charTermAttribute.toString());
      }
      tokenStream.close();
    } catch (IOException wrapped) {
      throw new RuntimeException(wrapped);
    }

    return tokens;
  }
}
