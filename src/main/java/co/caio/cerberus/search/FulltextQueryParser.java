package co.caio.cerberus.search;

import java.util.Map;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.simple.SimpleQueryParser;
import org.apache.lucene.search.BooleanClause.Occur;

class FulltextQueryParser extends SimpleQueryParser {

  private static final int FEATURES = NOT_OPERATOR | PHRASE_OPERATOR | WHITESPACE_OPERATOR;

  private static final Map<String, Float> weights = Map.of(IndexField.FULL_RECIPE, 1F);

  FulltextQueryParser(Analyzer analyzer) {
    super(analyzer, weights, FEATURES);
    setDefaultOperator(Occur.MUST);
  }
}
