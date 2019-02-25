package co.caio.cerberus.search;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class FulltextQueryParserTest {

  private static FulltextQueryParser parser;

  @BeforeAll
  static void init() {
    parser = new FulltextQueryParser(new StandardAnalyzer());
  }

  @Test
  void simpleTermQuery() {
    var query = parser.parse("oil");
    assertEquals("fulltext:oil", query.toString());
  }

  @Test
  void simplePhraseQuery() {
    var query = parser.parse("\"phrase query\"");
    assertEquals("fulltext:\"phrase query\"", query.toString());
  }

  @Test
  void simpleTermNegation() {
    var query = parser.parse("-oil");
    // NOT queries are encoded as (NOT:term AND match_all_docs)
    assertEquals("-fulltext:oil *:*", query.toString());
  }

  @Test
  void simplePhraseNegation() {
    var query = parser.parse("-\"black pepper\"");
    assertEquals("-fulltext:\"black pepper\" *:*", query.toString());
  }

  @Test
  void implicitGrouping() {
    var query = parser.parse("-oil salt"); // with salt, without oil
    assertEquals("+(-fulltext:oil *:*) +fulltext:salt", query.toString());
  }

  @Test
  void matchAllDocsIsAllowed() {
    var query = parser.parse("*");
    assertEquals("*:*", query.toString());
    assertEquals(MatchAllDocsQuery.class, query.getClass());
  }
}
