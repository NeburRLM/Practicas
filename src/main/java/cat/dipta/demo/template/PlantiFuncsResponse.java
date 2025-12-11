package cat.dipta.demo.template;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class PlantiFuncsResponse {
    @JsonProperty("plantifuncs")
    private List<PlantiFunc> plantifuncs;
}