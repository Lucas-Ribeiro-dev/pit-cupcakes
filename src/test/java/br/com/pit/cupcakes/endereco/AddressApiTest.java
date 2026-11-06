package br.com.pit.cupcakes.endereco;

import br.com.pit.cupcakes.endereco.integration.PostalCodeClient;
import br.com.pit.cupcakes.identidade.entity.User;
import br.com.pit.cupcakes.identidade.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AddressApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository users;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @MockitoBean PostalCodeClient postal;
    String email;
    @BeforeEach void setup() { email = UUID.randomUUID()+"@example.com"; users.save(new User("Teste",email,"sem-login-real")); }
    Map<String,Object> payload() {
        return new LinkedHashMap<>(Map.of("destinatario"," José ","cep","70000-001","logradouro","Rua de teste","numero","S/N","bairro","Centro","cidade","Região administrativa","uf","df"));
    }
    long create() throws Exception {
        var result = mvc.perform(post("/api/conta/enderecos").with(user(email)).with(csrf()).contentType("application/json").content(mapper.writeValueAsString(payload())))
                .andExpect(status().isCreated()).andExpect(header().string("Cache-Control",org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.cep").value("70000001")).andExpect(jsonPath("$.usuario").doesNotExist()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
    }
    @Test void createsListsDetailsUpdatesAndDeletesWithOwnershipAndVersions() throws Exception {
        long id = create();
        mvc.perform(get("/api/conta/enderecos").with(user(email))).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(get("/api/conta/enderecos/"+id).with(user(email))).andExpect(status().isOk()).andExpect(jsonPath("$.destinatario").value("José"));
        var data = payload(); data.put("versao",0); data.put("numero","2");
        mvc.perform(put("/api/conta/enderecos/"+id).with(user(email)).with(csrf()).contentType("application/json").content(mapper.writeValueAsString(data)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.versao").value(1));
        mvc.perform(put("/api/conta/enderecos/"+id).with(user(email)).with(csrf()).contentType("application/json").content(mapper.writeValueAsString(data)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("ENDERECO_DESATUALIZADO"));
        mvc.perform(delete("/api/conta/enderecos/"+id+"?versao=0").with(user(email)).with(csrf())).andExpect(status().isConflict());
        mvc.perform(delete("/api/conta/enderecos/"+id).with(user(email)).with(csrf())).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/conta/enderecos/"+id+"?versao=-1").with(user(email)).with(csrf())).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/conta/enderecos/"+id+"?versao=1").with(user(email)).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/conta/enderecos/"+id).with(user(email))).andExpect(status().isNotFound());
    }
    @Test void hidesForeignAddressesFromAllOperationsAndQuotes() throws Exception {
        long id = create(); String other = UUID.randomUUID()+"@example.com"; users.save(new User("Outro",other,"sem-login-real"));
        mvc.perform(get("/api/conta/enderecos/"+id).with(user(other))).andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("ENDERECO_NAO_ENCONTRADO"));
        var data = payload(); data.put("versao",0);
        mvc.perform(put("/api/conta/enderecos/"+id).with(user(other)).with(csrf()).contentType("application/json").content(mapper.writeValueAsString(data))).andExpect(status().isNotFound());
        mvc.perform(delete("/api/conta/enderecos/"+id+"?versao=0").with(user(other)).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post("/api/conta/frete/cotacao").with(user(other)).with(csrf()).contentType("application/json").content("{\"modalidade\":\"ENTREGA\",\"enderecoId\":"+id+"}")).andExpect(status().isNotFound());
        mvc.perform(get("/api/conta/enderecos").with(user(other))).andExpect(content().json("[]"));
    }
    @Test void rejectsUnknownFieldsInvalidInputAndMissingSecurity() throws Exception {
        mvc.perform(get("/api/conta/enderecos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/conta/cep/70000001")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/conta/enderecos").with(user(email)).contentType("application/json").content(mapper.writeValueAsString(payload()))).andExpect(status().isForbidden());
        mvc.perform(post("/api/conta/enderecos").with(user(email)).with(csrf()).contentType("text/plain").content("x")).andExpect(status().isUnsupportedMediaType());
        for (String field : List.of("id","usuarioId","perfil","valor","versao")) {
            var data = payload(); data.put(field,1);
            mvc.perform(post("/api/conta/enderecos").with(user(email)).with(csrf()).contentType("application/json").content(mapper.writeValueAsString(data))).andExpect(status().isBadRequest());
        }
        var data = payload(); data.put("cep","abcdefghi");
        mvc.perform(post("/api/conta/enderecos").with(user(email)).with(csrf()).contentType("application/json").content(mapper.writeValueAsString(data))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/conta/enderecos/0").with(user(email))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/conta/enderecos").with(user(email))).andExpect(content().json("[]"));
    }
    @Test void pickupIsFreeAndDeliveryWithoutRangesIsNotConfigured() throws Exception {
        long id = create();
        mvc.perform(post("/api/conta/frete/cotacao").with(user(email)).with(csrf()).contentType("application/json").content("{\"modalidade\":\"RETIRADA\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.valor").value("0.00"));
        mvc.perform(post("/api/conta/frete/cotacao").with(user(email)).with(csrf()).contentType("application/json").content("{\"modalidade\":\"ENTREGA\",\"enderecoId\":"+id+"}"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("ENTREGA_NAO_CONFIGURADA"));
        mvc.perform(post("/api/conta/frete/cotacao").with(user(email)).with(csrf()).contentType("application/json").content("{\"modalidade\":\"RETIRADA\",\"valor\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test void quotesFictitiousInclusiveRangesInH2WithoutClaimingPostgresConstraints() throws Exception {
        jdbc.update("insert into faixa_frete(cep_inicio,cep_fim,valor,ativa) values ('70000001','70000009',12.34,true),('70000010','70000020',5.00,false)");
        try {
            for (String postalCode : List.of("70000001","70000009","70000000","70000010","70000021")) {
                var data = payload(); data.put("cep",postalCode);
                var result = mvc.perform(post("/api/conta/enderecos").with(user(email)).with(csrf()).contentType("application/json").content(mapper.writeValueAsString(data)))
                        .andExpect(status().isCreated()).andReturn();
                long id = mapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
                var quote = mvc.perform(post("/api/conta/frete/cotacao").with(user(email)).with(csrf()).contentType("application/json").content("{\"modalidade\":\"ENTREGA\",\"enderecoId\":"+id+"}"));
                if (postalCode.equals("70000001") || postalCode.equals("70000009"))
                    quote.andExpect(status().isOk()).andExpect(jsonPath("$.valor").value("12.34")).andExpect(jsonPath("$.versaoEndereco").value(0));
                else quote.andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("FORA_DA_AREA_DE_ENTREGA"));
            }
            var data = payload(); data.put("uf","GO");
            var result = mvc.perform(post("/api/conta/enderecos").with(user(email)).with(csrf()).contentType("application/json").content(mapper.writeValueAsString(data))).andReturn();
            long id = mapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
            mvc.perform(post("/api/conta/frete/cotacao").with(user(email)).with(csrf()).contentType("application/json").content("{\"modalidade\":\"ENTREGA\",\"enderecoId\":"+id+"}"))
                    .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("FORA_DA_AREA_DE_ENTREGA"));
        } finally { jdbc.update("delete from faixa_frete"); }
    }

    @Test void enforcesEveryTextLimitAndExactVersionNumbers() throws Exception {
        for (var field : Map.of("destinatario",120,"logradouro",160,"numero",20,"complemento",120,"bairro",100,"cidade",100,"uf",2).entrySet()) {
            var data = payload(); data.put(field.getKey(),"A".repeat(field.getValue()+1));
            mvc.perform(post("/api/conta/enderecos").with(user(email)).with(csrf()).contentType("application/json").content(mapper.writeValueAsString(data))).andExpect(status().isBadRequest());
        }
        long id = create();
        for (Object version : new Object[]{-1,0.5,"0",Long.MAX_VALUE}) {
            var data = payload(); data.put("versao",version);
            mvc.perform(put("/api/conta/enderecos/"+id).with(user(email)).with(csrf()).contentType("application/json").content(mapper.writeValueAsString(data)))
                    .andExpect(status().is(version.equals(Long.MAX_VALUE) ? 409 : 400));
        }
        mvc.perform(put("/api/conta/enderecos/"+id).with(user(email)).with(csrf()).contentType("application/json").content(mapper.writeValueAsString(payload()))).andExpect(status().isBadRequest());
        for (String method : List.of("PUT","DELETE")) {
            mvc.perform(request(org.springframework.http.HttpMethod.valueOf(method),"/api/conta/enderecos/"+id).with(user(email)))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/conta/frete/cotacao").with(user(email)).contentType("application/json").content("{\"modalidade\":\"RETIRADA\"}")).andExpect(status().isForbidden());
    }

    @Test void mapsSimulatedPostalResponsesToPrivateApiErrors() throws Exception {
        org.mockito.Mockito.when(postal.lookup("70000001")).thenReturn(new br.com.pit.cupcakes.endereco.dto.PostalCodeResponse("70000001","","","Brasília","DF"));
        mvc.perform(get("/api/conta/cep/70000001").with(user(email))).andExpect(status().isOk()).andExpect(jsonPath("$.logradouro").value(""))
                .andExpect(jsonPath("$.cidade").value("Brasília")).andExpect(header().string("Cache-Control",org.hamcrest.Matchers.containsString("no-store")));
        for (int status : List.of(404,502,504)) {
            String code = status == 404 ? "CEP_NAO_ENCONTRADO" : status == 502 ? "SERVICO_CEP_INDISPONIVEL" : "CONSULTA_CEP_EXPIRADA";
            org.mockito.Mockito.doThrow(new br.com.pit.cupcakes.endereco.service.AccountOperationException(status,code,"Mensagem pública")).when(postal).lookup("70000002");
            mvc.perform(get("/api/conta/cep/70000002").with(user(email))).andExpect(status().is(status)).andExpect(jsonPath("$.codigo").value(code));
        }
    }
}
