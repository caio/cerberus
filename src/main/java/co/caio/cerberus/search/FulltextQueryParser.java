package co.caio.cerberus.search;

import java.util.Collections;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.simple.SimpleQueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;

class FulltextQueryParser extends SimpleQueryParser {

  private static final int FEATURES = NOT_OPERATOR | PHRASE_OPERATOR | WHITESPACE_OPERATOR;

  FulltextQueryParser(Analyzer analyzer) {
    super(analyzer, Collections.singletonMap(IndexField.FULLTEXT, 1f), FEATURES);
    setDefaultOperator(Occur.MUST);
  }

  @Override
  public Query parse(String input) {
    if (input.equals("*")) {
      return new MatchNoDocsQuery("MatchAllDocsQuery() not allowed");
    }
    return super.parse(input);
  }
}
