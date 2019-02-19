package co.caio.cerberus.search;

import static co.caio.cerberus.search.IndexField.*;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.facet.FacetsConfig;

class IndexConfiguration {
  private final FacetsConfig facetsConfig;
  private final Analyzer analyzer;

  IndexConfiguration() {
    facetsConfig = new FacetsConfig();
    facetsConfig.setIndexFieldName(FACET_DIET, FACET_DIET);
    facetsConfig.setMultiValued(FACET_DIET, true);

    facetsConfig.setIndexFieldName(FACET_KEYWORD, FACET_KEYWORD);
    facetsConfig.setMultiValued(FACET_KEYWORD, true);

    analyzer = new EnglishAnalyzer();
  }

  FacetsConfig getFacetsConfig() {
    return facetsConfig;
  }

  Analyzer getAnalyzer() {
    return analyzer;
  }
}
