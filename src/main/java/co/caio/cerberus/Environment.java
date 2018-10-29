package co.caio.cerberus;

import co.caio.cerberus.ImmutableBuildStatus.Builder;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import java.io.IOException;
import java.util.Properties;
import org.immutables.value.Value;
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

    buildStatus =
        new BuildStatus.Builder()
            .commitId(props.getProperty("git.commit.id"))
            .describedCommit(props.getProperty("git.commit.id.describe"))
            .isDirty(props.getProperty("git.dirty"))
            .commitTime(props.getProperty("git.commit.time"))
            .buildTime(props.getProperty("git.build.time"))
            .build();

    logger.info("{}", buildStatus);
  }

  public static ObjectMapper getObjectMapper() {
    return mapper;
  }

  public static BuildStatus getBuildStatus() {
    return buildStatus;
  }

  @Value.Style(
      visibility = Value.Style.ImplementationVisibility.PACKAGE,
      overshadowImplementation = true)
  @JsonSerialize(as = ImmutableBuildStatus.class)
  @JsonDeserialize(as = ImmutableBuildStatus.class)
  @Value.Immutable
  interface BuildStatus {
    String commitId();

    String describedCommit();

    String commitTime();

    String buildTime();

    boolean isDirty();

    class Builder extends ImmutableBuildStatus.Builder {
      Builder isDirty(String text) {
        switch (text.toLowerCase()) {
          case "true":
            isDirty(true);
            break;
          case "false":
            isDirty(false);
          default:
            isDirty(true); // default to true as `isDirty` is bad
        }
        return this;
      }
    }
  }
}
