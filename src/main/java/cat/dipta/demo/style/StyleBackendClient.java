package cat.dipta.demo.style;

import cat.dipta.demo.url.PlantillesApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class StyleBackendClient {
    private final RestTemplate restTemplate;
    private final PlantillesApi plantillesApi;

    public ResponseEntity<StyleResponse> findAll() {
        return restTemplate.exchange(plantillesApi.styles(), HttpMethod.GET, HttpEntity.EMPTY, StyleResponse.class);
    }

    public ResponseEntity<Style> findById(Long id) {
        String url = plantillesApi.stylesFindById(id);
        return restTemplate.exchange(url, HttpMethod.GET, HttpEntity.EMPTY, Style.class);
    }

    public ResponseEntity<Style> saveStyle(Style style) {
        HttpEntity<Style> req = new HttpEntity<>(style);
        return restTemplate.exchange(plantillesApi.styles(), HttpMethod.POST, req, Style.class);
    }

    public ResponseEntity<Style> updateStyle(Style style) {
        HttpEntity<Style> req = new HttpEntity<>(style);
        String url = plantillesApi.stylesFindById(style.getId());
        return restTemplate.exchange(url, HttpMethod.PUT, req, Style.class);
    }

    public ResponseEntity<Void> deleteStyle(Long id) {
        String url = plantillesApi.stylesFindById(id);
        return restTemplate.exchange(url, HttpMethod.DELETE, HttpEntity.EMPTY, Void.class);
    }

    public ResponseEntity<EnsLocalResponse> findAllEnsLocal() {
        return restTemplate.exchange(plantillesApi.ensLocals(), HttpMethod.GET, HttpEntity.EMPTY, EnsLocalResponse.class);
    }

    public ResponseEntity<String> getStyleCss(Long styleId) {
        String url = plantillesApi.stylesCss(styleId);
        return restTemplate.exchange(url, HttpMethod.GET, HttpEntity.EMPTY, String.class);
    }

}
