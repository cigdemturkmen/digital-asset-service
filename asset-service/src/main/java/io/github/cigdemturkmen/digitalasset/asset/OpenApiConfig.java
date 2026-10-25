package io.github.cigdemturkmen.digitalasset.asset;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Metadata shown at the top of Swagger UI and in the generated OpenAPI document (/v3/api-docs). */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI assetServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Asset Service API")
                .version("v1")
                .description("Wallets for the EURT tokenized deposit: mint, burn, transfer and ledger history."));
    }
}
