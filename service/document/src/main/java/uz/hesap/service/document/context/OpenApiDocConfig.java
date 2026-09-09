package uz.hesap.service.document.context;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tashqi hamkorlar uchun OpenAPI spetsifikatsiyasi — faqat `/openapi/**` yo'llari. Ichki kabinet
 * endpointlari bu guruhga tushmaydi.
 *
 * <p>Spec: `GET /document/v3/api-docs/openapi` (gateway orqali `/v3/api-docs/openapi`,
 * hamkorlarga esa `https://open.hesap.uz/api-docs`).
 *
 * <p>Hamkor `https://open.hesap.uz/v1/...` manzilidan foydalanadi — nginx uni ichki
 * `/openapi/v1/...` ga o'giradi. Shu sabab spec'da yo'llardan `/openapi` prefiksi olib
 * tashlanadi va server sifatida subdomen ko'rsatiladi: spec'dagi manzil hamkor
 * chaqiradigan haqiqiy URL bilan bir xil bo'ladi (Postman/codegen to'g'ri ishlaydi).
 */
@Configuration
public class OpenApiDocConfig {

  private static final String API_KEY_SCHEME = "apiKeyAuth";
  private static final String INTERNAL_PREFIX = "/openapi";
  private static final String PUBLIC_BASE_URL = "https://open.hesap.uz";

  // Ichki (kabinet/mobil) endpointlar guruhi. GroupedOpenApi bean paydo bo'lgach
  // springdoc guruhsiz `/document/v3/api-docs` ni bermaydi — shu sabab eski spec
  // shu guruh sifatida `/document/v3/api-docs/document` da qoladi (gateway route mos).
  @Bean
  public GroupedOpenApi internalApiGroup() {
    return GroupedOpenApi.builder().group("document").pathsToMatch("/document/**").build();
  }

  @Bean
  public GroupedOpenApi publicApiGroup() {
    return GroupedOpenApi.builder()
        .group("openapi")
        .pathsToMatch("/openapi/**")
        .addOpenApiCustomizer(OpenApiDocConfig::applyPublicBaseUrl)
        .addOpenApiCustomizer(
            openApi ->
                openApi
                    .info(
                        new Info()
                            .title("Hesap OpenAPI")
                            .version("1.0")
                            .description(
                                """
                                Hesap public API — shartnomalarni o'z tizimingizdan turib tuzish.

                                Autentifikatsiya: har bir so'rovda `X-API-Key: hsp_...` headeri.
                                Kalit kabinetdagi "Integratsiyalar → OpenAPI" bo'limida olinadi;
                                unga muddat (yoki abadiy) va ishlay oladigan shablonlar
                                (yoki barchasi) biriktiriladi.

                                Webhook: kalitga URL ko'rsatilsa, shartnoma hodisalari o'sha
                                manzilga POST qilinadi. Har bir so'rov `X-Hesap-Signature`
                                (HMAC-SHA256, kalit webhook secret'i) bilan imzolanadi.
                                """))
                    .components(
                        (openApi.getComponents() == null
                                ? new Components()
                                : openApi.getComponents())
                            .addSecuritySchemes(
                                API_KEY_SCHEME,
                                new SecurityScheme()
                                    .type(SecurityScheme.Type.APIKEY)
                                    .in(SecurityScheme.In.HEADER)
                                    .name("X-API-Key")
                                    .description("Kabinetda olingan OpenAPI kaliti")))
                    // setSecurityItem EMAS: DocumentApplication'dagi klass darajasidagi
                    // `bearerAuth` bu guruhga ham tushadi va spec "bearer YOKI api-key"
                    // deb ko'rsatardi. Public API'da faqat X-API-Key ishlaydi — ro'yxatni
                    // butunlay almashtiramiz, bearerAuth sxemasini esa olib tashlaymiz.
                    .security(List.of(new SecurityRequirement().addList(API_KEY_SCHEME))))
        .addOpenApiCustomizer(OpenApiDocConfig::dropBearerScheme)
        .build();
  }

  // Ichki JWT sxemasi hamkorlar spec'ida ortiqcha — codegen undan bearer-token
  // klient yasamasligi uchun components'dan chiqarib tashlaymiz.
  private static void dropBearerScheme(final OpenAPI openApi) {
    if (openApi.getComponents() != null && openApi.getComponents().getSecuritySchemes() != null) {
      openApi.getComponents().getSecuritySchemes().remove("bearerAuth");
    }
  }

  // Yo'llardan ichki `/openapi` prefiksini olib tashlaydi va serverni public
  // subdomenga qo'yadi: `/openapi/v1/contracts` -> `https://open.hesap.uz` + `/v1/contracts`.
  private static void applyPublicBaseUrl(final OpenAPI openApi) {
    openApi.setServers(
        List.of(new Server().url(PUBLIC_BASE_URL).description("Hesap OpenAPI")));

    if (openApi.getPaths() == null) {
      return;
    }
    Paths rewritten = new Paths();
    rewritten.setExtensions(openApi.getPaths().getExtensions());
    openApi
        .getPaths()
        .forEach(
            (path, item) ->
                rewritten.addPathItem(
                    path.startsWith(INTERNAL_PREFIX)
                        ? path.substring(INTERNAL_PREFIX.length())
                        : path,
                    item));
    openApi.setPaths(rewritten);
  }
}
