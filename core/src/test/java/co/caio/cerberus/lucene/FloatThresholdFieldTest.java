package co.caio.cerberus.lucene;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FloatThresholdFieldTest {
  @Test
  void validation() {
    assertThrows(
        IllegalArgumentException.class, () -> new FloatThresholdField(-1f, "dimension", "path"));
    assertThrows(
        IllegalArgumentException.class, () -> new FloatThresholdField(1.1f, "dimension", "path"));
    assertDoesNotThrow(() -> new FloatThresholdField(1.0f, "dimension", "path"));
    assertDoesNotThrow(() -> new FloatThresholdField(0f, "dimension", "path"));
  }
}
