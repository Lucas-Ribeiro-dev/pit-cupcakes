package br.com.pit.cupcakes.endereco;

import br.com.pit.cupcakes.endereco.dto.AddressRequest;
import br.com.pit.cupcakes.endereco.service.AddressValidation;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class AddressValidationTest {
    @Test void normalizesWithoutLosingAccentsOrLeadingZero() {
        var result = AddressValidation.normalize(new AddressRequest(" José ","01234-567"," Rua São João "," S/N "," "," Centro "," Brasília "," df ",null),false);
        assertThat(result.recipient()).isEqualTo("José"); assertThat(result.postalCode()).isEqualTo("01234567");
        assertThat(result.state()).isEqualTo("DF"); assertThat(result.number()).isEqualTo("S/N"); assertThat(result.complement()).isNull();
    }
    @Test void rejectsInvalidPostalCodes() {
        for (String value : new String[]{"1234567","123456789","1234-5678","abcdefgh"," 70000000","70000-000 ","７０００００００"})
            assertThatThrownBy(() -> AddressValidation.postalCode(value)).isInstanceOf(ValidationException.class);
    }
    @Test void enforcesRequiredTextLimitsStateAndVersion() {
        var valid = new AddressRequest("A","70000000","Rua","S/N",null,"Bairro","Cidade","DF",0L);
        assertThatCode(() -> AddressValidation.normalize(valid,true)).doesNotThrowAnyException();
        assertThatThrownBy(() -> AddressValidation.normalize(valid,false)).isInstanceOf(ValidationException.class);
        for (String value : new String[]{" ","A".repeat(121)}) assertThatThrownBy(() -> AddressValidation.normalize(
                new AddressRequest(value,"70000000","Rua","1",null,"Bairro","Cidade","DF",null),false)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> AddressValidation.normalize(new AddressRequest("A","70000000","Rua","1",null,"Bairro","Cidade","XX",null),false)).isInstanceOf(ValidationException.class);
    }
}
