package cl.duocuc.pedidos360.pedidos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "azure.ad")
public record AzureAdProperties(String tenantId, String clientId) {

    public String issuerUri() {
        return "https://login.microsoftonline.com/" + tenantId + "/v2.0";
    }

    public String jwkSetUri() {
        return "https://login.microsoftonline.com/" + tenantId + "/discovery/v2.0/keys";
    }
}
