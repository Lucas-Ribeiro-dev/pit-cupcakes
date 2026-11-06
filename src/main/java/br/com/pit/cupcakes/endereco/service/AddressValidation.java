package br.com.pit.cupcakes.endereco.service;

import br.com.pit.cupcakes.endereco.dto.AddressRequest;
import jakarta.validation.ValidationException;
import java.util.Locale;
import java.util.Set;

public final class AddressValidation {
    private static final Set<String> STATES = Set.of("AC","AL","AP","AM","BA","CE","DF","ES","GO","MA","MT","MS","MG","PA","PB","PR","PE","PI","RJ","RN","RS","RO","RR","SC","SP","SE","TO");
    private AddressValidation() {}
    public static String postalCode(String value) {
        if (value == null || !value.matches("[0-9]{5}-?[0-9]{3}")) throw new ValidationException();
        return value.replace("-", "");
    }
    private static String text(String value, int limit, boolean optional) {
        String result = value == null ? "" : value.strip();
        if ((!optional && result.isBlank()) || result.codePointCount(0, result.length()) > limit) throw new ValidationException();
        return optional && result.isEmpty() ? null : result;
    }
    public static AddressRequest normalize(AddressRequest request, boolean update) {
        if (request == null || (update ? request.version() == null || request.version() < 0 : request.version() != null)) throw new ValidationException();
        String state = text(request.state(), 2, false).toUpperCase(Locale.ROOT);
        if (!STATES.contains(state)) throw new ValidationException();
        return new AddressRequest(text(request.recipient(),120,false), postalCode(request.postalCode()),
                text(request.street(),160,false), text(request.number(),20,false), text(request.complement(),120,true),
                text(request.neighborhood(),100,false), text(request.city(),100,false), state, request.version());
    }
    public static void positiveId(Long id) {
        if (id == null || id <= 0) throw new ValidationException();
    }
    public static void version(Long version) {
        if (version == null || version < 0) throw new ValidationException();
    }
}
