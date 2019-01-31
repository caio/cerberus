package co.caio.cerberus.db;

import static org.junit.jupiter.api.Assertions.*;

import co.caio.cerberus.Util;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class FlatBufferSerializerTest {

  private final FlatBufferSerializer serializer = new FlatBufferSerializer();

  @Test
  void roundTrip() {

    var recipe = Util.getBasicRecipe();
    var recipeMetadata = RecipeMetadata.fromRecipe(recipe);

    assertEquals(recipe.recipeId(), serializer.readRecipe(serializer.flattenRecipe(recipe)).id());
    assertEquals(
        recipe.recipeId(), serializer.readRecipe(serializer.flattenRecipe(recipeMetadata)).id());
  }

  @Test
  void optionalInt() {
    assertEquals(
        OptionalInt.empty(),
        serializer.readOptionalInt(FlatBufferSerializer.NON_EXISTENT_OPTIONAL_INT));
    assertEquals(OptionalInt.of(12), serializer.readOptionalInt(12));
  }
}
