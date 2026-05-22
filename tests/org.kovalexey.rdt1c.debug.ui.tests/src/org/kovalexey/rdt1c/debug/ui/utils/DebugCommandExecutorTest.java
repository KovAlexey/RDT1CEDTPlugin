package org.kovalexey.rdt1c.debug.ui.utils;

import static org.junit.Assert.*;

import org.junit.Test;
import org.mockito.Mockito;

import com._1c.g5.v8.dt.debug.core.model.values.IBslValue;
import com._1c.g5.v8.dt.platform.IEObjectTypeNames;

/**
 * Automated tests for {@link DebugCommandExecutor}.
 */
public class DebugCommandExecutorTest {

    @Test
    public void testCreateTextForExecure() {
        // Simple string
        assertEquals("\"hello\"", DebugCommandExecutor.CreateTextForExecure("hello"));

        // Quotes escaping
        assertEquals("\"hello \"\"world\"\"\"", DebugCommandExecutor.CreateTextForExecure("hello \"world\""));

        // Empty string
        assertEquals("\"\"", DebugCommandExecutor.CreateTextForExecure(""));
    }

    @Test
    public void testIsItBslValueDataComposition() {
        IBslValue value1 = Mockito.mock(IBslValue.class);
        Mockito.when(value1.getValueTypeName()).thenReturn(DebugCommandExecutor.TYPE_DATA_COMPOSITION_SCHEME_RU);
        assertTrue(DebugCommandExecutor.IsItBslValueDataComposition(value1));

        IBslValue value2 = Mockito.mock(IBslValue.class);
        Mockito.when(value2.getValueTypeName()).thenReturn(IEObjectTypeNames.DATA_COMPOSITION_SCHEMA);
        assertTrue(DebugCommandExecutor.IsItBslValueDataComposition(value2));

        IBslValue value3 = Mockito.mock(IBslValue.class);
        Mockito.when(value3.getValueTypeName()).thenReturn("Структура");
        assertFalse(DebugCommandExecutor.IsItBslValueDataComposition(value3));
    }

    @Test
    public void testIsItBslValueDataCompositionSettings() {
        IBslValue value1 = Mockito.mock(IBslValue.class);
        Mockito.when(value1.getValueTypeName()).thenReturn(DebugCommandExecutor.DATA_COMPOSITION_SETTINGS_RU);
        assertTrue(DebugCommandExecutor.IsItBslValueDataCompositionSettings(value1));

        IBslValue value2 = Mockito.mock(IBslValue.class);
        Mockito.when(value2.getValueTypeName()).thenReturn(IEObjectTypeNames.DATA_COMPOSITION_SETTINGS);
        assertTrue(DebugCommandExecutor.IsItBslValueDataCompositionSettings(value2));

        IBslValue value3 = Mockito.mock(IBslValue.class);
        Mockito.when(value3.getValueTypeName()).thenReturn("Строка");
        assertFalse(DebugCommandExecutor.IsItBslValueDataCompositionSettings(value3));
    }
}
