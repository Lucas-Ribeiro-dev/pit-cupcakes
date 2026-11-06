package br.com.pit.cupcakes.endereco.integration;

import br.com.pit.cupcakes.endereco.dto.PostalCodeResponse;
import br.com.pit.cupcakes.endereco.service.*;
import com.fasterxml.jackson.databind.*;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.concurrent.*;

@Component
public class PostalCodeClient {
    private final HttpClient client;
    private final ObjectMapper mapper;
    @org.springframework.beans.factory.annotation.Autowired
    public PostalCodeClient(ObjectMapper mapper) {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).followRedirects(HttpClient.Redirect.NEVER).build(),mapper);
    }
    PostalCodeClient(HttpClient client, ObjectMapper mapper) { this.client = client; this.mapper = mapper; }
    public PostalCodeResponse lookup(String input) {
        String postalCode = AddressValidation.postalCode(input);
        var request = HttpRequest.newBuilder(URI.create("https://viacep.com.br/ws/" + postalCode + "/json/"))
                .timeout(Duration.ofSeconds(4)).GET().build();
        var pending = client.sendAsync(request,HttpResponse.BodyHandlers.ofString());
        try {
            var response = pending.get(4, TimeUnit.SECONDS);
            if (response.statusCode() != 200) throw unavailable();
            return parse(response.body(),postalCode);
        } catch (TimeoutException exception) {
            pending.cancel(true);
            throw new AccountOperationException(504,"CONSULTA_CEP_EXPIRADA","A consulta de CEP demorou demais. Preencha manualmente ou tente novamente.");
        } catch (InterruptedException exception) {
            pending.cancel(true);
            Thread.currentThread().interrupt();
            throw unavailable();
        } catch (ExecutionException exception) {
            if (exception.getCause() instanceof HttpTimeoutException) throw new AccountOperationException(504,"CONSULTA_CEP_EXPIRADA","A consulta de CEP demorou demais. Preencha manualmente ou tente novamente.");
            throw unavailable();
        }
    }
    PostalCodeResponse parse(String body, String expected) {
        try {
            JsonNode data = mapper.readTree(body);
            if (data == null || !data.isObject()) throw unavailable();
            if (data.path("erro").asBoolean(false)) throw new AccountOperationException(404,"CEP_NAO_ENCONTRADO","CEP não encontrado. Confira ou preencha manualmente.");
            String postalCode = AddressValidation.postalCode(requiredText(data,"cep"));
            String street = requiredText(data,"logradouro");
            String neighborhood = requiredText(data,"bairro");
            String city = requiredText(data,"localidade");
            String state = requiredText(data,"uf");
            if (!postalCode.equals(expected) || street.length() > 160 || neighborhood.length() > 100 || city.isBlank() || city.length() > 100
                    || !state.matches("AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO")) throw unavailable();
            return new PostalCodeResponse(postalCode,street,neighborhood,city,state);
        } catch (com.fasterxml.jackson.core.JsonProcessingException | jakarta.validation.ValidationException exception) { throw unavailable(); }
    }
    private String requiredText(JsonNode data, String key) {
        if (!data.path(key).isTextual()) throw unavailable();
        return data.path(key).asText();
    }
    private AccountOperationException unavailable() {
        return new AccountOperationException(502,"SERVICO_CEP_INDISPONIVEL","Consulta de CEP indisponível. Você pode preencher manualmente.");
    }
}
