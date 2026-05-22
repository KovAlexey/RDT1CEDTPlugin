package org.kovalexey.rdt1c.debug.ui.utils;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Function;
import com._1c.g5.v8.dt.bsl.model.FormalParam;
import com._1c.g5.v8.dt.debug.core.model.values.IBslValue;
import com._1c.g5.v8.dt.debug.core.model.IBslVariable;

/**
 * Generates BSL-Doc compliant comment blocks based on active debug session values.
 */
public class BslDocGenerator {

    /**
     * Generates a BSL-Doc comment block for the given method and parameter values.
     *
     * @param method the AST method node
     * @param paramValues the values of the parameters captured from debug context
     * @return the generated BSL-Doc comment string
     */
    public static String generateDoc(Method method, Map<String, IBslValue> paramValues) {
        StringBuilder sb = new StringBuilder();
        sb.append("// <Описание ").append(method instanceof com._1c.g5.v8.dt.bsl.model.Function ? "функции" : "процедуры").append(">\n");
        sb.append("//\n");
        
        if (!method.getFormalParams().isEmpty()) {
            sb.append("// Параметры:\n");
            for (FormalParam param : method.getFormalParams()) {
                String paramName = param.getName();
                IBslValue val = paramValues.get(paramName);
                if (val == null && paramName != null) {
                    val = paramValues.get(paramName.toLowerCase());
                }
                if (val != null) {
                    buildTypeDescription(paramName, val, 0, sb);
                } else {
                    sb.append("//  ").append(paramName).append(" - Произвольный\n");
                }
            }
        }
        
        if (method instanceof com._1c.g5.v8.dt.bsl.model.Function) {
            sb.append("//\n// Возвращаемое значение:\n//  Произвольный - <Описание возвращаемого значения>\n");
        }
        
        return sb.toString();
    }

    private static final int MAX_NESTING_LEVEL = 3;

    private static class UnwrappedKeyValue {
        public final String key;
        public final IBslValue value;

        public UnwrappedKeyValue(String key, IBslValue value) {
            this.key = key;
            this.value = value;
        }
    }

    private static UnwrappedKeyValue unwrapKeyValue(IBslValue val) {
        if (val == null) {
            return null;
        }
        String typeName = val.getValueTypeName();
        if ("КлючИЗначение".equalsIgnoreCase(typeName)) {
            try {
                IBslVariable[] vars = val.getVariables();
                String keyStr = null;
                IBslValue valVal = null;
                if (vars != null) {
                    for (IBslVariable v : vars) {
                        if ("Ключ".equalsIgnoreCase(v.getName())) {
                            IBslValue kv = v.getValue();
                            if (kv != null) {
                                keyStr = kv.getValueString();
                                if (keyStr != null) {
                                    keyStr = keyStr.replaceAll("^\"|\"$", "");
                                }
                            }
                        } else if ("Значение".equalsIgnoreCase(v.getName())) {
                            valVal = v.getValue();
                        }
                    }
                }
                if (keyStr != null && valVal != null) {
                    return new UnwrappedKeyValue(keyStr, valVal);
                }
            } catch (Exception e) {
                // Fallback
            }
        }
        return null;
    }

    private static String getPrefixForLevel(int level, String name) {
        if (level == 0) {
            return "//  " + name + " - ";
        } else {
            StringBuilder prefix = new StringBuilder("//  ");
            for (int i = 0; i < level - 1; i++) {
                prefix.append("  ");
            }
            for (int i = 0; i < level; i++) {
                prefix.append("*");
            }
            prefix.append(" ").append(name).append(" - ");
            return prefix.toString();
        }
    }

    private static void buildTypeDescription(String name, IBslValue value, int level, StringBuilder sb) {
        if (value == null) {
            sb.append(getPrefixForLevel(level, name)).append("Произвольный\n");
            return;
        }

        UnwrappedKeyValue unwrapped = unwrapKeyValue(value);
        if (unwrapped != null) {
            buildTypeDescription(unwrapped.key, unwrapped.value, level, sb);
            return;
        }

        String typeName = value.getValueTypeName();
        if (typeName == null) {
            typeName = "Произвольный";
        }

        appendValueDetails(name, typeName, value, level, sb);
    }

    private static void appendValueDetails(String name, String typeName, IBslValue value, int level, StringBuilder sb) {
        String prefix = getPrefixForLevel(level, name);

        if ("Структура".equalsIgnoreCase(typeName)) {
            if (level < MAX_NESTING_LEVEL) {
                boolean hasKeys = false;
                StringBuilder keysSb = new StringBuilder();
                try {
                    IBslVariable[] vars = value.getVariables();
                    if (vars != null && vars.length > 0) {
                        hasKeys = true;
                        for (IBslVariable v : vars) {
                            IBslValue childVal = v.getValue();
                            String childName = v.getName();
                            UnwrappedKeyValue unwrappedChild = unwrapKeyValue(childVal);
                            if (unwrappedChild != null) {
                                buildTypeDescription(unwrappedChild.key, unwrappedChild.value, level + 1, keysSb);
                            } else {
                                buildTypeDescription(childName, childVal, level + 1, keysSb);
                            }
                        }
                    }
                } catch (Exception e) {
                    // Fallback
                }
                if (hasKeys) {
                    sb.append(prefix).append("Структура:\n").append(keysSb.toString());
                } else {
                    sb.append(prefix).append("Структура\n");
                }
            } else {
                sb.append(prefix).append("Структура\n");
            }
        } else if ("Соответствие".equalsIgnoreCase(typeName)) {
            if (level < MAX_NESTING_LEVEL) {
                sb.append(prefix).append("Соответствие из КлючИЗначение:\n");
                IBslValue keyVal = null;
                IBslValue valVal = null;
                try {
                    IBslVariable[] vars = value.getVariables();
                    if (vars != null && vars.length > 0) {
                        IBslValue firstEntry = vars[0].getValue();
                        if (firstEntry != null) {
                            UnwrappedKeyValue unwrappedEntry = unwrapKeyValue(firstEntry);
                            if (unwrappedEntry != null) {
                                IBslVariable[] entryVars = firstEntry.getVariables();
                                if (entryVars != null) {
                                    for (IBslVariable ev : entryVars) {
                                        if ("Ключ".equalsIgnoreCase(ev.getName())) {
                                            keyVal = ev.getValue();
                                        } else if ("Значение".equalsIgnoreCase(ev.getName())) {
                                            valVal = ev.getValue();
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // Fallback
                }

                if (keyVal != null) {
                    buildTypeDescription("Ключ", keyVal, level + 1, sb);
                } else {
                    StringBuilder fallbackPrefix = new StringBuilder("//  ");
                    for (int i = 0; i < level; i++) {
                        fallbackPrefix.append("  ");
                    }
                    fallbackPrefix.append("* Ключ - Строка\n");
                    sb.append(fallbackPrefix.toString());
                }

                if (valVal != null) {
                    buildTypeDescription("Значение", valVal, level + 1, sb);
                } else {
                    StringBuilder fallbackPrefix = new StringBuilder("//  ");
                    for (int i = 0; i < level; i++) {
                        fallbackPrefix.append("  ");
                    }
                    fallbackPrefix.append("* Значение - Произвольный\n");
                    sb.append(fallbackPrefix.toString());
                }
            } else {
                sb.append(prefix).append("Соответствие\n");
            }
        } else if ("Массив".equalsIgnoreCase(typeName)) {
            String elemType = "Произвольный";
            try {
                IBslVariable[] vars = value.getVariables();
                if (vars != null && vars.length > 0) {
                    IBslValue firstVal = vars[0].getValue();
                    if (firstVal != null && firstVal.getValueTypeName() != null) {
                        elemType = firstVal.getValueTypeName();
                    }
                }
            } catch (Exception e) {
                // Fallback
            }
            sb.append(prefix).append("Массив из ").append(elemType).append("\n");
        } else if ("СписокЗначений".equalsIgnoreCase(typeName)) {
            String elemType = "Произвольный";
            try {
                IBslVariable[] vars = value.getVariables();
                if (vars != null && vars.length > 0) {
                    IBslValue firstItem = vars[0].getValue();
                    if (firstItem != null) {
                        for (IBslVariable itemProp : firstItem.getVariables()) {
                            if ("Значение".equalsIgnoreCase(itemProp.getName())) {
                                IBslValue val = itemProp.getValue();
                                if (val != null && val.getValueTypeName() != null) {
                                    elemType = val.getValueTypeName();
                                }
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Fallback
            }
            sb.append(prefix).append("СписокЗначений из ").append(elemType).append("\n");
        } else if ("ТаблицаЗначений".equalsIgnoreCase(typeName) || "СтрокаТаблицыЗначений".equalsIgnoreCase(typeName)) {
            String displayTypeName = "ТаблицаЗначений".equalsIgnoreCase(typeName) ? "ТаблицаЗначений" : "СтрокаТаблицыЗначений";
            if (level < MAX_NESTING_LEVEL) {
                boolean hasCols = false;
                StringBuilder colSb = new StringBuilder();
                try {
                    IBslVariable[] rows = value.getVariables();
                    if (rows != null && rows.length > 0) {
                        IBslValue firstRow = "ТаблицаЗначений".equalsIgnoreCase(typeName) ? rows[0].getValue() : value;
                        if (firstRow != null) {
                            IBslVariable[] cols = firstRow.getVariables();
                            if (cols != null && cols.length > 0) {
                                hasCols = true;
                                for (IBslVariable col : cols) {
                                    String colName = col.getName();
                                    IBslValue colVal = col.getValue();
                                    buildTypeDescription(colName, colVal, level + 1, colSb);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // Fallback
                }
                if (hasCols) {
                    sb.append(prefix).append(displayTypeName).append(":\n").append(colSb.toString());
                } else {
                    sb.append(prefix).append(displayTypeName).append("\n");
                }
            } else {
                sb.append(prefix).append(displayTypeName).append("\n");
            }
        } else if ("УправляемаяФорма".equalsIgnoreCase(typeName)) {
            String formName = null;
            try {
                for (IBslVariable v : value.getVariables()) {
                    if ("ИмяФормы".equalsIgnoreCase(v.getName())) {
                        formName = v.getValue().getValueString();
                        if (formName != null) {
                            formName = formName.replace("\"", "");
                        }
                        break;
                    }
                }
            } catch (Exception e) {
                // Fallback
            }
            if (formName != null && !formName.isEmpty()) {
                sb.append(prefix).append("см. ").append(formName).append("\n");
            } else {
                sb.append(prefix).append("УправляемаяФорма\n");
            }
        } else {
            sb.append(prefix).append(typeName).append("\n");
        }
    }

    public static Map<Integer, String> parseParamMapping(String mappingStr) {
        Map<Integer, String> map = new HashMap<>();
        String[] parts = (mappingStr != null && !mappingStr.isEmpty()) ? mappingStr.split(";") : new String[0];
        for (int i = 0; i < 10; i++) {
            String keyName = (i < parts.length && parts[i] != null && !parts[i].trim().isEmpty()) ? parts[i].trim() : "П" + (i + 1);
            map.put(i + 1, keyName);
        }
        return map;
    }

    public static String generateEvaluationExpression(String methodName, Method method) {
        StringBuilder sb = new StringBuilder();
        sb.append(methodName).append("(");
        List<FormalParam> params = method.getFormalParams();
        for (int i = 0; i < params.size(); i++) {
            FormalParam param = params.get(i);
            String name = param.getName();
            if (name == null || name.trim().isEmpty()) {
                sb.append("Неопределено");
            } else {
                sb.append(name.trim());
            }
            if (i < params.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(")");
        return sb.toString();
    }

    private static Map<String, IBslValue> extractFields(IBslValue value) {
        Map<String, IBslValue> fields = new HashMap<>();
        if (value == null) {
            return fields;
        }
        String typeName = value.getValueTypeName();
        try {
            IBslVariable[] vars = value.getVariables();
            if (vars != null) {
                if ("Структура".equalsIgnoreCase(typeName)) {
                    for (IBslVariable v : vars) {
                        IBslValue childVal = v.getValue();
                        String childName = v.getName();
                        if (childName != null && childVal != null) {
                            fields.put(childName.toLowerCase(), childVal);
                        }
                    }
                } else if ("Соответствие".equalsIgnoreCase(typeName)) {
                    for (IBslVariable v : vars) {
                        IBslValue entryVal = v.getValue();
                        if (entryVal != null) {
                            UnwrappedKeyValue unwrapped = unwrapKeyValue(entryVal);
                            if (unwrapped != null && unwrapped.key != null && unwrapped.value != null) {
                                fields.put(unwrapped.key.toLowerCase(), unwrapped.value);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Log or ignore
        }
        return fields;
    }

    public static String generateDocFromBslValue(Method method, IBslValue value, Map<Integer, String> mapping) {
        if (value == null) {
            return null;
        }

        String typeName = value.getValueTypeName();
        if (typeName != null && "Строка".equalsIgnoreCase(typeName)) {
            String strVal = null;
            try {
                strVal = value.getValueString();
            } catch (Exception e) {
                // Ignore
            }
            if (strVal != null) {
                if (strVal.startsWith("\"") && strVal.endsWith("\"")) {
                    strVal = strVal.substring(1, strVal.length() - 1);
                }
                strVal = strVal.replace("\"\"", "\"");
                return strVal;
            }
        }

        Map<String, IBslValue> fields = extractFields(value);

        StringBuilder sb = new StringBuilder();
        sb.append("// <Описание ").append(method instanceof Function ? "функции" : "процедуры").append(">\n");
        sb.append("//\n");

        List<FormalParam> formalParams = method.getFormalParams();
        if (!formalParams.isEmpty()) {
            sb.append("// Параметры:\n");
            for (int i = 0; i < formalParams.size(); i++) {
                FormalParam param = formalParams.get(i);
                String paramName = param.getName();
                if (paramName == null || paramName.trim().isEmpty()) {
                    continue;
                }

                int paramIndex = i + 1;
                String keyName = mapping.get(paramIndex);
                if (keyName == null) {
                    keyName = "П" + paramIndex;
                }

                IBslValue paramVal = fields.get(keyName.toLowerCase());
                if (paramVal != null) {
                    buildTypeDescription(paramName, paramVal, 0, sb);
                } else {
                    sb.append("//  ").append(paramName).append(" - Произвольный\n");
                }
            }
        }

        if (method instanceof Function) {
            sb.append("//\n// Возвращаемое значение:\n");
            IBslValue retVal = null;
            String[] retKeys = {"результат", "возврат", "result", "return"};
            for (String rk : retKeys) {
                if (fields.containsKey(rk)) {
                    retVal = fields.get(rk);
                    break;
                }
            }

            if (retVal != null) {
                buildTypeDescription("Возвращаемое значение", retVal, 0, sb);
            } else {
                sb.append("//  Произвольный - <Описание возвращаемого значения>\n");
            }
        }

        return sb.toString();
    }

    public static String generateParameterDoc(String paramName, IBslValue value) {
        if (value == null) {
            return "//  " + paramName + " - Произвольный\n";
        }
        String typeName = value.getValueTypeName();
        if (typeName != null && "Строка".equalsIgnoreCase(typeName)) {
            String strVal = null;
            try {
                strVal = value.getValueString();
            } catch (Exception e) {
                // Ignore
            }
            if (strVal != null) {
                if (strVal.startsWith("\"") && strVal.endsWith("\"")) {
                    strVal = strVal.substring(1, strVal.length() - 1);
                }
                strVal = strVal.replace("\"\"", "\"");
                if (strVal.startsWith("//")) {
                    return strVal + (strVal.endsWith("\n") ? "" : "\n");
                }
                return "//  " + paramName + " - " + strVal + "\n";
            }
        }
        StringBuilder sb = new StringBuilder();
        buildTypeDescription(paramName, value, 0, sb);
        return sb.toString();
    }
}
