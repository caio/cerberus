package co.caio.cerberus;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AppTest {
    @Test
    public void testHello() {
        assertEquals(App.hello(), "Hello, world!");
    }
}