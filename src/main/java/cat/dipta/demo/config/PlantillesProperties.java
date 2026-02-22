package cat.dipta.demo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "plantilles") //=>http://dev01.intranet.dtgna:8000/plantilles/rest
public class PlantillesProperties {
    private String url;
    private String user;
    private String password;

    private int connectionTimeout = 1000000000;
    private int readTimeout = 1000000000;

    private int longConnectionTimeout = 1000000000;
    private int longReadTimeout = 1000000000;
}