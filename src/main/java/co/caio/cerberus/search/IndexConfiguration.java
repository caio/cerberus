package co.caio.cerberus.search;

import static co.caio.cerberus.search.IndexField.*;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.facet.FacetsConfig;

public class IndexConfiguration {
  private static final FacetsConfig facetsConfig;

  static {
    facetsConfig = new FacetsConfig();
    facetsConfig.setIndexFieldName(FACET_DIET, FACET_DIET);
    facetsConfig.setMultiValued(FACET_DIET, true);

    facetsConfig.setIndexFieldName(FACET_KEYWORD, FACET_KEYWORD);
    facetsConfig.setMultiValued(FACET_KEYWORD, true);
  }

  public static FacetsConfig getFacetsConfig() {
    return facetsConfig;
  }

  public static Analyzer getAnalyzer() {
    return new StandardAnalyzer();
  }
}
