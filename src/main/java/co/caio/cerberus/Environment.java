package co.caio.cerberus;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Environment {
  private static final Logger logger = LoggerFactory.getLogger(Environment.class);

  private static final ObjectMapper mapper;
  private static final BuildStatus buildStatus;

  static {
    mapper = new ObjectMapper();
    mapper.registerModule(new Jdk8Module());
    mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

    var props = new Properties();
    try {
      props.load(Environment.class.getClassLoader().getResourceAsStream("git.properties"));
    } catch (IOException rethrown) {
      throw new RuntimeException(rethrown);
    }

    buildStatus = BuildStatus.fromProperties(props);
  }

  public static ObjectMapper getObjectMapper() {
    return mapper;
  }

  public static BuildStatus getBuildStatus() {
    return buildStatus;
  }

  public interface BuildStatus {
    String commitId();

    String commitTime();

    String buildTime();

    boolean isDirty();

    boolean isValid();

    List<String> invalidReasons();

    static BuildStatus fromProperties(Properties props) {
      return new BuildStatusImpl(props);
    }

    class BuildStatusImpl implements BuildStatus {

      private static final DateTimeFormatter formatter =
          DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

      static final String UNDEFINED = "undefined";
      static final String GIT_COMMIT_ID = "git.commit.id";
      static final String GIT_COMMIT_TIME = "git.commit.time";
      static final String GIT_BUILD_TIME = "git.build.time";
      static final String GIT_DIRTY = "git.dirty";

      private final List<String> invalidReasons;
      private final String commitId;
      private final String commitTime;
      private final String buildTime;
      private final boolean isDirty;

      BuildStatusImpl(Properties props) {
        List<String> reasons = new LinkedList<>();

        // TODO validate commit ids
        commitId = getProperty(props, GIT_COMMIT_ID, reasons);

        commitTime = getTime(props, GIT_COMMIT_TIME, reasons);
        buildTime = getTime(props, GIT_BUILD_TIME, reasons);

        var gitDirty = getProperty(props, GIT_DIRTY, reasons);
        switch (gitDirty.toLowerCase()) {
          case "true":
            reasons.add("Dirty workdir during build");
            isDirty = true;
            break;
          case "false":
            isDirty = false;
            break;
          case UNDEFINED:
            // reason populated already
            isDirty = true;
            break;
          default:
            reasons.add(String.format("Invalid value for %s: %s", GIT_DIRTY, gitDirty));
            isDirty = true;
            break;
        }

        invalidReasons = Collections.unmodifiableList(reasons);
      }

      private String getProperty(Properties props, String key, List<String> reason) {
        var result = props.getProperty(key, UNDEFINED);
        if (result.equals(UNDEFINED)) {
          reason.add(String.format("Property %s undefined", key));
        }
        return result;
      }

      private String getTime(Properties props, String key, List<String> reason) {
        var result = getProperty(props, key, reason);
        if (!result.equals(UNDEFINED) && isTimeInvalid(result)) {
          reason.add(String.format("Invalid datetime for property %s: %s", key, result));
        }
        return result;
      }

      public List<String> invalidReasons() {
        return invalidReasons;
      }

      @Override
      public String commitId() {
        return commitId;
      }

      @Override
      public String commitTime() {
        return commitTime;
      }

      @Override
      public String buildTime() {
        return buildTime;
      }

      @Override
      public boolean isDirty() {
        return isDirty;
      }

      public boolean isValid() {
        return invalidReasons.isEmpty();
      }

      private boolean isTimeInvalid(String time) {
        try {
          ZonedDateTime.parse(time, formatter);
          return false;
        } catch (DateTimeParseException logged) {
          logger.debug("Exception parsing time string", logged);
          return true;
        }
      }
    }
  }
}
