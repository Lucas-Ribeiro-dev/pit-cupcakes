package br.com.pit.cupcakes.endereco.integration;

import br.com.pit.cupcakes.endereco.service.AccountOperationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.net.http.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PostalCodeClientTest {
    private final HttpClient http = mock(HttpClient.class);
    private final PostalCodeClient client = new PostalCodeClient(http,new ObjectMapper());
    @SuppressWarnings("unchecked")
    private void response(int status, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status); when(response.body()).thenReturn(body);
        when(http.sendAsync(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenReturn(CompletableFuture.completedFuture(response));
    }
    @Test @SuppressWarnings("unchecked") void mapsOnlyPostalFieldsAndAllowsEmptyStreet() {
        response(200,"{\"cep\":\"70000-001\",\"logradouro\":\"\",\"bairro\":\"\",\"localidade\":\"Brasília\",\"uf\":\"DF\",\"ibge\":\"ignorado\"}");
        var result = client.lookup("70000001");
        assertThat(result.postalCode()).isEqualTo("70000001"); assertThat(result.street()).isEmpty();
        verify(http).sendAsync(argThat(request -> request.uri().toString().equals("https://viacep.com.br/ws/70000001/json/")
                && request.headers().map().isEmpty() && request.timeout().orElseThrow().getSeconds() == 4),any(HttpResponse.BodyHandler.class));
    }
    @Test void distinguishesMissingFromInvalidAndUpstreamFailures() {
        response(200,"{\"erro\":true}"); expect(404,"CEP_NAO_ENCONTRADO");
        for (String body : new String[]{"[]","null","{}","não json","{\"cep\":null}"}) {
            response(200,body); expect(502,"SERVICO_CEP_INDISPONIVEL");
        }
        response(302,""); expect(502,"SERVICO_CEP_INDISPONIVEL");
        response(500,"conteúdo privado"); expect(502,"SERVICO_CEP_INDISPONIVEL");
    }
    @Test @SuppressWarnings("unchecked") void handlesConnectionFailureAndTimeoutWithoutRetry() {
        when(http.sendAsync(any(HttpRequest.class),any(HttpResponse.BodyHandler.class)))
                .thenReturn(CompletableFuture.failedFuture(new java.io.IOException("privado")));
        expect(502,"SERVICO_CEP_INDISPONIVEL");
        when(http.sendAsync(any(HttpRequest.class),any(HttpResponse.BodyHandler.class)))
                .thenReturn(CompletableFuture.failedFuture(new HttpTimeoutException("privado")));
        expect(504,"CONSULTA_CEP_EXPIRADA");
        verify(http,times(2)).sendAsync(any(HttpRequest.class),any(HttpResponse.BodyHandler.class));
    }
    @Test @SuppressWarnings("unchecked") void limitsTotalWaitIncludingBodyAndCancels() {
        CompletableFuture<HttpResponse<String>> pending = new CompletableFuture<>();
        when(http.sendAsync(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenReturn(pending);
        long start = System.nanoTime(); expect(504,"CONSULTA_CEP_EXPIRADA");
        assertThat(java.time.Duration.ofNanos(System.nanoTime()-start)).isLessThan(java.time.Duration.ofSeconds(5));
        assertThat(pending.isCancelled()).isTrue();
    }
    @Test void invalidPostalCodeDoesNotCallExternalService() {
        assertThatThrownBy(() -> client.lookup("https://outro/" )).isInstanceOf(jakarta.validation.ValidationException.class);
        verifyNoInteractions(http);
    }
    private void expect(int status, String code) {
        assertThatThrownBy(() -> client.lookup("70000001")).isInstanceOfSatisfying(AccountOperationException.class,
                exception -> { assertThat(exception.getStatus()).isEqualTo(status); assertThat(exception.getCode()).isEqualTo(code); });
    }
}
