package br.com.pit.cupcakes.catalogo.service;

import br.com.pit.cupcakes.catalogo.dto.AdminProductResponse;
import br.com.pit.cupcakes.catalogo.entity.Product;
import br.com.pit.cupcakes.catalogo.repository.ProductRepository;
import br.com.pit.cupcakes.compartilhado.exception.InvalidIdException;
import br.com.pit.cupcakes.compartilhado.exception.ProductNotFoundException;
import br.com.pit.cupcakes.endereco.service.AccountOperationException;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.ValidationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class AdminProductService {

    public static final Set<String> ALLOWED_IMAGE_PATHS = Set.of(
            "/assets/img/cupcake-baunilha.svg",
            "/assets/img/cupcake-chocolate.svg",
            "/assets/img/cupcake-caramelo.svg",
            "/assets/img/cupcake-placeholder.svg"
    );

    private static final Set<String> CREATE_FIELDS = Set.of("nome", "descricao", "preco", "imagemCaminho", "disponivel");
    private static final Set<String> UPDATE_FIELDS = Set.of("nome", "descricao", "preco", "imagemCaminho", "disponivel", "versao");
    private static final BigDecimal MIN_PRICE = new BigDecimal("0.01");
    private static final BigDecimal MAX_PRICE = new BigDecimal("99999999.99");

    private final ProductRepository productRepository;

    public AdminProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<AdminProductResponse> findAll() {
        return productRepository.findAllByOrderByIdAsc().stream()
                .map(AdminProductResponse::fromEntity)
                .toList();
    }

    public AdminProductResponse findById(Long id) {
        validatePositiveId(id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Produto não encontrado para o identificador informado."));
        return AdminProductResponse.fromEntity(product);
    }

    @Transactional
    public AdminProductResponse create(JsonNode body) {
        ValidatedProductInput input = parseAndValidate(body, false);
        Product product = new Product(
                input.name(),
                input.description(),
                input.price(),
                input.imagePath(),
                input.available()
        );
        return AdminProductResponse.fromEntity(productRepository.saveAndFlush(product));
    }

    @Transactional
    public AdminProductResponse update(Long id, JsonNode body) {
        validatePositiveId(id);
        ValidatedProductInput input = parseAndValidate(body, true);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Produto não encontrado para o identificador informado."));

        if (!product.getVersion().equals(input.version())) {
            throw new AccountOperationException(409, "PRODUTO_DESATUALIZADO",
                    "Produto alterado em outra sessão. Revise os dados atuais antes de tentar novamente.");
        }

        product.setName(input.name());
        product.setDescription(input.description());
        product.setPrice(input.price());
        product.setImagePath(input.imagePath());
        product.setAvailable(input.available());

        try {
            return AdminProductResponse.fromEntity(productRepository.saveAndFlush(product));
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new AccountOperationException(409, "PRODUTO_DESATUALIZADO",
                    "Produto alterado em outra sessão. Revise os dados atuais antes de tentar novamente.");
        }
    }

    private static ValidatedProductInput parseAndValidate(JsonNode body, boolean update) {
        if (body == null || !body.isObject()) {
            throw new ValidationException("Corpo da requisição inválido.");
        }
        Set<String> allowedFields = update ? UPDATE_FIELDS : CREATE_FIELDS;
        Iterator<String> fieldNames = body.fieldNames();
        while (fieldNames.hasNext()) {
            String field = fieldNames.next();
            if (!allowedFields.contains(field)) {
                throw new ValidationException("Propriedade não permitida: " + field);
            }
        }

        String name = parseRequiredText(body.get("nome"), 150);
        String description = parseRequiredText(body.get("descricao"), 2000);
        BigDecimal price = parsePrice(body.get("preco"));
        String imagePath = parseImagePath(body.get("imagemCaminho"));

        JsonNode availableNode = body.get("disponivel");
        if (availableNode == null || !availableNode.isBoolean()) {
            throw new ValidationException("Informe a disponibilidade do produto (true ou false).");
        }
        boolean available = availableNode.booleanValue();

        Long version = null;
        if (update) {
            JsonNode versionNode = body.get("versao");
            if (versionNode == null || !versionNode.isIntegralNumber() || !versionNode.canConvertToLong() || versionNode.longValue() < 0) {
                throw new ValidationException("Informe a versão atual não negativa do produto.");
            }
            version = versionNode.longValue();
        }

        return new ValidatedProductInput(name, description, price, imagePath, available, version);
    }

    private static String parseRequiredText(JsonNode node, int maxLength) {
        if (node == null || !node.isTextual()) {
            throw new ValidationException("Campo textual obrigatório ausente.");
        }
        String stripped = node.asText().strip();
        int codePoints = stripped.codePointCount(0, stripped.length());
        if (stripped.isEmpty() || codePoints > maxLength || stripped.length() > maxLength) {
            throw new ValidationException("Texto fora dos limites permitidos.");
        }
        return stripped;
    }

    private static BigDecimal parsePrice(JsonNode node) {
        if (node == null || (!node.isTextual() && !node.isNumber())) {
            throw new ValidationException("Informe um preço válido.");
        }
        String text = node.isTextual() ? node.asText().strip() : node.decimalValue().toPlainString();
        if (!text.matches("^[0-9]{1,8}(\\.[0-9]{1,2})?$")) {
            throw new ValidationException("O preço deve estar entre 0.01 e 99999999.99 com no máximo duas casas decimais.");
        }
        BigDecimal value = new BigDecimal(text);
        if (value.compareTo(MIN_PRICE) < 0 || value.compareTo(MAX_PRICE) > 0) {
            throw new ValidationException("O preço deve estar entre 0.01 e 99999999.99.");
        }
        return value.setScale(2, RoundingMode.UNNECESSARY);
    }

    private static String parseImagePath(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isTextual()) {
            throw new ValidationException("O caminho da imagem deve ser textual.");
        }
        String stripped = node.asText().strip();
        if (stripped.isEmpty()) {
            return null;
        }
        if (!ALLOWED_IMAGE_PATHS.contains(stripped)) {
            throw new ValidationException("A imagem deve pertencer aos recursos estáticos locais permitidos.");
        }
        return stripped;
    }

    private static void validatePositiveId(Long id) {
        if (id == null || id <= 0) {
            throw new InvalidIdException("O identificador informado deve ser um número inteiro positivo.");
        }
    }

    private record ValidatedProductInput(
            String name,
            String description,
            BigDecimal price,
            String imagePath,
            boolean available,
            Long version
    ) {}
}

