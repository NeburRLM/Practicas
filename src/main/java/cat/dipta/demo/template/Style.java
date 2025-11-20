package cat.dipta.demo.template;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class Style {
    private long id;
    private String code;
    private String name;
    private String rules;
    private EnsLocal ensLocal;

    @Getter
    @Setter
    public static class EnsLocal {
        private String id;
        private String nom;
    }
}
