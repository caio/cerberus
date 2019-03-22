package co.caio.cerberus.search;

import java.io.IOException;
import java.nio.file.Path;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

class IndexConfiguration {
  static final String INDEX_DIR_NAME = "index";
  static final String TAXONOMY_DIR_NAME = "taxonomy";

  private final FacetsConfig facetsConfig;
  private final Analyzer analyzer;
  private final Path baseDirectory;

  IndexConfiguration(Path baseDirectory) {

    if (!baseDirectory.toFile().isDirectory()) {
      throw new IndexConfigurationException("Not a directory: " + baseDirectory);
    }

    this.baseDirectory = baseDirectory;

    facetsConfig = new FacetsConfig();

    // Static facets that have inclusive ranges configured
    facetsConfig.setMultiValued(IndexField.FACET_DIET, true);
    facetsConfig.setMultiValued(IndexField.FACET_TOTAL_TIME, true);
    facetsConfig.setMultiValued(IndexField.FACET_CALORIES, true);

    analyzer = new EnglishAnalyzer();
  }

  FacetsConfig getFacetsConfig() {
    return facetsConfig;
  }

  Analyzer getAnalyzer() {
    return analyzer;
  }

  Directory openIndexDirectory() {
    return uncheckedOpen(INDEX_DIR_NAME);
  }

  Directory openTaxonomyDirectory() {
    return uncheckedOpen(TAXONOMY_DIR_NAME);
  }

  private Directory uncheckedOpen(String dirName) {
    try {
      return FSDirectory.open(baseDirectory.resolve(dirName));
    } catch (IOException wrapped) {
      throw new IndexConfigurationException(wrapped);
    }
  }

  static class IndexConfigurationException extends RuntimeException {
    IndexConfigurationException(Throwable throwable) {
      super(throwable);
    }

    IndexConfigurationException(String message) {
      super(message);
    }
  }
}
