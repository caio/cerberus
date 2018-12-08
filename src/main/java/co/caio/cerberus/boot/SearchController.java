package co.caio.cerberus.boot;

import co.caio.cerberus.model.SearchQuery;
import co.caio.cerberus.model.SearchQuery.SortOrder;
import co.caio.cerberus.search.Searcher;
import javax.validation.ConstraintViolationException;
import javax.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@Validated
public class SearchController {

  private final Searcher searcher;

  public SearchController(Searcher injectedSearcher) {
    searcher = injectedSearcher;
  }

  @GetMapping("search")
  public Mono<SuccessResponse> search(
      @RequestParam("q")
          @Size(
              min = 3,
              max = 1000,
              message = "Query must be of at least two characters (max 1000)")
          String fulltext,
      @RequestParam(value = "n", defaultValue = "10") int maxResults,
      @RequestParam(value = "sort", required = false) SortOrder order) {
    var builder = new SearchQuery.Builder();

    if (fulltext != null) builder.fulltext(fulltext);
    if (order != null) builder.sort(order);

    builder.maxResults(maxResults);
    var query = builder.build();

    return Mono.fromCallable(() -> new SuccessResponse(searcher.search(query)))
        .publishOn(Schedulers.parallel());
  }

  @ExceptionHandler({
    IllegalStateException.class,
    ServerWebInputException.class,
    ConstraintViolationException.class,
  })
  @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
  @ResponseBody
  FailureResponse handleQueryBuildError(Exception exc) {
    return FailureResponse.queryError(exc.getMessage());
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ResponseBody
  FailureResponse handleException(Exception exc) {
    return FailureResponse.unknownError(exc.getMessage());
  }
}
