package ardaaydinkilinc.Cam_Sise.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration.
 * Enables JWT authentication in Swagger UI.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Palet & Separatör Havuz Yönetim Sistemi API")
                        .version("1.0.0")
                        .description("""
                                Cam şişe dolum sektöründe kullanılan yeniden kullanılabilir palet ve separatörlerin
                                geri toplama sürecini yöneten sistem.

                                ## Authentication
                                Bu API JWT token tabanlı authentication kullanıyor.

                                **Test için kullanıcılar:**
                                - **ADMIN**: username=`admin`, password=`admin123`
                                - **COMPANY_STAFF**: username=`staff`, password=`staff123`
                                - **CUSTOMER**: username=`customer`, password=`customer123`

                                **Token almak için:**
                                1. POST /api/auth/login endpoint'ini kullan
                                2. Dönen JWT token'ı kopyala
                                3. Sağ üstteki "Authorize" butonuna tıkla
                                4. "Bearer " prefix'i olmadan token'ı yapıştır
                                """)
                        .contact(new Contact()
                                .name("Arda Aydın Kılınç")
                                .email("ardaaydinkilinc@example.com")
                                .url("https://github.com/ardaaydinkilinc"))
                        .license(new License()
                                .name("BTÜ Bitirme Tezi")
                                .url("https://btu.edu.tr")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT token'ı buraya girin (Bearer prefix'i olmadan)")));
    }
}
