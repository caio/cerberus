package co.caio.cerberus.search;

import co.caio.cerberus.model.FacetData;
import co.caio.cerberus.model.SearchQuery;
import co.caio.cerberus.model.SearchResult;
import org.apache.lucene.document.Document;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class Searcher {
    private final IndexSearcher indexSearcher;
    private final TaxonomyReader taxonomyReader;
    private final QueryInterpreter interpreter;
    private final FacetsConfig facetsConfig;

    private static final SearchResult emptyResults = new SearchResult.Builder().build();
    private static final Logger logger = LoggerFactory.getLogger(Searcher.class);

    public SearchResult search(SearchQuery query, int maxResults) {
        try {
            return _search(query, maxResults);
        } catch (Exception exception) {
            logger.error("Query <{}> failed with: {}", query, exception);
            return emptyResults;
        }
    }

    private Searcher(Searcher.Builder builder) {
        indexSearcher = new IndexSearcher(builder.indexReader);
        taxonomyReader = builder.taxonomyReader;
        interpreter = new QueryInterpreter();
        facetsConfig = FacetConfiguration.getFacetsConfig();
    }

    private SearchResult _search(SearchQuery query, int maxResults) throws Exception {
        var fc = new FacetsCollector();

        var result = FacetsCollector.search(
                indexSearcher, interpreter.toLuceneQuery(query), maxResults, fc);
        var builder = new SearchResult.Builder().totalHits(result.totalHits);

        var diets = new FastTaxonomyFacetCounts(
                IndexField.FACET_DIET, taxonomyReader, facetsConfig, fc);

        var keywords = new FastTaxonomyFacetCounts(
                IndexField.FACET_KEYWORD, taxonomyReader, facetsConfig, fc);


        // TODO maybe allow changing how many facets to retrieve
        var topDiets = diets.getTopChildren(10, IndexField.FACET_DIM_DIET);
        var topKeywords = keywords.getTopChildren(10, IndexField.FACET_DIM_KEYWORD);

        for (int i = 0; i < result.scoreDocs.length; i++) {
            Document doc = indexSearcher.doc(result.scoreDocs[i].doc);
            builder.addRecipe(
                    doc.getField(IndexField.RECIPE_ID).numericValue().longValue(),
                    doc.get(IndexField.NAME),
                    doc.get(IndexField.CRAWL_URL));
        }
        addFacetData(builder, topDiets);
        addFacetData(builder, topKeywords);
        return builder.build();
    }

    private void addFacetData(SearchResult.Builder sb, FacetResult fr) {
        if (fr == null) {
            return;
        }
        var facetDataBuilder = new FacetData.Builder().dimension(fr.dim);
        for (int i = 0; i < fr.labelValues.length; i++) {
            facetDataBuilder.addChild(fr.labelValues[i].label, fr.labelValues[i].value.longValue());
        }
        sb.addFacets(facetDataBuilder.build());
    }

    public static class Builder {
        private IndexReader indexReader;
        private TaxonomyReader taxonomyReader;

        public Builder dataDirectory(Path dir) {
            try {
                var indexDirectory = FileSystem.openDirectory(dir.resolve(FileSystem.INDEX_DIR_NAME));
                var taxonomyDirectory = FileSystem.openDirectory(dir.resolve(FileSystem.TAXONOMY_DIR_NAME));
                indexReader = DirectoryReader.open(indexDirectory);
                taxonomyReader = new DirectoryTaxonomyReader(taxonomyDirectory);
            } catch (Exception e) {
                throw new SearcherBuilderException(e.getMessage());
            }
            return this;
        }

        protected Builder indexReader(Directory dir) {
            try {
                indexReader = DirectoryReader.open(dir);
            } catch (Exception wrapped) {
                throw new SearcherBuilderException(wrapped.getMessage());
            }
            return this;
        }

        protected Builder taxonomyReader(Directory dir) {
            try {
                taxonomyReader = new DirectoryTaxonomyReader(dir);
            } catch (Exception wrapped) {
                throw new SearcherBuilderException(wrapped.getMessage());
            }
            return this;
        }

        public Searcher build() {
            if (indexReader == null) {
                throw new IllegalStateException("`indexReader` can't be null");
            }
            return new Searcher(this);
        }

        protected class SearcherBuilderException extends RuntimeException {
            SearcherBuilderException(String message) {
                super(message);
            }
        }
    }
}
