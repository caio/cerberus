package co.caio.cerberus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("The Recipe wrapper class")
class RecipeTest {

    @Test
    @DisplayName("Doesn't yield a valid instance on bad json")
    void fromBadJson() {
        assertFalse(Recipe.fromJson("").isPresent());
        assertFalse(Recipe.fromJson("[]").isPresent());
        assertFalse(Recipe.fromJson("--").isPresent());
        // FIXME we need very basic validation AT LEAST.
        // gonna investigate other schemas...
        assertFalse(Recipe.fromJson("{}").isPresent());
    }
}