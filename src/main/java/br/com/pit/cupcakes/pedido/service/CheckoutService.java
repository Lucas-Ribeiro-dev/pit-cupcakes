package br.com.pit.cupcakes.pedido.service;

import br.com.pit.cupcakes.carrinho.service.CartService;
import br.com.pit.cupcakes.catalogo.entity.Product;
import br.com.pit.cupcakes.catalogo.repository.ProductRepository;
import br.com.pit.cupcakes.endereco.entity.Address;
import br.com.pit.cupcakes.endereco.repository.AddressRepository;
import br.com.pit.cupcakes.endereco.service.AccountOperationException;
import br.com.pit.cupcakes.frete.entity.ShippingRange;
import br.com.pit.cupcakes.frete.repository.ShippingRangeRepository;
import br.com.pit.cupcakes.identidade.entity.User;
import br.com.pit.cupcakes.identidade.repository.UserRepository;
import br.com.pit.cupcakes.pedido.dto.CheckoutRevisionRequest;
import br.com.pit.cupcakes.pedido.dto.CheckoutRevisionResponse;
import br.com.pit.cupcakes.pedido.dto.ConfirmOrderRequest;
import br.com.pit.cupcakes.pedido.dto.OrderDetailResponse;
import br.com.pit.cupcakes.pedido.entity.Order;
import br.com.pit.cupcakes.pedido.entity.OrderAddress;
import br.com.pit.cupcakes.pedido.entity.OrderItem;
import br.com.pit.cupcakes.pedido.entity.OrderMode;
import br.com.pit.cupcakes.pedido.entity.OrderStatus;
import br.com.pit.cupcakes.pedido.entity.SimulatedPayment;
import br.com.pit.cupcakes.pedido.entity.SimulatedPaymentResult;
import br.com.pit.cupcakes.pedido.repository.OrderAddressRepository;
import br.com.pit.cupcakes.pedido.repository.OrderItemRepository;
import br.com.pit.cupcakes.pedido.repository.OrderRepository;
import br.com.pit.cupcakes.pedido.repository.SimulatedPaymentRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.ValidationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CheckoutService {

    private static final String REVISIONS_SESSION_ATTRIBUTE = "checkoutRevisions";
    private static final String CONSUMED_REVISIONS_SESSION_ATTRIBUTE = "consumedCheckoutRevisions";
    private static final Duration REVISION_TTL = Duration.ofMinutes(10);

    private final CartService cartService;
    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;
    private final ShippingRangeRepository shippingRangeRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderAddressRepository orderAddressRepository;
    private final SimulatedPaymentRepository simulatedPaymentRepository;
    private final OrderService orderService;
    private final TransactionTemplate readTx;
    private final TransactionTemplate writeTx;

    public CheckoutService(CartService cartService,
                           ProductRepository productRepository,
                           AddressRepository addressRepository,
                           ShippingRangeRepository shippingRangeRepository,
                           UserRepository userRepository,
                           OrderRepository orderRepository,
                           OrderItemRepository orderItemRepository,
                           OrderAddressRepository orderAddressRepository,
                           SimulatedPaymentRepository simulatedPaymentRepository,
                           OrderService orderService,
                           PlatformTransactionManager transactionManager) {
        this.cartService = cartService;
        this.productRepository = productRepository;
        this.addressRepository = addressRepository;
        this.shippingRangeRepository = shippingRangeRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderAddressRepository = orderAddressRepository;
        this.simulatedPaymentRepository = simulatedPaymentRepository;
        this.orderService = orderService;

        this.readTx = new TransactionTemplate(transactionManager);
        this.readTx.setReadOnly(true);
        this.readTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        this.writeTx = new TransactionTemplate(transactionManager);
        this.writeTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public record ConfirmationOutcome(OrderDetailResponse order, boolean created) {}

    public CheckoutRevisionResponse createRevision(CheckoutRevisionRequest request, HttpSession session, String email) {
        OrderMode mode = parseAndValidateRevisionRequest(request);
        return readTx.execute(status -> {
            User user = requireUser(email);
            Map<Long, Integer> cartSnapshot = cartService.snapshot(session);
            if (cartSnapshot.isEmpty()) {
                throw new AccountOperationException(409, "CARRINHO_VAZIO",
                        "Seu carrinho está vazio. Adicione produtos antes de revisar o pedido.");
            }

            Map<Long, Product> productsById = productRepository.findAllById(cartSnapshot.keySet()).stream()
                    .collect(Collectors.toMap(Product::getId, Function.identity()));

            List<CheckoutRevision.RevisionItem> revisionItems = new ArrayList<>();
            BigDecimal subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            for (Map.Entry<Long, Integer> entry : cartSnapshot.entrySet()) {
                Long productId = entry.getKey();
                int quantity = entry.getValue();
                Product product = productsById.get(productId);
                if (product == null || !Boolean.TRUE.equals(product.getAvailable())) {
                    throw new AccountOperationException(409, "ITEM_BLOQUEADO",
                            "Existem itens indisponíveis ou removidos no carrinho. Remova-os antes de revisar o pedido.");
                }
                BigDecimal unitPrice = product.getPrice().setScale(2, RoundingMode.HALF_UP);
                BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
                subtotal = subtotal.add(lineTotal);
                revisionItems.add(new CheckoutRevision.RevisionItem(
                        product.getId(),
                        product.getName(),
                        unitPrice,
                        quantity,
                        lineTotal,
                        product.getVersion()
                ));
            }

            CheckoutRevision.RevisionAddress revisionAddress = null;
            BigDecimal shippingFee = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            if (mode == OrderMode.ENTREGA) {
                Address address = addressRepository.findByIdAndOwnerId(request.addressId(), user.getId())
                        .orElseThrow(() -> new AccountOperationException(404, "ENDERECO_NAO_ENCONTRADO", "Endereço não encontrado."));
                if (!address.getVersion().equals(request.addressVersion())) {
                    throw new AccountOperationException(409, "ENDERECO_DESATUALIZADO",
                            "Endereço alterado em outra sessão. Revise os dados atuais antes de tentar novamente.");
                }
                shippingFee = calculateDeliveryFee(address);
                revisionAddress = new CheckoutRevision.RevisionAddress(
                        address.getId(),
                        address.getRecipient(),
                        address.getPostalCode(),
                        address.getStreet(),
                        address.getNumber(),
                        address.getComplement(),
                        address.getNeighborhood(),
                        address.getCity(),
                        address.getState(),
                        address.getVersion()
                );
            }

            BigDecimal total = subtotal.add(shippingFee).setScale(2, RoundingMode.HALF_UP);
            Instant now = Instant.now();
            Instant expiresAt = now.plus(REVISION_TTL);
            String revisionId = UUID.randomUUID().toString();

            CheckoutRevision revision = new CheckoutRevision(
                    revisionId,
                    user.getId(),
                    now,
                    expiresAt,
                    mode,
                    List.copyOf(revisionItems),
                    revisionAddress,
                    subtotal,
                    shippingFee,
                    total
            );

            synchronized (session) {
                Map<String, CheckoutRevision> revisions = revisions(session);
                if (revisions.size() >= 30) {
                    String oldestKey = revisions.keySet().iterator().next();
                    revisions.remove(oldestKey);
                }
                revisions.put(revisionId, revision);
            }

            return toRevisionResponse(revision);
        });
    }

    public ConfirmationOutcome confirmOrder(ConfirmOrderRequest request, HttpSession session, String email) {
        if (request == null || request.revisionId() == null || request.revisionId().isBlank()
                || request.revisionId().strip().length() > 64 || request.simulatedResult() == null) {
            throw new ValidationException("Informe o identificador da revisão e o resultado simulado do pagamento.");
        }
        String revisionId = request.revisionId().strip();
        SimulatedPaymentResult simulatedResult;
        try {
            simulatedResult = SimulatedPaymentResult.valueOf(request.simulatedResult().strip());
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Resultado de pagamento simulado inválido.");
        }

        Long userId = readTx.execute(status -> requireUser(email).getId());

        // Verifica primeiro se o pedido dessa chave ja foi persistido (recuperacao de resposta perdida / repeticao idempotente)
        Optional<OrderDetailResponse> existingBeforeLock = findExistingOrderForRevision(userId, revisionId, simulatedResult);
        if (existingBeforeLock.isPresent()) {
            return new ConfirmationOutcome(existingBeforeLock.get(), false);
        }

        CheckoutRevision revision;
        synchronized (session) {
            revision = revisions(session).get(revisionId);
            if (revision == null || !revision.userId().equals(userId)) {
                throw new AccountOperationException(404, "REVISAO_NAO_ENCONTRADA",
                        "Revisão de pedido não encontrada ou inválida. Faça uma nova revisão.");
            }
            if (Instant.now().isAfter(revision.expiresAt())) {
                throw new AccountOperationException(409, "REVISAO_EXPIRADA",
                        "A revisão do pedido expirou. Faça uma nova revisão para continuar.");
            }
            if (consumedRevisions(session).contains(revisionId)) {
                Optional<OrderDetailResponse> existing = findExistingOrderForRevision(userId, revisionId, simulatedResult);
                if (existing.isPresent()) {
                    return new ConfirmationOutcome(existing.get(), false);
                }
                throw new AccountOperationException(409, "REVISAO_DESATUALIZADA",
                        "Esta revisão já foi utilizada. Faça uma nova revisão do pedido.");
            }
            verifyCartMatchesRevision(cartService.snapshot(session), revision);
        }

        OrderDetailResponse createdOrder;
        try {
            createdOrder = writeTx.execute(status -> persistValidatedOrder(userId, revision, simulatedResult, session));
        } catch (DataIntegrityViolationException ex) {
            // Tratamento de concorrencia em nova transacao limpa apos rollback da violacao de unicidade
            Optional<OrderDetailResponse> concurrentWinner = findExistingOrderForRevision(userId, revisionId, simulatedResult);
            if (concurrentWinner.isPresent()) {
                return new ConfirmationOutcome(concurrentWinner.get(), false);
            }
            throw ex;
        }

        if (createdOrder == null) {
            Optional<OrderDetailResponse> concurrentWinner = findExistingOrderForRevision(userId, revisionId, simulatedResult);
            if (concurrentWinner.isPresent()) {
                return new ConfirmationOutcome(concurrentWinner.get(), false);
            }
            throw new AccountOperationException(409, "REVISAO_DESATUALIZADA",
                    "A revisão não pôde ser confirmada. Faça uma nova revisão do pedido.");
        }

        // Limpa o carrinho somente apos o commit bem-sucedido e apenas quando o pagamento simulado e APROVADO
        synchronized (session) {
            consumedRevisions(session).add(revisionId);
            if (simulatedResult == SimulatedPaymentResult.APROVADO) {
                cartService.clear(session);
            }
        }

        return new ConfirmationOutcome(createdOrder, true);
    }

    public void expireRevisionForTest(HttpSession session, String revisionId) {
        synchronized (session) {
            Map<String, CheckoutRevision> map = revisions(session);
            CheckoutRevision existing = map.get(revisionId);
            if (existing != null) {
                Instant past = Instant.now().minus(Duration.ofMinutes(15));
                map.put(revisionId, new CheckoutRevision(
                        existing.revisionId(),
                        existing.userId(),
                        past.minus(REVISION_TTL),
                        past,
                        existing.mode(),
                        existing.items(),
                        existing.address(),
                        existing.subtotal(),
                        existing.shippingFee(),
                        existing.total()
                ));
            }
        }
    }

    private OrderDetailResponse persistValidatedOrder(Long userId,
                                                      CheckoutRevision revision,
                                                      SimulatedPaymentResult simulatedResult,
                                                      HttpSession session) {
        Optional<Order> alreadyPersisted = orderRepository.findByUserIdAndRevisionId(userId, revision.revisionId());
        if (alreadyPersisted.isPresent()) {
            return null;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AccountOperationException(401, "NAO_AUTENTICADO", "É necessário entrar na conta."));

        // Bloqueia produtos em ordem crescente de ID para evitar deadlocks e garantir consistencia ate o commit
        List<Long> productIds = revision.items().stream()
                .map(CheckoutRevision.RevisionItem::productId)
                .sorted()
                .toList();
        List<Product> lockedProducts = productRepository.findAllByIdInOrderByIdAscForUpdate(productIds);
        Map<Long, Product> lockedById = lockedProducts.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        if (lockedById.size() != revision.items().size()) {
            throw new AccountOperationException(409, "REVISAO_DESATUALIZADA",
                    "Um ou mais produtos do pedido não foram encontrados. Faça uma nova revisão.");
        }

        BigDecimal recalculatedSubtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (CheckoutRevision.RevisionItem item : revision.items()) {
            Product product = lockedById.get(item.productId());
            if (product == null || !Boolean.TRUE.equals(product.getAvailable())) {
                throw new AccountOperationException(409, "REVISAO_DESATUALIZADA",
                        "Um produto do carrinho ficou indisponível antes da confirmação. Remova-o e faça uma nova revisão.");
            }
            BigDecimal currentPrice = product.getPrice().setScale(2, RoundingMode.HALF_UP);
            if (currentPrice.compareTo(item.unitPrice()) != 0) {
                throw new AccountOperationException(409, "REVISAO_DESATUALIZADA",
                        "O preço de um produto foi alterado antes da confirmação. Faça uma nova revisão do pedido.");
            }
            if (!product.getVersion().equals(item.productVersion()) || !product.getName().equals(item.name())) {
                throw new AccountOperationException(409, "REVISAO_DESATUALIZADA",
                        "Os dados de um produto foram atualizados antes da confirmação. Faça uma nova revisão do pedido.");
            }
            BigDecimal lineTotal = currentPrice.multiply(BigDecimal.valueOf(item.quantity())).setScale(2, RoundingMode.HALF_UP);
            recalculatedSubtotal = recalculatedSubtotal.add(lineTotal);
        }

        if (recalculatedSubtotal.compareTo(revision.subtotal()) != 0) {
            throw new AccountOperationException(409, "REVISAO_DESATUALIZADA",
                    "O subtotal do pedido mudou. Faça uma nova revisão.");
        }

        BigDecimal recalculatedShipping = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (revision.mode() == OrderMode.ENTREGA) {
            CheckoutRevision.RevisionAddress revAddr = revision.address();
            if (revAddr == null) {
                throw new AccountOperationException(409, "REVISAO_DESATUALIZADA",
                        "Endereço de entrega ausente na revisão. Faça uma nova revisão.");
            }
            Address address = addressRepository.findByIdAndOwnerIdForUpdate(revAddr.id(), userId)
                    .orElseThrow(() -> new AccountOperationException(409, "REVISAO_DESATUALIZADA",
                            "O endereço selecionado foi removido antes da confirmação. Escolha outro endereço ou retirada em uma nova revisão."));
            if (!address.getVersion().equals(revAddr.version())
                    || !address.getPostalCode().equals(revAddr.postalCode())
                    || !address.getState().equals(revAddr.state())) {
                throw new AccountOperationException(409, "REVISAO_DESATUALIZADA",
                        "O endereço selecionado foi alterado antes da confirmação. Faça uma nova revisão do pedido.");
            }
            if (!"DF".equals(address.getState())) {
                throw new AccountOperationException(409, "REVISAO_DESATUALIZADA",
                        "O endereço selecionado não pertence à área atendida (DF). Faça uma nova revisão ou opte por retirada gratuita.");
            }
            List<ShippingRange> matchingRanges = shippingRangeRepository.matchingForUpdate(address.getPostalCode());
            if (matchingRanges.isEmpty()) {
                throw new AccountOperationException(409, "REVISAO_DESATUALIZADA",
                        "A faixa de frete para o CEP informado não está mais ativa. Faça uma nova revisão ou opte por retirada gratuita.");
            }
            if (matchingRanges.size() != 1) {
                throw new IllegalStateException("Configuração de frete ambígua.");
            }
            recalculatedShipping = matchingRanges.getFirst().getAmount().setScale(2, RoundingMode.HALF_UP);
            if (recalculatedShipping.compareTo(revision.shippingFee()) != 0) {
                throw new AccountOperationException(409, "REVISAO_DESATUALIZADA",
                        "O valor do frete foi atualizado antes da confirmação. Faça uma nova revisão do pedido.");
            }
        }

        synchronized (session) {
            if (consumedRevisions(session).contains(revision.revisionId())) {
                return null;
            }
            if (Instant.now().isAfter(revision.expiresAt())) {
                throw new AccountOperationException(409, "REVISAO_EXPIRADA",
                        "A revisão do pedido expirou. Faça uma nova revisão para continuar.");
            }
            verifyCartMatchesRevision(cartService.snapshot(session), revision);
        }

        OrderStatus initialStatus = simulatedResult == SimulatedPaymentResult.APROVADO
                ? OrderStatus.RECEBIDO
                : OrderStatus.NAO_AUTORIZADO;

        BigDecimal total = recalculatedSubtotal.add(recalculatedShipping).setScale(2, RoundingMode.HALF_UP);
        Order order = orderRepository.save(new Order(
                user,
                revision.revisionId(),
                revision.mode(),
                initialStatus,
                recalculatedSubtotal,
                recalculatedShipping,
                total
        ));

        List<OrderItem> itemsToSave = new ArrayList<>();
        for (CheckoutRevision.RevisionItem item : revision.items()) {
            Product product = lockedById.get(item.productId());
            itemsToSave.add(new OrderItem(order, product, item.name(), item.unitPrice(), item.quantity()));
        }
        orderItemRepository.saveAll(itemsToSave);

        if (revision.mode() == OrderMode.ENTREGA) {
            CheckoutRevision.RevisionAddress addr = revision.address();
            orderAddressRepository.save(new OrderAddress(
                    order,
                    addr.recipient(),
                    addr.postalCode(),
                    addr.street(),
                    addr.number(),
                    addr.complement(),
                    addr.neighborhood(),
                    addr.city(),
                    addr.state()
            ));
        }

        simulatedPaymentRepository.save(new SimulatedPayment(order, simulatedResult));
        orderRepository.flush();

        return orderService.toOrderDetailResponse(order);
    }

    private Optional<OrderDetailResponse> findExistingOrderForRevision(Long userId,
                                                                       String revisionId,
                                                                       SimulatedPaymentResult requestedResult) {
        return readTx.execute(status -> {
            Optional<Order> existingOpt = orderRepository.findByUserIdAndRevisionId(userId, revisionId);
            if (existingOpt.isEmpty()) {
                return Optional.empty();
            }
            Order existing = existingOpt.get();
            SimulatedPayment payment = simulatedPaymentRepository.findByOrderId(existing.getId())
                    .orElseThrow(() -> new IllegalStateException("Pagamento simulado ausente para o pedido."));
            if (payment.getResult() != requestedResult) {
                throw new AccountOperationException(409, "CONFIRMACAO_DIVERGENTE",
                        "Esta revisão já foi confirmada com outro resultado de pagamento simulado.");
            }
            return Optional.of(orderService.toOrderDetailResponse(existing));
        });
    }

    private static void verifyCartMatchesRevision(Map<Long, Integer> cartSnapshot, CheckoutRevision revision) {
        if (cartSnapshot.isEmpty() || cartSnapshot.size() != revision.items().size()) {
            throw new AccountOperationException(409, "REVISAO_DESATUALIZADA",
                    "O carrinho foi alterado após a revisão. Faça uma nova revisão do pedido.");
        }
        for (CheckoutRevision.RevisionItem item : revision.items()) {
            Integer currentQuantity = cartSnapshot.get(item.productId());
            if (currentQuantity == null || currentQuantity != item.quantity()) {
                throw new AccountOperationException(409, "REVISAO_DESATUALIZADA",
                        "As quantidades do carrinho mudaram após a revisão. Faça uma nova revisão do pedido.");
            }
        }
    }

    private BigDecimal calculateDeliveryFee(Address address) {
        if (!"DF".equals(address.getState())) {
            throw new AccountOperationException(422, "FORA_DA_AREA_DE_ENTREGA",
                    "Endereço fora da área de entrega. Retirada gratuita disponível.");
        }
        if (!shippingRangeRepository.existsByActiveTrue()) {
            throw new AccountOperationException(422, "ENTREGA_NAO_CONFIGURADA",
                    "Entrega ainda não configurada. Retirada gratuita disponível.");
        }
        List<ShippingRange> matching = shippingRangeRepository.matching(address.getPostalCode());
        if (matching.isEmpty()) {
            throw new AccountOperationException(422, "FORA_DA_AREA_DE_ENTREGA",
                    "Endereço fora da área de entrega. Retirada gratuita disponível.");
        }
        if (matching.size() != 1) {
            throw new IllegalStateException("Configuração de frete ambígua.");
        }
        return matching.getFirst().getAmount().setScale(2, RoundingMode.HALF_UP);
    }

    private static OrderMode parseAndValidateRevisionRequest(CheckoutRevisionRequest request) {
        if (request == null || request.mode() == null) {
            throw new ValidationException("Informe a modalidade do pedido.");
        }
        OrderMode mode;
        try {
            mode = OrderMode.valueOf(request.mode().strip());
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Modalidade inválida. Use ENTREGA ou RETIRADA.");
        }
        if (mode == OrderMode.RETIRADA) {
            if (request.addressId() != null || request.addressVersion() != null) {
                throw new ValidationException("Retirada não aceita endereço de entrega.");
            }
        } else {
            if (request.addressId() == null || request.addressId() <= 0
                    || request.addressVersion() == null || request.addressVersion() < 0) {
                throw new ValidationException("Entrega exige enderecoId positivo e versaoEndereco não negativa.");
            }
        }
        return mode;
    }

    private CheckoutRevisionResponse toRevisionResponse(CheckoutRevision revision) {
        List<CheckoutRevisionResponse.Item> items = revision.items().stream()
                .map(i -> new CheckoutRevisionResponse.Item(
                        i.productId(),
                        i.name(),
                        OrderService.money(i.unitPrice()),
                        i.quantity(),
                        OrderService.money(i.subtotal())
                ))
                .toList();

        CheckoutRevisionResponse.AddressSnapshot address = null;
        if (revision.mode() == OrderMode.ENTREGA && revision.address() != null) {
            CheckoutRevision.RevisionAddress a = revision.address();
            address = new CheckoutRevisionResponse.AddressSnapshot(
                    a.id(),
                    a.recipient(),
                    a.postalCode(),
                    a.street(),
                    a.number(),
                    a.complement(),
                    a.neighborhood(),
                    a.city(),
                    a.state(),
                    a.version()
            );
        }

        return new CheckoutRevisionResponse(
                revision.revisionId(),
                revision.expiresAt(),
                revision.mode().name(),
                items,
                address,
                OrderService.money(revision.subtotal()),
                OrderService.money(revision.shippingFee()),
                OrderService.money(revision.total())
        );
    }

    private User requireUser(String email) {
        return userRepository.findByNormalizedEmail(email)
                .orElseThrow(() -> new AccountOperationException(401, "NAO_AUTENTICADO", "É necessário entrar na conta."));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, CheckoutRevision> revisions(HttpSession session) {
        Object stored = session.getAttribute(REVISIONS_SESSION_ATTRIBUTE);
        if (stored instanceof Map<?, ?>) {
            return (Map<String, CheckoutRevision>) stored;
        }
        Map<String, CheckoutRevision> created = new LinkedHashMap<>();
        session.setAttribute(REVISIONS_SESSION_ATTRIBUTE, created);
        return created;
    }

    @SuppressWarnings("unchecked")
    private static Set<String> consumedRevisions(HttpSession session) {
        Object stored = session.getAttribute(CONSUMED_REVISIONS_SESSION_ATTRIBUTE);
        if (stored instanceof Set<?>) {
            return (Set<String>) stored;
        }
        Set<String> created = new HashSet<>();
        session.setAttribute(CONSUMED_REVISIONS_SESSION_ATTRIBUTE, created);
        return created;
    }
}

