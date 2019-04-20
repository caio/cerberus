package co.caio.cerberus.db;

import co.caio.cerberus.flatbuffers.FlatRecipe;
import co.caio.cerberus.model.Recipe;
import com.google.flatbuffers.FlatBufferBuilder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public class FlatBufferSerializer {

  static final int NON_EXISTENT_OPTIONAL_INT = -1;
  static final float NON_EXISTENT_OPTIONAL_FLOAT = -1;

  public static final FlatBufferSerializer INSTANCE = new FlatBufferSerializer();

  ByteBuffer flattenRecipe(RecipeMetadata recipe) {
    var builder =
        new FlatBufferBuilder(
            5_000, cap -> ByteBuffer.allocateDirect(cap).order(ByteOrder.LITTLE_ENDIAN));

    var nameOffset = builder.createString(recipe.getName());
    var sourceOffset = builder.createString(recipe.getCrawlUrl());

    var siteNameOffset = builder.createString(recipe.getSiteName());
    var slugOffset = builder.createString(recipe.getSlug());

    var ingredientsOffsets =
        recipe.getIngredients().stream().mapToInt(builder::createString).toArray();
    var ingredientsVectorOffset = FlatRecipe.createIngredientsVector(builder, ingredientsOffsets);

    var similarIdsVectorOffset =
        FlatRecipe.createSimilarIdsVector(
            builder, recipe.getSimilarRecipeIds().stream().mapToLong(Long::valueOf).toArray());

    var rootTable =
        FlatRecipe.createFlatRecipe(
            builder,
            recipe.getRecipeId(),
            nameOffset,
            siteNameOffset,
            slugOffset,
            sourceOffset,
            ingredientsVectorOffset,
            recipe.getPrepTime().orElse(NON_EXISTENT_OPTIONAL_INT),
            recipe.getCookTime().orElse(NON_EXISTENT_OPTIONAL_INT),
            recipe.getTotalTime().orElse(NON_EXISTENT_OPTIONAL_INT),
            recipe.getCalories().orElse(NON_EXISTENT_OPTIONAL_INT),
            (float) recipe.getFatContent().orElse(NON_EXISTENT_OPTIONAL_FLOAT),
            (float) recipe.getProteinContent().orElse(NON_EXISTENT_OPTIONAL_FLOAT),
            (float) recipe.getCarbohydrateContent().orElse(NON_EXISTENT_OPTIONAL_FLOAT),
            similarIdsVectorOffset);

    builder.finish(rootTable);
    return builder.dataBuffer();
  }

  public ByteBuffer flattenRecipe(Recipe recipe) {
    return flattenRecipe(RecipeMetadata.fromRecipe(recipe));
  }

  FlatRecipe readRecipe(ByteBuffer buffer) {
    return FlatRecipe.getRootAsFlatRecipe(buffer);
  }

  OptionalInt readOptionalInt(int number) {
    if (number == NON_EXISTENT_OPTIONAL_INT) {
      return OptionalInt.empty();
    } else {
      return OptionalInt.of(number);
    }
  }

  OptionalDouble readOptionalDouble(float number) {
    if (number == NON_EXISTENT_OPTIONAL_FLOAT) {
      return OptionalDouble.empty();
    } else {
      return OptionalDouble.of(number);
    }
  }
}
