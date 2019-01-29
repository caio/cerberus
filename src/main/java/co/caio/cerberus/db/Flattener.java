package co.caio.cerberus.db;

import co.caio.cerberus.flatbuffers.FlatRecipe;
import co.caio.cerberus.model.Recipe;
import com.google.flatbuffers.FlatBufferBuilder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Flattener {

  public final int NON_EXISTENT_OPTIONAL_INT = 0;
  public static final Flattener INSTANCE = new Flattener();

  public ByteBuffer flattenRecipe(RecipeMetadata recipe) {
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

    var instructionsOffsets =
        recipe.getInstructions().stream().mapToInt(builder::createString).toArray();
    var instructionsVectorOffset = FlatRecipe.createIngredientsVector(builder, instructionsOffsets);

    var rootTable =
        FlatRecipe.createFlatRecipe(
            builder,
            recipe.getRecipeId(),
            nameOffset,
            siteNameOffset,
            slugOffset,
            sourceOffset,
            ingredientsVectorOffset,
            instructionsVectorOffset,
            recipe.getTotalTime().orElse(NON_EXISTENT_OPTIONAL_INT),
            recipe.getCalories().orElse(NON_EXISTENT_OPTIONAL_INT));

    builder.finish(rootTable);
    return builder.dataBuffer();
  }

  public ByteBuffer flattenRecipe(Recipe recipe) {
    return flattenRecipe(RecipeMetadata.fromRecipe(recipe));
  }
}
