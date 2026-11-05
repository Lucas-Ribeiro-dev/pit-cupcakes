package br.com.pit.cupcakes.identidade;

import br.com.pit.cupcakes.identidade.dto.RegistrationRequest;
import br.com.pit.cupcakes.identidade.entity.User;
import br.com.pit.cupcakes.identidade.repository.UserRepository;
import br.com.pit.cupcakes.identidade.service.*;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.sql.SQLException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class RegistrationServiceTest {
    private static final ValidatorFactory VALIDATORS = Validation.buildDefaultValidatorFactory();
    private final UserRepository repository = mock(UserRepository.class);
    private final PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    private final RegistrationService service = new RegistrationService(repository, encoder, VALIDATORS.getValidator());

    @AfterAll
    static void closeValidator() { VALIDATORS.close(); }

    @Test
    void shouldNormalizeCreateOnlyCustomerAndHashLiteralPassword() {
        String password = " senha teste ";
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assertThat(user.getName()).isEqualTo("Cliente");
            assertThat(user.getNormalizedEmail()).isEqualTo("cliente@example.com");
            assertThat(user.getRole().name()).isEqualTo("CLIENTE");
            assertThat(user.getPasswordHash()).startsWith("{bcrypt}").isNotEqualTo(password);
            assertThat(encoder.matches(password, user.getPasswordHash())).isTrue();
            assertThat(encoder.matches(password.strip(), user.getPasswordHash())).isFalse();
            assertThat(user.getCreatedAt()).isNotNull();
            return user;
        });
        var response = service.register(new RegistrationRequest(" Cliente ", " CLIENTE@Example.COM ", password));
        assertThat(response.email()).isEqualTo("cliente@example.com");
        assertThat(response.role()).isEqualTo("CLIENTE");
    }

    @Test
    void shouldValidateUnicodeCodePointsAndUtf8BytesWithoutTruncation() {
        for (String password : new String[]{"12345678", "a".repeat(64), "é".repeat(36), "😀".repeat(18), "       a"}) {
            assertThatCode(() -> IdentityValidation.validatePassword(password)).doesNotThrowAnyException();
        }
        for (String password : new String[]{"", "1234567", "a".repeat(65), "é".repeat(37), "😀".repeat(19), "😀".repeat(7)}) {
            assertThatThrownBy(() -> IdentityValidation.validatePassword(password)).isInstanceOf(ValidationException.class);
        }
        assertThatThrownBy(() -> IdentityValidation.validatePassword(null)).isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldRejectInvalidFieldsBeforeSaving() {
        for (RegistrationRequest request : new RegistrationRequest[]{
                new RegistrationRequest(null, "c@example.com", "12345678"),
                new RegistrationRequest(" a ", "c@example.com", "12345678"),
                new RegistrationRequest("a".repeat(121), "c@example.com", "12345678"),
                new RegistrationRequest("Cliente", "invalido", "12345678"),
                new RegistrationRequest("Cliente", null, "12345678"),
                new RegistrationRequest("Cliente", "a".repeat(250) + "@example.com", "12345678"),
                new RegistrationRequest("Cliente", "c@example.com", "curta")}) {
            assertThatThrownBy(() -> service.register(request)).isInstanceOf(ValidationException.class);
        }
        verifyNoInteractions(repository);
    }

    @Test
    void shouldTranslateDuplicatePrecheckAndConcurrentUniqueViolation() {
        var request = new RegistrationRequest("Cliente", " C@EXAMPLE.COM ", "12345678");
        when(repository.existsByNormalizedEmail("c@example.com")).thenReturn(true);
        assertThatThrownBy(() -> service.register(request)).isInstanceOf(EmailAlreadyRegisteredException.class);
        verify(repository, never()).saveAndFlush(any());
        when(repository.existsByNormalizedEmail("c@example.com")).thenReturn(false);
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("Falha",
                new org.hibernate.exception.ConstraintViolationException("E-mail duplicado",
                        new SQLException("Violação de unicidade", "23505"), "uk_usuario_email_normalizado")));
        assertThatThrownBy(() -> service.register(request)).isInstanceOf(EmailAlreadyRegisteredException.class);
    }

    @Test
    void shouldNotTranslateUnrelatedPersistenceErrorsAsDuplicateEmail() {
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("Outra restrição"));
        assertThatThrownBy(() -> service.register(new RegistrationRequest("Cliente", "c@example.com", "12345678")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
