package co.caio.cerberus.search;

import static co.caio.cerberus.search.IndexField.*;

import co.caio.cerberus.lucene.FloatThresholdField;
import co.caio.cerberus.model.Recipe;
import java.io.IOException;
import java.nio.file.Path;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.facet.FacetField;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;

public interface Indexer {
  void addRecipe(Recipe recipe) throws IOException;

  int numDocs();

  void close() throws IOException;

  void commit() throws IOException;

  void mergeSegments() throws IOException;

  Searcher buildSearcher();

  class Builder {
    private Directory indexDirectory;
    private Directory taxonomyDirectory;
    private Analyzer analyzer;
    private IndexWriterConfig writerConfig;
    private IndexWriterConfig.OpenMode openMode;

    public Builder reset() {
      indexDirectory = null;
      taxonomyDirectory = null;
      analyzer = null;
      writerConfig = null;
      openMode = null;
      return this;
    }

    public Builder dataDirectory(Path dir) {
      if (!dir.toFile().isDirectory()) {
        throw new IndexBuilderException(String.format("'%s' is not a directory", dir));
      }
      try {
        indexDirectory = FileSystem.openDirectory(dir.resolve(FileSystem.INDEX_DIR_NAME), true);
        taxonomyDirectory =
            FileSystem.openDirectory(dir.resolve(FileSystem.TAXONOMY_DIR_NAME), true);
      } catch (Exception e) {
        throw new IndexBuilderException(e.getMessage());
      }
      return this;
    }

    public Builder analyzer(Analyzer an) {
      analyzer = an;
      return this;
    }

    private Builder openMode(IndexWriterConfig.OpenMode mode) {
      openMode = mode;
      return this;
    }

    public Builder createMode() {
      return openMode(IndexWriterConfig.OpenMode.CREATE);
    }

    public Builder appendMode() {
      return openMode(IndexWriterConfig.OpenMode.APPEND);
    }

    public Builder createOrAppendMode() {
      return openMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
    }

    public Indexer build() {
      if (analyzer == null) {
        analyzer = IndexConfiguration.DEFAULT_ANALYZER;
      }

      if (writerConfig == null) {
        writerConfig = new IndexWriterConfig(analyzer);
      }

      if (openMode == null) {
        throw new IndexBuilderException("Missing `openMode`");
      }

      if (indexDirectory == null) {
        throw new IndexBuilderException("Missing `indexDirectory`");
      }

      if (taxonomyDirectory == null) {
        throw new IndexBuilderException("Missing `taxonomyDirectory`");
      }

      writerConfig.setOpenMode(openMode);

      try {
        return new IndexerImpl(
            new IndexWriter(indexDirectory, writerConfig),
            new DirectoryTaxonomyWriter(taxonomyDirectory, openMode));
      } catch (IOException e) {
        throw new IndexBuilderException(String.format("Failure creating index writer: %s", e));
      }
    }

    private static final class IndexerImpl implements Indexer {
      private final IndexWriter indexWriter;
      private final DirectoryTaxonomyWriter taxonomyWriter;
      private final FacetsConfig facetsConfig;

      private IndexerImpl(IndexWriter writer, DirectoryTaxonomyWriter taxWriter) {
        indexWriter = writer;
        taxonomyWriter = taxWriter;
        facetsConfig = IndexConfiguration.getFacetsConfig();
      }

      @Override
      public void addRecipe(Recipe recipe) throws IOException {
        // FIXME temporarily drop unsearchable fields?
        var doc = new Document();

        doc.add(new StoredField(RECIPE_ID, recipe.recipeId()));
        doc.add(new StringField(CRAWL_URL, recipe.crawlUrl(), Field.Store.YES));

        doc.add(new TextField(NAME, recipe.name(), Field.Store.YES));

        var fulltext =
            Stream.concat(
                    Stream.of(recipe.name()),
                    Stream.concat(recipe.instructions().stream(), recipe.ingredients().stream()))
                .collect(Collectors.joining("\n"));
        doc.add(new TextField(FULLTEXT, fulltext, Field.Store.NO));

        recipe
            .diets()
            .forEach(
                (diet, score) -> {
                  doc.add(new FloatPoint(IndexField.getFieldNameForDiet(diet), score));
                  doc.add(new FloatThresholdField(score, FACET_DIET, diet));
                });

        recipe.keywords().forEach(kw -> doc.add(new FacetField(FACET_KEYWORD, kw)));

        recipe.ingredients().forEach(i -> doc.add(new TextField(INGREDIENTS, i, Field.Store.NO)));
        addOptionalIntField(doc, NUM_INGREDIENTS, OptionalInt.of(recipe.ingredients().size()));

        addOptionalIntField(doc, PREP_TIME, recipe.prepTime());
        addOptionalIntField(doc, COOK_TIME, recipe.cookTime());
        addOptionalIntField(doc, TOTAL_TIME, recipe.totalTime());

        addOptionalIntField(doc, CALORIES, recipe.calories());
        addOptionalIntField(doc, CARBOHYDRATE_CONTENT, recipe.carbohydrateContent());
        addOptionalIntField(doc, FAT_CONTENT, recipe.fatContent());
        addOptionalIntField(doc, PROTEIN_CONTENT, recipe.proteinContent());

        indexWriter.addDocument(facetsConfig.build(taxonomyWriter, doc));
      }

      private void addOptionalIntField(
          Document doc,
          String fieldName,
          @SuppressWarnings("OptionalUsedAsFieldOrParameterType") OptionalInt value) {
        if (value.isPresent()) {
          // For filtering
          doc.add(new IntPoint(fieldName, value.getAsInt()));
          // For sorting and range-facets
          doc.add(new NumericDocValuesField(fieldName, value.getAsInt()));
        }
      }

      @Override
      public int numDocs() {
        return indexWriter.numDocs();
      }

      @Override
      public void mergeSegments() throws IOException {
        indexWriter.forceMerge(1, true);
      }

      @Override
      public void close() throws IOException {
        indexWriter.close();
        taxonomyWriter.close();
      }

      @Override
      public void commit() throws IOException {
        indexWriter.commit();
        taxonomyWriter.commit();
      }

      @Override
      public Searcher buildSearcher() {
        return new Searcher.Builder()
            .indexReader(indexWriter.getDirectory())
            .taxonomyReader(taxonomyWriter.getDirectory())
            .build();
      }
    }
  }

  class IndexBuilderException extends RuntimeException {
    IndexBuilderException(String message) {
      super(message);
    }
  }
}
