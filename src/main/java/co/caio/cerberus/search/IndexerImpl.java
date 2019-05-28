package co.caio.cerberus.search;

import static co.caio.cerberus.search.IndexField.CALORIES;
import static co.caio.cerberus.search.IndexField.CARBOHYDRATE_CONTENT;
import static co.caio.cerberus.search.IndexField.COOK_TIME;
import static co.caio.cerberus.search.IndexField.FAT_CONTENT;
import static co.caio.cerberus.search.IndexField.FULL_RECIPE;
import static co.caio.cerberus.search.IndexField.NUM_INGREDIENTS;
import static co.caio.cerberus.search.IndexField.PREP_TIME;
import static co.caio.cerberus.search.IndexField.PROTEIN_CONTENT;
import static co.caio.cerberus.search.IndexField.RECIPE_ID;
import static co.caio.cerberus.search.IndexField.TOTAL_TIME;
import static co.caio.cerberus.search.IndexField.getFieldNameForDiet;
import static org.apache.lucene.index.IndexWriterConfig.OpenMode.CREATE_OR_APPEND;

import co.caio.cerberus.model.Recipe;
import java.io.IOException;
import java.nio.file.Path;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.facet.FacetField;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;

public final class IndexerImpl implements Indexer {
  private final IndexWriter indexWriter;
  private final DirectoryTaxonomyWriter taxonomyWriter;
  private final IndexConfiguration indexConfiguration;
  private final CategoryExtractor categoryExtractor;

  IndexerImpl(Path dir, CategoryExtractor extractor) throws IOException {
    categoryExtractor = extractor;
    indexConfiguration = new IndexConfiguration(dir, categoryExtractor.multiValuedCategories());
    indexConfiguration.save();

    var writerConfig = new IndexWriterConfig(indexConfiguration.getAnalyzer());
    writerConfig.setOpenMode(CREATE_OR_APPEND);

    indexWriter = new IndexWriter(indexConfiguration.openIndexDirectory(), writerConfig);
    taxonomyWriter =
        new DirectoryTaxonomyWriter(indexConfiguration.openTaxonomyDirectory(), CREATE_OR_APPEND);
  }

  @Override
  public void addRecipe(Recipe recipe) throws IOException {
    var doc = new Document();

    doc.add(new StoredField(RECIPE_ID, recipe.recipeId()));
    doc.add(new LongPoint(RECIPE_ID, recipe.recipeId()));

    doc.add(new TextField(FULL_RECIPE, recipe.name(), Store.NO));
    recipe.instructions().forEach(i -> doc.add(new TextField(FULL_RECIPE, i, Store.NO)));
    recipe.ingredients().forEach(i -> doc.add(new TextField(FULL_RECIPE, i, Store.NO)));

    recipe
        .diets()
        .forEach(
            (diet, score) -> {
              if (score > 0) {
                doc.add(new FloatPoint(getFieldNameForDiet(diet), score));
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

    categoryExtractor
        .categoryToExtractor()
        .forEach(
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
}
