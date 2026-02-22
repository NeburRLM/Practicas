package cat.dipta.demo;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

// TODO: En cas que l'aplicació no tingui API REST, esborrar aquest arxiu

@Profile("generate-openapi")
@Configuration
@Slf4j
@OpenAPIDefinition( // TODO: canviar els valors pels què correspongui. Nom de l'app i servers.
    info = @Info(
        title = "Dipta Demo",
        version = "1.0",
        description = "API d'accés als serveis de l'aplicació 'Dipta DEMO' de la Diputació de Tarragona",
        contact = @Contact(
            name = "Àrea de Tecnologies de la Informació i la Comunicació",
            url = "https://www.dipta.cat/diputacio/arees-organismes/tecnologies-informacio-comunicacio",
            email = "tic@dipta.cat"
        )),
    security = {
        @SecurityRequirement(name = "basicAuth")
    },
    servers = {
        @Server(
            description = "Local",
            url = "http://localhost:8080/dipta-demo"
        ),
        @Server(
            description = "DEV - Desenvolupament",
            url = "http://serveis-dev.intranet.dtgna/dipta-demo"
        ),
        @Server(
            description = "PRE - Preproducció",
            url = "http://serveis-pre.intranet.dtgna/dipta-demo"
        ),
        @Server(
            description = "PRO - Producció",
            url = "http://serveis.intranet.dtgna/dipta-demo"
        )
    }
)
@SecurityScheme(name = "basicAuth",
    description = "Autenticació BASIC amb usuari de servei",
    type = SecuritySchemeType.HTTP,
    scheme = "Basic"
)
public class OpenApiConfiguration {

}
