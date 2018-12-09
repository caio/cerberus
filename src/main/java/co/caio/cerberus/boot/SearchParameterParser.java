package co.caio.cerberus.boot;

import co.caio.cerberus.model.SearchQuery;
import co.caio.cerberus.model.SearchQuery.RangedSpec;
import co.caio.cerberus.model.SearchQuery.SortOrder;
import java.util.Map;
import java.util.Scanner;
import org.springframework.web.server.ServerWebInputException;

class SearchParameterParser {
  SearchQuery buildQuery(Map<String, String> params) {
    var builder = new SearchQuery.Builder();

    params.forEach(
        (param, value) -> {
          switch (param) {
            case "q":
              builder.fulltext(value);
              break;
            case "n":
              builder.maxResults(Integer.parseInt(value));
              break;
            case "sort":
              builder.sort(parseSortOrder(value));
              break;
            case "ni":
              builder.numIngredients(parseRange(value));
              break;
            default:
              throw new ServerWebInputException("Unknown parameter " + param);
          }
        });

    return builder.build();
  }

  SortOrder parseSortOrder(String order) {
    switch (order) {
      case "cook_time":
        return SortOrder.COOK_TIME;
      case "total_time":
        return SortOrder.TOTAL_TIME;
      case "prep_time":
        return SortOrder.PREP_TIME;
      case "relevance":
        return SortOrder.RELEVANCE;
      case "num_ingredients":
        return SortOrder.NUM_INGREDIENTS;
    }
    throw new SearchParameterException("Invalid sort order: " + order);
  }

  RangedSpec parseRange(String input) {
    try {
      if (input.contains(",")) {
        var scanner = new Scanner(input).useDelimiter(",");
        var spec = RangedSpec.of(scanner.nextInt(), scanner.nextInt());
        if (scanner.hasNext()) {
          throw new SearchParameterException("Invalid range: " + input);
        }
        return spec;
      } else {
        return RangedSpec.of(0, Integer.parseInt(input));
      }
    } catch (SearchParameterException rethrown) {
      throw rethrown;
    } catch (Exception swallowed) {
      throw new SearchParameterException("Invalid range: " + input);
    }
  }

  public class SearchParameterException extends RuntimeException {
    SearchParameterException(String message) {
      super(message);
    }
  }
}