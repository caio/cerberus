package co.caio.cerberus.search;

import co.caio.cerberus.model.SearchQuery;
import co.caio.cerberus.model.SearchResult;
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

    public Builder dataDirectory(Path dir) {
      return indexConfiguration(new IndexConfiguration(dir))
          .indexReader(indexConfiguration.openIndexDirectory())
          .taxonomyReader(indexConfiguration.openTaxonomyDirectory());
    }

    Builder indexConfiguration(IndexConfiguration conf) {
      indexConfiguration = conf;
      return this;
    }

    Builder indexReader(Directory dir) {
      try {
        indexReader = DirectoryReader.open(dir);
      } catch (Exception wrapped) {
        throw new SearcherBuilderException(wrapped);
      }
      return this;
    }

    Builder taxonomyReader(Directory dir) {
      try {
        taxonomyReader = new DirectoryTaxonomyReader(dir);
      } catch (Exception wrapped) {
        throw new SearcherBuilderException(wrapped);
      }
      return this;
    }

    public Builder searchPolicy(SearchPolicy policy) {
      searchPolicy = policy;
      return this;
    }

    public Searcher build() {
      if (indexReader == null || taxonomyReader == null || indexConfiguration == null) {
        throw new IllegalStateException("`dataDirectory` not set");
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
    }
  }

  class SearcherException extends RuntimeException {
    SearcherException(Exception e) {
      super(e);
    }
  }
}
