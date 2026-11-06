package br.com.pit.cupcakes.endereco;

import br.com.pit.cupcakes.catalogo.PostgresTestDatabaseGuard;
import br.com.pit.cupcakes.endereco.dto.AddressRequest;
import br.com.pit.cupcakes.endereco.service.*;
import br.com.pit.cupcakes.frete.dto.ShippingRequest;
import br.com.pit.cupcakes.frete.service.ShippingService;
import br.com.pit.cupcakes.identidade.entity.User;
import br.com.pit.cupcakes.identidade.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.*;
import org.springframework.test.context.*;
import org.springframework.transaction.annotation.*;
import javax.sql.DataSource;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test-pg")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = PostgresTestDatabaseGuard.class)
@Tag("postgres")
@Transactional
class AddressShippingPostgresTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource source;
    @Autowired UserRepository users;
    @Autowired AddressService addresses;
    @Autowired ShippingService shipping;
    String email;
    @BeforeEach void setup() {
        assertThat(jdbc.queryForObject("select current_database()",String.class)).isEqualTo("cupcakes_test");
        email = "address-"+UUID.randomUUID()+"@example.com";
        users.saveAndFlush(new User("Teste de endereço",email,"hash-ficticio"));
    }
    AddressRequest request(String postalCode, String state, Long version) {
        return new AddressRequest("Pessoa fictícia",postalCode,"Rua fictícia","S/N",null,"Bairro fictício","Região administrativa",state,version);
    }
    @Test void quotesInclusiveBoundsWithExactDecimalsAndRefusesInactiveOutsideAndWrongState() {
        jdbc.update("delete from faixa_frete");
        jdbc.update("insert into faixa_frete(cep_inicio,cep_fim,valor,ativa) values ('70000001','70000009',12.34,true),('70000010','70000020',5.00,false)");
        for (String postalCode : new String[]{"70000001","70000009"}) {
            var address = addresses.create(request(postalCode,"DF",null),email);
            var quote = shipping.quote(new ShippingRequest("ENTREGA",address.id()),email);
            assertThat(quote.amount()).isEqualTo("12.34"); assertThat(quote.addressVersion()).isEqualTo(address.version());
        }
        for (String postalCode : new String[]{"70000000","70000010","70000021"}) {
            var address = addresses.create(request(postalCode,"DF",null),email);
            expectQuoteError(address.id(),"FORA_DA_AREA_DE_ENTREGA");
        }
        var wrongState = addresses.create(request("70000001","GO",null),email);
        expectQuoteError(wrongState.id(),"FORA_DA_AREA_DE_ENTREGA");
    }
    @Test void refusesUnconfiguredAndForeignAddressAndReturnsFreePickup() {
        jdbc.update("delete from faixa_frete");
        var address = addresses.create(request("70000001","DF",null),email);
        expectQuoteError(address.id(),"ENTREGA_NAO_CONFIGURADA");
        assertThat(shipping.quote(new ShippingRequest("RETIRADA",null),email).amount()).isEqualTo("0.00");
        String other = "other-"+email; users.saveAndFlush(new User("Outro",other,"hash-ficticio"));
        assertThatThrownBy(() -> shipping.quote(new ShippingRequest("ENTREGA",address.id()),other))
                .isInstanceOfSatisfying(AccountOperationException.class,exception -> assertThat(exception.getStatus()).isEqualTo(404));
    }
    @Test void persistsNormalizedAddressAndVersionAndDeletesOnlyCurrentVersion() {
        var created = addresses.create(request("01234-567","df",null),email);
        assertThat(jdbc.queryForObject("select cep from endereco where id = ?",String.class,created.id())).isEqualTo("01234567");
        var changed = addresses.update(created.id(),request("70000001","DF",created.version()),email);
        assertThat(changed.version()).isEqualTo(created.version()+1);
        assertThatThrownBy(() -> addresses.delete(created.id(),created.version(),email)).isInstanceOf(AccountOperationException.class);
        addresses.delete(created.id(),changed.version(),email);
        assertThat(jdbc.queryForObject("select count(*) from endereco where id = ?",Integer.class,created.id())).isZero();
    }
    @Test void databaseRejectsMalformedRangesOverlapsAndInvalidAddresses() {
        jdbc.update("delete from faixa_frete");
        jdbc.update("insert into faixa_frete(cep_inicio,cep_fim,valor,ativa) values ('70000001','70000009',1,true)");
        for (String values : new String[]{"('70000009','70000010',1,true)","('70000010','70000001',1,true)","('70000010','70000011',-1,true)","('abcdef00','70000011',1,false)"}) {
            rejectsSql("insert into faixa_frete(cep_inicio,cep_fim,valor,ativa) values "+values);
        }
        jdbc.update("insert into faixa_frete(cep_inicio,cep_fim,valor,ativa) values ('70000009','70000010',1,false)");
        rejectsSql("update faixa_frete set ativa = true where ativa = false");
        var address = addresses.create(request("70000001","DF",null),email);
        rejectsSql("update endereco set cep='123' where id="+address.id());
        rejectsSql("update endereco set destinatario=' ' where id="+address.id());
        rejectsSql("update endereco set usuario_id=-1 where id="+address.id());
        rejectsSql("update endereco set uf='XX' where id="+address.id());
    }
    @Test @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void exclusionConstraintSerializesConcurrentOverlaps() throws Exception {
        try (Connection first = source.getConnection(); Connection second = source.getConnection();
             var executor = Executors.newSingleThreadExecutor()) {
            first.setAutoCommit(false); second.setAutoCommit(false);
            long id;
            try (var insert = first.prepareStatement("insert into faixa_frete(cep_inicio,cep_fim,valor,ativa) values ('09999001','09999009',1,true) returning id")) {
                var result = insert.executeQuery(); result.next(); id = result.getLong(1);
            }
            try {
                var started = new CountDownLatch(1);
                var future = executor.submit(() -> {
                    try (var insert = second.createStatement()) {
                        insert.execute("set local statement_timeout = '4s'"); started.countDown();
                        insert.execute("insert into faixa_frete(cep_inicio,cep_fim,valor,ativa) values ('09999009','09999010',2,true)");
                        return "accepted";
                    } catch (SQLException exception) { return exception.getSQLState(); }
                });
                assertThat(started.await(2,TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(() -> future.get(200,TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
                first.commit();
                assertThat(future.get(3,TimeUnit.SECONDS)).isEqualTo("23P01");
            } finally {
                first.rollback(); second.rollback();
                jdbc.update("delete from faixa_frete where id = ?",id);
                jdbc.update("delete from usuario where email_normalizado = ?",email);
            }
        }
    }
    private void rejectsSql(String sql) {
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            var savepoint = connection.setSavepoint();
            try { assertThatThrownBy(() -> connection.createStatement().execute(sql)).isInstanceOf(SQLException.class); }
            finally { connection.rollback(savepoint); connection.releaseSavepoint(savepoint); }
            return null;
        });
    }
    private void expectQuoteError(Long id, String code) {
        assertThatThrownBy(() -> shipping.quote(new ShippingRequest("ENTREGA",id),email))
                .isInstanceOfSatisfying(AccountOperationException.class,exception -> assertThat(exception.getCode()).isEqualTo(code));
    }
}
