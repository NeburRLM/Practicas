package cat.dipta.demo.config;

import cat.dipta.starters.userinfo.dto.DiptaUserInfoData;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
@RequiredArgsConstructor
public class RestTemplateConfig {

    private static final String APP_NAME = "plantilles-web";

    private final DiptaUserInfoData diptaUserInfoData;
    private final PlantillesProperties plantillesProperties;

    @Bean(name = "restTemplate")
    public RestTemplate restTemplate() {
        return createRestTemplate(plantillesProperties.getConnectionTimeout(),
                plantillesProperties.getReadTimeout());
    }

    @Bean(name = "longRestTemplate")
    public RestTemplate longOperationRestTemplate() {
        return createRestTemplate(plantillesProperties.getLongConnectionTimeout(),
                plantillesProperties.getLongReadTimeout());
    }

    private RestTemplate createRestTemplate(int connectionTimeout, int readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectionTimeout);
        factory.setReadTimeout(readTimeout);

        RestTemplate restTemplate = new RestTemplate(factory);

        restTemplate.getInterceptors().add((request, body, execution) -> {
            if (request.getHeaders().getContentType() == null) {
                request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            }
            String auth = plantillesProperties.getUser() + ":" + plantillesProperties.getPassword();
            String encodedAuth = "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            request.getHeaders().set("Authorization", encodedAuth);

            // Headers DIPTA
            request.getHeaders().set("X-DIPTA-App-Id", APP_NAME);
            request.getHeaders().set("X-DIPTA-Remote-User-Id", diptaUserInfoData.getUser());
            request.getHeaders().set("X-DIPTA-Remote-User-Unit", diptaUserInfoData.getUnit());
            request.getHeaders().set("X-DIPTA-Remote-User-Ens", diptaUserInfoData.getEns());

            return execution.execute(request, body);
        });

        return restTemplate;
    }
}