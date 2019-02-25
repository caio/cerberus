package co.caio.cerberus.boot;

import co.caio.cerberus.model.SearchQuery;
import co.caio.cerberus.model.SearchQuery.RangedSpec;
import co.caio.cerberus.model.SearchQuery.SortOrder;
import java.util.Map;
import java.util.Scanner;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
class SearchParameterParser {
  private final int pageSize;

  SearchParameterParser(@Qualifier("searchPageSize") int pageSize) {
    this.pageSize = pageSize;
  }

  SearchQuery buildQuery(Map<String, String> params) {
    var builder = new SearchQuery.Builder().maxResults(pageSize);

    var fulltext = params.getOrDefault("q", "").strip();

    if (fulltext.length() < 3) {
      throw new SearchParameterException("At least three characters are required");
    }

    builder.fulltext(fulltext);

    // TODO jdk12 switches plz
    params.forEach(
        (param, value) -> {
          switch (param) {
            case "q":
              // Ignored: handled outside the switch
              break;
            case "sort":
              builder.sort(parseSortOrder(value));
              break;
            case "ni":
              builder.numIngredients(parseRange(value));
              break;
            case "tt":
              builder.totalTime(parseRange(value));
              break;
            case "n_k":
              builder.calories(parseRange(value));
              break;
            case "n_f":
              builder.fatContent(parseRange(value));
              break;
            case "n_c":
              builder.carbohydrateContent(parseRange(value));
              break;
            case "diet":
              var threshold = Float.parseFloat(params.getOrDefault("science", "1"));
              builder.putDietThreshold(value, threshold);
              break;
            case "science":
              // Ignored: handled on "diet" right above
              break;
            case "page":
              // page starts from 1, not 0
              var pageNumber = parseUnsignedInt(value);
              if (pageNumber > 30) {
                throw new SearchParameterException(
                    "For performance reasons, viewing pages 31+ is not allowed.");
              }
              builder.offset((pageNumber - 1) * pageSize);
              break;
            default:
              throw new SearchParameterException("Unknown parameter " + param);
          }
        });

    return builder.build();
  }

  private int parseUnsignedInt(String value) {
    try {
      return Integer.parseUnsignedInt(value);
    } catch (NumberFormatException ex) {
      throw new SearchParameterException("Can't parse a number >= 0 from " + value);
    }
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
      case "calories":
        return SortOrder.CALORIES;
    }
    throw new SearchParameterException("Invalid sort order: " + order);
  }

  RangedSpec parseRange(String input) {
    try {
      if (input.contains(",")) {
        var scanner = new Scanner(input).useDelimiter(",");

        int start = scanner.nextInt();
        int end = scanner.nextInt();

        // We encode a range like [x, infinity[ as x,0
        var spec = RangedSpec.of(start, end == 0 ? Integer.MAX_VALUE : end);
        if (scanner.hasNext()) {
          throw new SearchParameterException("Invalid range: " + input);
        }
        return spec;
      } else {
        return RangedSpec.of(0, parseUnsignedInt(input));
      }
    } catch (SearchParameterException rethrown) {
      throw rethrown;
    } catch (Exception swallowed) {
      throw new SearchParameterException("Invalid range: " + input);
    }
  }

  static class SearchParameterException extends RuntimeException {
    SearchParameterException(String message) {
      super(message);
    }
  }
}
