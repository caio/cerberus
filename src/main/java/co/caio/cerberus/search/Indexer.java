package co.caio.cerberus.search;

import static co.caio.cerberus.search.IndexField.*;

import co.caio.cerberus.model.Recipe;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.apache.lucene.document.*;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.facet.FacetField;
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
    private IndexConfiguration indexConfiguration;
    private IndexWriterConfig writerConfig;
    private IndexWriterConfig.OpenMode openMode;
    private Path dataDirectory;
    private CategoryExtractor categoryExtractor;

    Builder reset() {
      dataDirectory = null;
      indexDirectory = null;
      taxonomyDirectory = null;
      writerConfig = null;
      openMode = null;
      indexConfiguration = null;
      categoryExtractor = null;
      return this;
    }

    public Builder categoryExtractor(CategoryExtractor extractor) {
      categoryExtractor = extractor;
      return this;
    }

    public Builder dataDirectory(Path dir) {
      if (!dir.toFile().isDirectory()) {
        throw new IndexBuilderException(String.format("'%s' is not a directory", dir));
      }

      dataDirectory = dir;

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

    public Indexer build() {
      if (openMode == null) {
        throw new IndexBuilderException("Missing `openMode`");
      }

      if (dataDirectory == null) {
        throw new IndexBuilderException("dataDirectory() not set");
      }

      if (categoryExtractor == null) {
        categoryExtractor = CategoryExtractor.NOOP;
      }

      indexConfiguration =
          new IndexConfiguration(dataDirectory, categoryExtractor.multiValuedCategories());
      indexConfiguration.save();

      indexDirectory = indexConfiguration.openIndexDirectory();
      taxonomyDirectory = indexConfiguration.openTaxonomyDirectory();

      writerConfig = new IndexWriterConfig(indexConfiguration.getAnalyzer());
      writerConfig.setOpenMode(openMode);

      try {
        return new IndexerImpl(
            new IndexWriter(indexDirectory, writerConfig),
            new DirectoryTaxonomyWriter(taxonomyDirectory, openMode),
            indexConfiguration,
            categoryExtractor.categoryToExtractor());
      } catch (IOException e) {
        throw new IndexBuilderException(String.format("Failure creating index writer: %s", e));
      }
    }

    private static final class IndexerImpl implements Indexer {
      private final IndexWriter indexWriter;
      private final DirectoryTaxonomyWriter taxonomyWriter;
      private final IndexConfiguration indexConfiguration;
      private final Map<String, Function<Recipe, Set<String>>> categoryExtractors;

      private IndexerImpl(
          IndexWriter writer,
          DirectoryTaxonomyWriter taxWriter,
          IndexConfiguration conf,
          Map<String, Function<Recipe, Set<String>>> categoryExtractors) {
        this.indexWriter = writer;
        this.taxonomyWriter = taxWriter;
        this.indexConfiguration = conf;
        this.categoryExtractors = categoryExtractors;
      }

      @Override
      public void addRecipe(Recipe recipe) throws IOException {
        var doc = new Document();

        doc.add(new StoredField(RECIPE_ID, recipe.recipeId()));

        doc.add(new TextField(NAME, recipe.name(), Store.NO));
        recipe.instructions().forEach(i -> doc.add(new TextField(INSTRUCTIONS, i, Store.NO)));
        recipe.ingredients().forEach(i -> doc.add(new TextField(INGREDIENTS, i, Store.NO)));

        recipe
            .diets()
            .forEach(
                (diet, score) -> {
                  if (score > 0) {
                    doc.add(new FloatPoint(IndexField.getFieldNameForDiet(diet), score));
                  }
                });

        var numIngredients = recipe.ingredients().size();
        doc.add(new IntPoint(NUM_INGREDIENTS, numIngredients));
        doc.add(new NumericDocValuesField(NUM_INGREDIENTS, numIngredients));

        // Timing

        recipe
            .prepTime()
            .ifPresent(
                value -> {
                  doc.add(new IntPoint(PREP_TIME, value));
                  // For sorting
                  doc.add(new NumericDocValuesField(PREP_TIME, value));
                });

        recipe
            .cookTime()
            .ifPresent(
                value -> {
                  doc.add(new IntPoint(COOK_TIME, value));
                  // For sorting
                  doc.add(new NumericDocValuesField(COOK_TIME, value));
                });

        recipe
            .totalTime()
            .ifPresent(
                value -> {
                  doc.add(new IntPoint(TOTAL_TIME, value));
                  // For sorting
                  doc.add(new NumericDocValuesField(TOTAL_TIME, value));
                });

        // Nutrition

        recipe
            .calories()
            .ifPresent(
                value -> {
                  doc.add(new IntPoint(CALORIES, value));
                  // For sorting
                  doc.add(new NumericDocValuesField(CALORIES, value));
                });

        recipe.fatContent().ifPresent(value -> doc.add(new FloatPoint(FAT_CONTENT, (float) value)));

        recipe
            .proteinContent()
            .ifPresent(value -> doc.add(new FloatPoint(PROTEIN_CONTENT, (float) value)));

        recipe
            .carbohydrateContent()
            .ifPresent(value -> doc.add(new FloatPoint(CARBOHYDRATE_CONTENT, (float) value)));

        categoryExtractors.forEach(
            (dimension, getLabels) ->
                getLabels
                    .apply(recipe)
                    .forEach(
                        label -> {
                          doc.add(new FacetField(dimension, label));
                        }));

        indexWriter.addDocument(indexConfiguration.getFacetsConfig().build(taxonomyWriter, doc));
      }

      @Override
      public int numDocs() {
        return indexWriter.getDocStats().numDocs;
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
        return Searcher.Builder.fromOpened(
            indexConfiguration, indexWriter.getDirectory(), taxonomyWriter.getDirectory());
      }
    }
  }

  class IndexBuilderException extends RuntimeException {
    IndexBuilderException(String message) {
      super(message);
    }
  }
}
