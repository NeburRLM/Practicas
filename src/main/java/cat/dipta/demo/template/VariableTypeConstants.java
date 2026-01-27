package cat.dipta.demo.template;

import java.util.Map;

public class VariableTypeConstants {

    private VariableTypeConstants() {
        throw new UnsupportedOperationException("Classe d'utilitats no instanciable");
    }

    private static final Map<Integer, String> TYPE_MAP = Map.ofEntries(
            Map.entry(0, "short"),
            Map.entry(1, "int"),
            Map.entry(2, "long"),
            Map.entry(3, "float"),
            Map.entry(4, "double"),
            Map.entry(5, "char"),
            Map.entry(6, "boolean"),
            Map.entry(7, "String"),
            Map.entry(8, "List"),
            Map.entry(9, "Map"),
            Map.entry(10, "Data"),
            Map.entry(11, "Html"),
            Map.entry(12, "taula csv o html")
    );

    private static final Map<Integer, String> DEFAULT_VALUES = Map.ofEntries(
            Map.entry(0, "0"),
            Map.entry(1, "0"),
            Map.entry(2, "0"),
            Map.entry(3, "0.0"),
            Map.entry(4, "0.0"),
            Map.entry(5, "a"),
            Map.entry(6, "true"),
            Map.entry(7, "text de prova"),
            Map.entry(8, "[]"),
            Map.entry(9, "[:]"),
            Map.entry(10, "2018-01-01T12:56:42+0200"),
            Map.entry(11, "<p>html de prova</p>"),
            Map.entry(12, "<table><tr><td>csv/html de prova</td></tr></table>")
    );

    public static Map<Integer, String> getTypeMap() {
        return TYPE_MAP;
    }

    public static Map<Integer, String> getDefaultValues() {
        return DEFAULT_VALUES;
    }
}
