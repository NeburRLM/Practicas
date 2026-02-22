package cat.dipta.demo.url;

import cat.dipta.demo.config.PlantillesProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class PlantillesApi {
    private static final String TEMPLATES = "/template";
    private static final String VARIABLES_IMPORT = "/variables/import";
    private static final String PLANTIFUNCS = "/plantifuncs";
    private static final String TEMPLATES_APPLY = "/templates";
    private static final String STYLES = "/styles";
    private static final String ENS_LOCALS = "/ensLocals";
    private static final String ATTACHMENTS = "/attachments";

    private final PlantillesProperties props;

    //TEMPLATES//
    public String templates() {
        return UriComponentsBuilder.fromUriString(props.getUrl()).path(TEMPLATES).toUriString();
    }

    public String templatesFindById(Long id) {
        return UriComponentsBuilder.fromUriString(props.getUrl()).path(TEMPLATES).pathSegment(String.valueOf(id)).toUriString();
    }

    public String importVariablesFromModel(String modelId) {
        return UriComponentsBuilder.fromUriString(props.getUrl())
                .path(VARIABLES_IMPORT)
                .queryParam("modelId", modelId)
                .toUriString();
    }

    public String plantiFuncs() {
        return UriComponentsBuilder.fromUriString(props.getUrl()).path(PLANTIFUNCS).toUriString();
    }

    public String templateImport() {
        return UriComponentsBuilder.fromUriString(templates())
                .pathSegment("import")
                .toUriString();
    }

    public String templateExport(Long id) {
        return UriComponentsBuilder.fromUriString(templatesFindById(id))
                .pathSegment("export")
                .toUriString();
    }

    public String processTemplateForPreview() {
        return UriComponentsBuilder.fromUriString(templates())
                .pathSegment("processTemplateForPreview")
                .toUriString();
    }

    public String templateApply(String templateCode) {
        return UriComponentsBuilder.fromUriString(props.getUrl())
                .path(TEMPLATES_APPLY)
                .pathSegment(templateCode, "apply")
                .toUriString();
    }

    //STYLES//
    public String styles() {
        return UriComponentsBuilder.fromUriString(props.getUrl()).path(STYLES).toUriString();
    }

    public String stylesFindById(Long id) {
        return UriComponentsBuilder.fromUriString(props.getUrl()).path(STYLES).pathSegment(String.valueOf(id)).toUriString();
    }

    public String ensLocals() {
        return UriComponentsBuilder.fromUriString(props.getUrl()).path(ENS_LOCALS).toUriString();
    }

    public String stylesCss(Long styleId) {
        return UriComponentsBuilder.fromUriString(styles())
                .pathSegment("css")
                .pathSegment(styleId + ".css")
                .toUriString();
    }

    //ATTACHMENTS//
    public String attachments() {
        return UriComponentsBuilder.fromUriString(props.getUrl()).path(ATTACHMENTS).toUriString();
    }

    public String attachmentsFindById(Long id) {
        return UriComponentsBuilder.fromUriString(props.getUrl()).path(ATTACHMENTS).pathSegment(String.valueOf(id)).toUriString();
    }
}
