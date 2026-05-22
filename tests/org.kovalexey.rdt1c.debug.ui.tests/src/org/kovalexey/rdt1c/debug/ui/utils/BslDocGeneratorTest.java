package org.kovalexey.rdt1c.debug.ui.utils;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.junit.Test;
import org.mockito.Mockito;

import com._1c.g5.v8.dt.bsl.model.FormalParam;
import com._1c.g5.v8.dt.bsl.model.Function;
import com._1c.g5.v8.dt.bsl.model.Procedure;
import com._1c.g5.v8.dt.debug.core.model.IBslVariable;
import com._1c.g5.v8.dt.debug.core.model.values.IBslValue;

/**
 * Automated tests for {@link BslDocGenerator}.
 */
public class BslDocGeneratorTest {

    private IBslValue mockPrimitive(String type, String valStr) throws Exception {
        IBslValue value = Mockito.mock(IBslValue.class);
        Mockito.when(value.getValueTypeName()).thenReturn(type);
        Mockito.when(value.getValueString()).thenReturn(valStr);
        return value;
    }

    private IBslVariable mockVar(String name, IBslValue value) {
        IBslVariable var = Mockito.mock(IBslVariable.class);
        Mockito.when(var.getName()).thenReturn(name);
        Mockito.when(var.getValue()).thenReturn(value);
        return var;
    }

    private IBslValue mockStructure(Map<String, IBslValue> fields) throws Exception {
        IBslValue structVal = Mockito.mock(IBslValue.class);
        Mockito.when(structVal.getValueTypeName()).thenReturn("Структура");
        IBslVariable[] vars = new IBslVariable[fields.size()];
        int idx = 0;
        for (Map.Entry<String, IBslValue> entry : fields.entrySet()) {
            vars[idx++] = mockVar(entry.getKey(), entry.getValue());
        }
        Mockito.when(structVal.getVariables()).thenReturn(vars);
        return structVal;
    }

    private IBslValue mockKeyValue(String key, IBslValue value) throws Exception {
        IBslValue entryVal = Mockito.mock(IBslValue.class);
        Mockito.when(entryVal.getValueTypeName()).thenReturn("КлючИЗначение");
        IBslVariable keyVar = mockVar("Ключ", mockPrimitive("Строка", "\"" + key + "\""));
        IBslVariable valVar = mockVar("Значение", value);
        Mockito.when(entryVal.getVariables()).thenReturn(new IBslVariable[]{keyVar, valVar});
        return entryVal;
    }

    private IBslValue mockMap(Map<String, IBslValue> entries) throws Exception {
        IBslValue mapVal = Mockito.mock(IBslValue.class);
        Mockito.when(mapVal.getValueTypeName()).thenReturn("Соответствие");
        IBslVariable[] vars = new IBslVariable[entries.size()];
        int idx = 0;
        for (Map.Entry<String, IBslValue> entry : entries.entrySet()) {
            IBslValue entryVal = mockKeyValue(entry.getKey(), entry.getValue());
            vars[idx++] = mockVar("Элемент", entryVal);
        }
        Mockito.when(mapVal.getVariables()).thenReturn(vars);
        return mapVal;
    }

    private IBslValue mockArray(IBslValue... elements) throws Exception {
        IBslValue arrVal = Mockito.mock(IBslValue.class);
        Mockito.when(arrVal.getValueTypeName()).thenReturn("Массив");
        IBslVariable[] vars = new IBslVariable[elements.length];
        for (int i = 0; i < elements.length; i++) {
            vars[i] = mockVar("Индекс_" + i, elements[i]);
        }
        Mockito.when(arrVal.getVariables()).thenReturn(vars);
        return arrVal;
    }

    private IBslValue mockValueList(IBslValue... elements) throws Exception {
        IBslValue listVal = Mockito.mock(IBslValue.class);
        Mockito.when(listVal.getValueTypeName()).thenReturn("СписокЗначений");
        IBslVariable[] vars = new IBslVariable[elements.length];
        for (int i = 0; i < elements.length; i++) {
            IBslValue itemVal = Mockito.mock(IBslValue.class);
            Mockito.when(itemVal.getValueTypeName()).thenReturn("ЭлементСпискаЗначений");
            IBslVariable valVar = mockVar("Значение", elements[i]);
            Mockito.when(itemVal.getVariables()).thenReturn(new IBslVariable[]{valVar});
            vars[i] = mockVar("Элемент_" + i, itemVal);
        }
        Mockito.when(listVal.getVariables()).thenReturn(vars);
        return listVal;
    }

    private IBslValue mockTable(Map<String, IBslValue> rowColumns) throws Exception {
        IBslValue tableVal = Mockito.mock(IBslValue.class);
        Mockito.when(tableVal.getValueTypeName()).thenReturn("ТаблицаЗначений");
        
        IBslValue rowVal = Mockito.mock(IBslValue.class);
        Mockito.when(rowVal.getValueTypeName()).thenReturn("СтрокаТаблицыЗначений");
        IBslVariable[] cols = new IBslVariable[rowColumns.size()];
        int idx = 0;
        for (Map.Entry<String, IBslValue> entry : rowColumns.entrySet()) {
            cols[idx++] = mockVar(entry.getKey(), entry.getValue());
        }
        Mockito.when(rowVal.getVariables()).thenReturn(cols);
        
        IBslVariable[] rows = new IBslVariable[]{mockVar("Строка_0", rowVal)};
        Mockito.when(tableVal.getVariables()).thenReturn(rows);
        return tableVal;
    }

    private IBslValue mockForm(String formName) throws Exception {
        IBslValue formVal = Mockito.mock(IBslValue.class);
        Mockito.when(formVal.getValueTypeName()).thenReturn("УправляемаяФорма");
        IBslVariable nameVar = mockVar("ИмяФормы", mockPrimitive("Строка", "\"" + formName + "\""));
        Mockito.when(formVal.getVariables()).thenReturn(new IBslVariable[]{nameVar});
        return formVal;
    }

    private FormalParam mockParam(String name) {
        FormalParam param = Mockito.mock(FormalParam.class);
        Mockito.when(param.getName()).thenReturn(name);
        return param;
    }

    @Test
    public void testGenerateDocForProcedureNoParams() {
        Procedure procedure = Mockito.mock(Procedure.class);
        Mockito.when(procedure.getFormalParams()).thenReturn(new BasicEList<>());

        String doc = BslDocGenerator.generateDoc(procedure, new HashMap<>());
        String expected = "// <Описание процедуры>\n//\n";
        assertEquals(expected, doc);
    }

    @Test
    public void testGenerateDocForFunctionNoParams() {
        Function function = Mockito.mock(Function.class);
        Mockito.when(function.getFormalParams()).thenReturn(new BasicEList<>());

        String doc = BslDocGenerator.generateDoc(function, new HashMap<>());
        String expected = "// <Описание функции>\n//\n//\n// Возвращаемое значение:\n//  Произвольный - <Описание возвращаемого значения>\n";
        assertEquals(expected, doc);
    }

    @Test
    public void testGenerateDocWithSimpleParameters() throws Exception {
        Procedure procedure = Mockito.mock(Procedure.class);
        EList<FormalParam> params = new BasicEList<>();
        params.add(mockParam("Параметр1"));
        params.add(mockParam("Параметр2"));
        Mockito.when(procedure.getFormalParams()).thenReturn(params);

        Map<String, IBslValue> values = new HashMap<>();
        values.put("Параметр1", mockPrimitive("Число", "42"));
        values.put("Параметр2", mockPrimitive("Строка", "Тест"));

        String doc = BslDocGenerator.generateDoc(procedure, values);
        assertTrue(doc.contains("//  Параметр1 - Число"));
        assertTrue(doc.contains("//  Параметр2 - Строка"));
    }

    @Test
    public void testGenerateDocWithStructureLevel1() throws Exception {
        Procedure procedure = Mockito.mock(Procedure.class);
        EList<FormalParam> params = new BasicEList<>();
        params.add(mockParam("Парам"));
        Mockito.when(procedure.getFormalParams()).thenReturn(params);

        Map<String, IBslValue> structFields = new HashMap<>();
        structFields.put("Поле1", mockPrimitive("Число", "1"));
        structFields.put("Поле2", mockPrimitive("Булево", "Истина"));
        IBslValue struct = mockStructure(structFields);

        Map<String, IBslValue> values = new HashMap<>();
        values.put("Парам", struct);

        String doc = BslDocGenerator.generateDoc(procedure, values);
        assertTrue(doc.contains("//  Парам - Структура:"));
        assertTrue(doc.contains("//  * Поле1 - Число"));
        assertTrue(doc.contains("//  * Поле2 - Булево"));
    }

    @Test
    public void testGenerateDocWithStructureLevel3() throws Exception {
        Procedure procedure = Mockito.mock(Procedure.class);
        EList<FormalParam> params = new BasicEList<>();
        params.add(mockParam("Парам"));
        Mockito.when(procedure.getFormalParams()).thenReturn(params);

        // Level 3 nested struct
        Map<String, IBslValue> struct3Fields = new HashMap<>();
        struct3Fields.put("Внутр3", mockPrimitive("Строка", "Конец"));
        IBslValue struct3 = mockStructure(struct3Fields);

        // Level 2 nested struct
        Map<String, IBslValue> struct2Fields = new HashMap<>();
        struct2Fields.put("Внутр2", struct3);
        IBslValue struct2 = mockStructure(struct2Fields);

        // Level 1 struct
        Map<String, IBslValue> struct1Fields = new HashMap<>();
        struct1Fields.put("Внутр1", struct2);
        IBslValue struct1 = mockStructure(struct1Fields);

        Map<String, IBslValue> values = new HashMap<>();
        values.put("Парам", struct1);

        String doc = BslDocGenerator.generateDoc(procedure, values);
        assertTrue(doc.contains("//  Парам - Структура:"));
        assertTrue(doc.contains("//  * Внутр1 - Структура:"));
        assertTrue(doc.contains("//    ** Внутр2 - Структура:"));
        assertTrue(doc.contains("//      *** Внутр3 - Строка"));
    }

    @Test
    public void testGenerateDocWithUnwrappedKeyValues() throws Exception {
        Procedure procedure = Mockito.mock(Procedure.class);
        EList<FormalParam> params = new BasicEList<>();
        params.add(mockParam("Парам"));
        Mockito.when(procedure.getFormalParams()).thenReturn(params);

        Map<String, IBslValue> mapFields = new HashMap<>();
        mapFields.put("КлючТест", mockPrimitive("Число", "10"));
        IBslValue map = mockMap(mapFields);

        Map<String, IBslValue> values = new HashMap<>();
        values.put("Парам", map);

        String doc = BslDocGenerator.generateDoc(procedure, values);
        assertTrue(doc.contains("//  Парам - Соответствие из КлючИЗначение:"));
        assertTrue(doc.contains("//  * Ключ - Строка"));
        assertTrue(doc.contains("//  * Значение - Число"));
    }

    @Test
    public void testGenerateDocWithArrayAndList() throws Exception {
        Procedure procedure = Mockito.mock(Procedure.class);
        EList<FormalParam> params = new BasicEList<>();
        params.add(mockParam("Масс"));
        params.add(mockParam("Спис"));
        Mockito.when(procedure.getFormalParams()).thenReturn(params);

        IBslValue arr = mockArray(mockPrimitive("СправочникСсылка.Номенклатура", ""));
        IBslValue list = mockValueList(mockPrimitive("Число", ""));

        Map<String, IBslValue> values = new HashMap<>();
        values.put("Масс", arr);
        values.put("Спис", list);

        String doc = BslDocGenerator.generateDoc(procedure, values);
        assertTrue(doc.contains("//  Масс - Массив из СправочникСсылка.Номенклатура"));
        assertTrue(doc.contains("//  Спис - СписокЗначений из Число"));
    }

    @Test
    public void testGenerateDocWithTable() throws Exception {
        Procedure procedure = Mockito.mock(Procedure.class);
        EList<FormalParam> params = new BasicEList<>();
        params.add(mockParam("ТЗ"));
        Mockito.when(procedure.getFormalParams()).thenReturn(params);

        Map<String, IBslValue> columns = new HashMap<>();
        columns.put("Товар", mockPrimitive("СправочникСсылка.Номенклатура", ""));
        columns.put("Количество", mockPrimitive("Число", ""));
        IBslValue table = mockTable(columns);

        Map<String, IBslValue> values = new HashMap<>();
        values.put("ТЗ", table);

        String doc = BslDocGenerator.generateDoc(procedure, values);
        assertTrue(doc.contains("//  ТЗ - ТаблицаЗначений:"));
        assertTrue(doc.contains("//  * Товар - СправочникСсылка.Номенклатура"));
        assertTrue(doc.contains("//  * Количество - Число"));
    }

    @Test
    public void testGenerateDocWithForm() throws Exception {
        Procedure procedure = Mockito.mock(Procedure.class);
        EList<FormalParam> params = new BasicEList<>();
        params.add(mockParam("Форма"));
        Mockito.when(procedure.getFormalParams()).thenReturn(params);

        IBslValue form = mockForm("Справочник.Номенклатура.Форма.ФормаЭлемента");

        Map<String, IBslValue> values = new HashMap<>();
        values.put("Форма", form);

        String doc = BslDocGenerator.generateDoc(procedure, values);
        assertTrue(doc.contains("//  Форма - см. Справочник.Номенклатура.Форма.ФормаЭлемента"));
    }

    @Test
    public void testParseParamMapping() {
        Map<Integer, String> mapping = BslDocGenerator.parseParamMapping("A;B;;D");
        assertEquals("A", mapping.get(1));
        assertEquals("B", mapping.get(2));
        assertEquals("П3", mapping.get(3));
        assertEquals("D", mapping.get(4));
        assertEquals("П10", mapping.get(10));
    }

    @Test
    public void testGenerateEvaluationExpression() {
        Procedure procedure = Mockito.mock(Procedure.class);
        EList<FormalParam> params = new BasicEList<>();
        params.add(mockParam("Param1"));
        params.add(mockParam(""));
        params.add(mockParam("Param3"));
        Mockito.when(procedure.getFormalParams()).thenReturn(params);

        String expr = BslDocGenerator.generateEvaluationExpression("MyModule.MyFunc", procedure);
        assertEquals("MyModule.MyFunc(Param1, Неопределено, Param3)", expr);
    }

    @Test
    public void testGenerateDocFromBslValueString() throws Exception {
        Procedure procedure = Mockito.mock(Procedure.class);
        Mockito.when(procedure.getFormalParams()).thenReturn(new BasicEList<>());

        IBslValue strVal = mockPrimitive("Строка", "\"// custom comment block\"");
        String doc = BslDocGenerator.generateDocFromBslValue(procedure, strVal, new HashMap<>());
        assertEquals("// custom comment block", doc);
    }

    @Test
    public void testGenerateDocFromBslValueStructure() throws Exception {
        Function function = Mockito.mock(Function.class);
        EList<FormalParam> params = new BasicEList<>();
        params.add(mockParam("Param1"));
        Mockito.when(function.getFormalParams()).thenReturn(params);

        Map<String, IBslValue> structFields = new HashMap<>();
        structFields.put("key1", mockPrimitive("Число", "123"));
        structFields.put("возврат", mockPrimitive("Булево", "Ложь"));
        IBslValue struct = mockStructure(structFields);

        Map<Integer, String> mapping = new HashMap<>();
        mapping.put(1, "key1");

        String doc = BslDocGenerator.generateDocFromBslValue(function, struct, mapping);
        assertTrue(doc.contains("//  Param1 - Число"));
        assertTrue(doc.contains("//  Возвращаемое значение - Булево"));
    }

    @Test
    public void testGenerateParameterDoc() throws Exception {
        IBslValue nullVal = null;
        assertEquals("//  param1 - Произвольный\n", BslDocGenerator.generateParameterDoc("param1", nullVal));

        IBslValue strVal = mockPrimitive("Строка", "\"// custom line\"");
        assertEquals("// custom line\n", BslDocGenerator.generateParameterDoc("param1", strVal));

        IBslValue strVal2 = mockPrimitive("Строка", "\"Число\"");
        assertEquals("//  param1 - Число\n", BslDocGenerator.generateParameterDoc("param1", strVal2));

        IBslValue numVal = mockPrimitive("Число", "42");
        assertEquals("//  param1 - Число\n", BslDocGenerator.generateParameterDoc("param1", numVal));
    }
}
