package co.caio.cerberus;

import co.caio.cerberus.model.Recipe;
import co.caio.cerberus.model.SearchQuery;
import co.caio.cerberus.model.SearchResult;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Serializer {
  private static final Logger logger = LoggerFactory.getLogger(Serializer.class);

  private static final ObjectMapper mapper;

  static {
    mapper = new ObjectMapper();
    mapper.registerModule(new Jdk8Module());
    mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
  }

  public Optional<String> write(Object obj) {
    try {
      return Optional.of(mapper.writeValueAsString(obj));
    } catch (Exception e) {
      logger.error("Failed to serialize {}", obj, e);
      return Optional.empty();
    }
  }

  public Optional<Recipe> readRecipe(String json) {
    return read(json, Recipe.class);
  }

  public Optional<SearchQuery> readSearchQuery(String json) {
    return read(json, SearchQuery.class);
  }

  public Optional<SearchResult> readSearchResult(String json) {
    return read(json, SearchResult.class);
  }

  private <T> Optional<T> read(String json, Class<T> clz) {
    try {
      return Optional.of(mapper.readValue(json, clz));
    } catch (Exception e) {
      logger.error("Failed to read json <{}>", json, e);
      return Optional.empty();
    }
  }
}
