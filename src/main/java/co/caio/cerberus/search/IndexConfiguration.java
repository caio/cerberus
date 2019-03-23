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
  private final CategoryExtractor categoryExtractor;

  IndexConfiguration(Path baseDirectory, CategoryExtractor categoryExtractor) {
    if (!baseDirectory.toFile().isDirectory()) {
      throw new IndexConfigurationException("Not a directory: " + baseDirectory);
    }

    this.baseDirectory = baseDirectory;
    this.analyzer = new EnglishAnalyzer();

    this.categoryExtractor = categoryExtractor;
    this.facetsConfig = new FacetsConfig();
    categoryExtractor.multiValuedCategories().forEach(c -> facetsConfig.setMultiValued(c, true));
  }

  FacetsConfig getFacetsConfig() {
    return facetsConfig;
  }

  Analyzer getAnalyzer() {
    return analyzer;
  }

  CategoryExtractor getCategoryExtractor() {
    return categoryExtractor;
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
