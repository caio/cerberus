package co.caio.cerberus.search;

import co.caio.cerberus.model.SearchQuery;
import co.caio.cerberus.model.SearchResult;
import java.io.IOException;
import java.nio.file.Path;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;

public interface Searcher {

  SearchResult search(SearchQuery query);

  int numDocs();

  class Builder {

    private IndexReader indexReader;
    private TaxonomyReader taxonomyReader;
    private IndexConfiguration indexConfiguration;
    private SearchPolicy searchPolicy;
    private Path dataDirectory;

    public Builder dataDirectory(Path dir) {
      dataDirectory = dir;
      return this;
    }

    public Builder searchPolicy(SearchPolicy policy) {
      searchPolicy = policy;
      return this;
    }

    static Searcher fromOpened(
        IndexConfiguration configuration, Directory indexDir, Directory taxonomyDir) {

      var builder = new Searcher.Builder();
      builder.indexConfiguration = configuration;
      try {
        builder.indexReader = DirectoryReader.open(indexDir);
        builder.taxonomyReader = new DirectoryTaxonomyReader(taxonomyDir);
      } catch (IOException wrapped) {
        throw new SearcherBuilderException(wrapped);
      }

      return builder.build();
    }

    public Searcher build() {

      if (dataDirectory != null && taxonomyReader == null && indexReader == null) {
        indexConfiguration = IndexConfiguration.fromBaseDirectory(dataDirectory);
        try {
          indexReader = DirectoryReader.open(indexConfiguration.openIndexDirectory());
          taxonomyReader = new DirectoryTaxonomyReader(indexConfiguration.openTaxonomyDirectory());
        } catch (IOException wrapped) {
          throw new SearcherBuilderException(wrapped);
        }
      }

      if (indexReader == null || taxonomyReader == null || indexConfiguration == null) {
        throw new SearcherBuilderException("This should never happen");
      }

      if (searchPolicy != null) {
        return new SearcherWithPolicy(this);
      } else {
        return new SearcherImpl(this);
      }
    }

    IndexReader getIndexReader() {
      return indexReader;
    }

    TaxonomyReader getTaxonomyReader() {
      return taxonomyReader;
    }

    IndexConfiguration getIndexConfiguration() {
      return indexConfiguration;
    }

    SearchPolicy getSearchPolicy() {
      return searchPolicy;
    }

    static class SearcherBuilderException extends RuntimeException {
      SearcherBuilderException(Throwable throwable) {
        super(throwable);
      }

      SearcherBuilderException(String message) {
        super(message);
      }
    }
  }

  class SearcherException extends RuntimeException {
    SearcherException(Exception e) {
      super(e);
    }
  }
}
