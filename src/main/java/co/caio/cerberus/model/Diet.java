package co.caio.cerberus.model;

import java.util.Collections;
import java.util.Set;

public class Diet {

  public static final Set<String> knownDiets =
      Collections.unmodifiableSet(Set.of("keto", "paleo", "lowcarb", "glutenfree", "vegan"));

  static boolean isKnown(String diet) {
    return knownDiets.contains(diet);
  }
}
