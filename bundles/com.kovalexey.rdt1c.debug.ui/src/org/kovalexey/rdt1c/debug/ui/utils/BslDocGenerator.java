package org.kovalexey.rdt1c.debug.ui.utils;

import java.util.Map;
import com._1c.g5.v8.dt.bsl.model.Method;
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

    private static void buildTypeDescription(String name, IBslValue value, int level, StringBuilder sb) {
        String typeName = value.getValueTypeName();
        if (typeName == null) {
            typeName = "Произвольный";
        }

        if (level == 0) {
            sb.append("//  ").append(name).append(" - ");
            appendValueDetails(typeName, value, level, sb);
        } else {
            String indent = (level == 1) ? "* " : "  ** ";
            sb.append("//  ").append(indent).append(name).append(" - ");
            appendValueDetails(typeName, value, level, sb);
        }
    }

    private static void appendValueDetails(String typeName, IBslValue value, int level, StringBuilder sb) {
        if ("Структура".equalsIgnoreCase(typeName)) {
            if (level < 2) {
                boolean hasKeys = false;
                StringBuilder keysSb = new StringBuilder();
                try {
                    IBslVariable[] vars = value.getVariables();
                    if (vars != null && vars.length > 0) {
                        hasKeys = true;
                        for (IBslVariable v : vars) {
                            buildTypeDescription(v.getName(), v.getValue(), level + 1, keysSb);
                        }
                    }
                } catch (Exception e) {
                    // Fallback
                }
                if (hasKeys) {
                    sb.append("Структура:\n").append(keysSb.toString());
                } else {
                    sb.append("Структура\n");
                }
            } else {
                sb.append("Структура\n");
            }
        } else if ("Соответствие".equalsIgnoreCase(typeName)) {
            sb.append("Соответствие из КлючИЗначение:\n");
            String kType = "Строка";
            String vType = "Произвольный";
            try {
                IBslVariable[] vars = value.getVariables();
                if (vars != null && vars.length > 0) {
                    IBslValue firstVal = vars[0].getValue();
                    if (firstVal != null && firstVal.getValueTypeName() != null) {
                        vType = firstVal.getValueTypeName();
                    }
                }
            } catch (Exception e) {
                // Fallback
            }
            String indent = (level == 0) ? "  " : "    ";
            sb.append("//").append(indent).append("* Ключ - ").append(kType).append("\n");
            sb.append("//").append(indent).append("* Значение - ").append(vType).append("\n");
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
            sb.append("Массив из ").append(elemType).append("\n");
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
            sb.append("СписокЗначений из ").append(elemType).append("\n");
        } else if ("ТаблицаЗначений".equalsIgnoreCase(typeName) || "СтрокаТаблицыЗначений".equalsIgnoreCase(typeName)) {
            String displayTypeName = "ТаблицаЗначений".equalsIgnoreCase(typeName) ? "ТаблицаЗначений" : "СтрокаТаблицыЗначений";
            if (level < 2) {
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
                                    String colType = "Произвольный";
                                    if (col.getValue() != null && col.getValue().getValueTypeName() != null) {
                                        colType = col.getValue().getValueTypeName();
                                    }
                                    String colIndent = (level == 0) ? "* " : "  ** ";
                                    colSb.append("//  ").append(colIndent).append(colName).append(" - ").append(colType).append("\n");
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // Fallback
                }
                if (hasCols) {
                    sb.append(displayTypeName).append(":\n").append(colSb.toString());
                } else {
                    sb.append(displayTypeName).append("\n");
                }
            } else {
                sb.append(displayTypeName).append("\n");
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
                sb.append("см. ").append(formName).append("\n");
            } else {
                sb.append("УправляемаяФорма\n");
            }
        } else {
            sb.append(typeName).append("\n");
        }
    }
}
