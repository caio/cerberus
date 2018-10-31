package co.caio.cerberus;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Properties;
import org.junit.jupiter.api.Test;

class BuildStatusTest {

  @Test
  void fromEmptyProperties() {
    var props = new Properties();

    var buildStatus = Environment.BuildStatus.fromProperties(props);
    assertFalse(buildStatus.isValid());
    assertEquals(4, buildStatus.invalidReasons().size());
    assertTrue(
        buildStatus
            .invalidReasons()
            .stream()
            .anyMatch(s -> s.contains("git.build.time undefined")));
    assertTrue(
        buildStatus
            .invalidReasons()
            .stream()
            .anyMatch(s -> s.contains("git.commit.time undefined")));
    assertTrue(
        buildStatus.invalidReasons().stream().anyMatch(s -> s.contains("git.commit.id undefined")));
    assertTrue(
        buildStatus.invalidReasons().stream().anyMatch(s -> s.contains("git.dirty undefined")));
  }

  @Test
  void isDirty() {
    var props = new Properties();

    props.setProperty("git.dirty", "invalid should make isDirty()=true");
    assertTrue(Environment.BuildStatus.fromProperties(props).isDirty());
    props.setProperty("git.dirty", "true");
    assertTrue(Environment.BuildStatus.fromProperties(props).isDirty());
    props.setProperty("git.dirty", "false");
    assertFalse(Environment.BuildStatus.fromProperties(props).isDirty());
  }

  @Test
  void commitId() {
    var props = new Properties();
    props.setProperty("git.commit.id", "any value makes this pass");
    var buildStatus = Environment.BuildStatus.fromProperties(props);
    assertTrue(
        buildStatus
            .invalidReasons()
            .stream()
            .noneMatch(s -> s.contains("git.commit.id undefined")));
  }

  @Test
  void timeParsing() {
    var props = new Properties();
    var validTimeString = "2018-10-29T20:11:42+0100";

    // Both invalid
    props.setProperty("git.build.time", "this is invalid");
    props.setProperty("git.commit.time", "this is also invalid");
    var buildStatus = Environment.BuildStatus.fromProperties(props);
    assertFalse(buildStatus.isValid());
    assertTrue(buildStatus.invalidReasons().stream().anyMatch(s -> s.contains("git.build.time")));
    assertTrue(buildStatus.invalidReasons().stream().anyMatch(s -> s.contains("git.commit.time")));

    // Just one invalid
    props.setProperty("git.build.time", validTimeString);
    buildStatus = Environment.BuildStatus.fromProperties(props);
    assertFalse(buildStatus.isValid());
    assertFalse(buildStatus.invalidReasons().stream().anyMatch(s -> s.contains("git.build.time")));
    assertTrue(buildStatus.invalidReasons().stream().anyMatch(s -> s.contains("git.commit.time")));

    // Both valid
    props.setProperty("git.commit.time", validTimeString);
    buildStatus = Environment.BuildStatus.fromProperties(props);
    assertFalse(buildStatus.isValid());
    assertFalse(buildStatus.invalidReasons().stream().anyMatch(s -> s.contains("git.build.time")));
    assertFalse(buildStatus.invalidReasons().stream().anyMatch(s -> s.contains("git.commit.time")));

    assertEquals(validTimeString, buildStatus.buildTime());
    assertEquals(validTimeString, buildStatus.commitTime());
  }

  @Test
  void basicValid() {
    var props = new Properties();
    var validTimeString = "2018-10-29T20:11:42+0100";
    props.setProperty("git.dirty", "false");
    props.setProperty("git.commit.time", validTimeString);
    props.setProperty("git.build.time", validTimeString);
    props.setProperty("git.commit.id", "valid commit id");

    var buildStatus = Environment.BuildStatus.fromProperties(props);
    assertEquals("valid commit id", buildStatus.commitId());
    assertTrue(buildStatus.isValid());
  }
}
