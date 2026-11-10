package br.com.pit.cupcakes.pedido.service;

import br.com.pit.cupcakes.compartilhado.exception.InvalidIdException;
import br.com.pit.cupcakes.endereco.service.AccountOperationException;
import br.com.pit.cupcakes.identidade.entity.User;
import br.com.pit.cupcakes.identidade.repository.UserRepository;
import br.com.pit.cupcakes.pedido.dto.AdminOrderDetailResponse;
import br.com.pit.cupcakes.pedido.dto.AdminOrderSummaryResponse;
import br.com.pit.cupcakes.pedido.dto.OrderDetailResponse;
import br.com.pit.cupcakes.pedido.dto.OrderSummaryResponse;
import br.com.pit.cupcakes.pedido.dto.UpdateOrderStatusRequest;
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
import jakarta.validation.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderAddressRepository orderAddressRepository;
    private final SimulatedPaymentRepository simulatedPaymentRepository;
    private final UserRepository userRepository;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        OrderAddressRepository orderAddressRepository,
                        SimulatedPaymentRepository simulatedPaymentRepository,
                        UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderAddressRepository = orderAddressRepository;
        this.simulatedPaymentRepository = simulatedPaymentRepository;
        this.userRepository = userRepository;
    }

    public List<OrderSummaryResponse> listCustomerOrders(String email) {
        User user = requireUser(email);
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDescIdDesc(user.getId());
        if (orders.isEmpty()) {
            return List.of();
        }
        Map<Long, String> paymentResults = paymentResultsByOrderId(orders);
        return orders.stream()
                .map(order -> new OrderSummaryResponse(
                        order.getId(),
                        order.getCreatedAt(),
                        order.getMode().name(),
                        order.getStatus().name(),
                        paymentResults.get(order.getId()),
                        money(order.getTotal())
                ))
                .toList();
    }

    public OrderDetailResponse getCustomerOrderDetail(Long id, String email) {
        validatePositiveId(id);
        User user = requireUser(email);
        Order order = orderRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new AccountOperationException(404, "PEDIDO_NAO_ENCONTRADO", "Pedido não encontrado."));
        return toOrderDetailResponse(order);
    }

    public List<AdminOrderSummaryResponse> listAdminOrders() {
        List<Order> orders = orderRepository.findAllWithUserOrderByCreatedAtDescIdDesc();
        if (orders.isEmpty()) {
            return List.of();
        }
        Map<Long, String> paymentResults = paymentResultsByOrderId(orders);
        return orders.stream()
                .map(order -> new AdminOrderSummaryResponse(
                        order.getId(),
                        order.getCreatedAt(),
                        order.getUser().getName(),
                        order.getUser().getNormalizedEmail(),
                        order.getMode().name(),
                        order.getStatus().name(),
                        paymentResults.get(order.getId()),
                        money(order.getTotal()),
                        order.getVersion()
                ))
                .toList();
    }

    public AdminOrderDetailResponse getAdminOrderDetail(Long id) {
        validatePositiveId(id);
        Order order = orderRepository.findWithUserById(id)
                .orElseThrow(() -> new AccountOperationException(404, "PEDIDO_NAO_ENCONTRADO", "Pedido não encontrado."));
        return toAdminOrderDetailResponse(order);
    }

    @Transactional
    public AdminOrderDetailResponse updateOrderStatus(Long id, UpdateOrderStatusRequest request) {
        validatePositiveId(id);
        if (request == null || request.version() == null || request.version() < 0 || request.status() == null) {
            throw new ValidationException("Informe o status de atendimento e a versão atual do pedido.");
        }
        OrderStatus targetStatus;
        try {
            targetStatus = OrderStatus.valueOf(request.status().strip());
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Status de atendimento inválido.");
        }

        Order order = orderRepository.findWithUserByIdForUpdate(id)
                .orElseThrow(() -> new AccountOperationException(404, "PEDIDO_NAO_ENCONTRADO", "Pedido não encontrado."));

        if (!order.getVersion().equals(request.version())) {
            throw new AccountOperationException(409, "PEDIDO_DESATUALIZADO",
                    "Pedido alterado em outra sessão. Atualize os dados antes de tentar novamente.");
        }

        SimulatedPayment payment = simulatedPaymentRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new IllegalStateException("Pagamento simulado ausente para o pedido."));

        // Repetir o estado atual e idempotente e nao incrementa a versao
        if (order.getStatus() == targetStatus) {
            return toAdminOrderDetailResponse(order, payment);
        }

        List<String> allowed = allowedNextStatuses(order, payment.getResult());
        if (!allowed.contains(targetStatus.name())) {
            throw new AccountOperationException(409, "TRANSICAO_STATUS_INVALIDA",
                    buildInvalidTransitionMessage(order, payment.getResult(), targetStatus));
        }

        order.setStatus(targetStatus);
        Order saved = orderRepository.saveAndFlush(order);
        return toAdminOrderDetailResponse(saved, payment);
    }

    public OrderDetailResponse toOrderDetailResponse(Order order) {
        SimulatedPayment payment = simulatedPaymentRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new IllegalStateException("Pagamento simulado ausente para o pedido."));
        List<OrderDetailResponse.Item> items = loadItems(order.getId());
        OrderDetailResponse.HistoricalAddress address = loadHistoricalAddress(order);
        return new OrderDetailResponse(
                order.getId(),
                order.getRevisionId(),
                order.getCreatedAt(),
                order.getMode().name(),
                order.getStatus().name(),
                payment.getResult().name(),
                items,
                address,
                money(order.getSubtotal()),
                money(order.getShippingFee()),
                money(order.getTotal()),
                order.getVersion()
        );
    }

    public AdminOrderDetailResponse toAdminOrderDetailResponse(Order order) {
        SimulatedPayment payment = simulatedPaymentRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new IllegalStateException("Pagamento simulado ausente para o pedido."));
        return toAdminOrderDetailResponse(order, payment);
    }

    private AdminOrderDetailResponse toAdminOrderDetailResponse(Order order, SimulatedPayment payment) {
        List<OrderDetailResponse.Item> items = loadItems(order.getId());
        OrderDetailResponse.HistoricalAddress address = loadHistoricalAddress(order);
        List<String> allowedNext = allowedNextStatuses(order, payment.getResult());
        return new AdminOrderDetailResponse(
                order.getId(),
                order.getRevisionId(),
                order.getCreatedAt(),
                order.getUser().getName(),
                order.getUser().getNormalizedEmail(),
                order.getMode().name(),
                order.getStatus().name(),
                payment.getResult().name(),
                items,
                address,
                money(order.getSubtotal()),
                money(order.getShippingFee()),
                money(order.getTotal()),
                order.getVersion(),
                allowedNext
        );
    }

    public static List<String> allowedNextStatuses(Order order, SimulatedPaymentResult paymentResult) {
        if (paymentResult == SimulatedPaymentResult.RECUSADO || order.getStatus() == OrderStatus.NAO_AUTORIZADO) {
            return List.of();
        }
        return switch (order.getStatus()) {
            case RECEBIDO -> List.of(OrderStatus.EM_PREPARACAO.name(), OrderStatus.CANCELADO.name());
            case EM_PREPARACAO -> order.getMode() == OrderMode.ENTREGA
                    ? List.of(OrderStatus.SAIU_PARA_ENTREGA.name(), OrderStatus.CANCELADO.name())
                    : List.of(OrderStatus.PRONTO_PARA_RETIRADA.name(), OrderStatus.CANCELADO.name());
            case PRONTO_PARA_RETIRADA -> List.of(OrderStatus.CONCLUIDO.name(), OrderStatus.CANCELADO.name());
            case SAIU_PARA_ENTREGA -> List.of(OrderStatus.CONCLUIDO.name());
            case CONCLUIDO, CANCELADO, NAO_AUTORIZADO -> List.of();
        };
    }

    private String buildInvalidTransitionMessage(Order order, SimulatedPaymentResult paymentResult, OrderStatus target) {
        if (paymentResult == SimulatedPaymentResult.RECUSADO || order.getStatus() == OrderStatus.NAO_AUTORIZADO) {
            return "Pedidos com pagamento simulado recusado (NAO_AUTORIZADO) não podem ser preparados ou alterados.";
        }
        if (order.getStatus() == OrderStatus.CONCLUIDO || order.getStatus() == OrderStatus.CANCELADO) {
            return "Pedidos concluídos ou cancelados estão em estado final e não permitem novas alterações.";
        }
        if (target == OrderStatus.CANCELADO && order.getStatus() == OrderStatus.SAIU_PARA_ENTREGA) {
            return "Não é permitido cancelar um pedido que já saiu para entrega.";
        }
        return "Transição de status inválida de " + order.getStatus().name()
                + " para " + target.name() + " na modalidade " + order.getMode().name() + ".";
    }

    private List<OrderDetailResponse.Item> loadItems(Long orderId) {
        List<OrderItem> items = orderItemRepository.findByOrderIdOrderByProductIdAsc(orderId);
        return items.stream()
                .map(item -> {
                    BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    return new OrderDetailResponse.Item(
                            item.getProduct().getId(),
                            item.getProductName(),
                            money(item.getUnitPrice()),
                            item.getQuantity(),
                            money(lineTotal)
                    );
                })
                .toList();
    }

    private OrderDetailResponse.HistoricalAddress loadHistoricalAddress(Order order) {
        if (order.getMode() != OrderMode.ENTREGA) {
            return null;
        }
        OrderAddress address = orderAddressRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new IllegalStateException("Endereço histórico ausente para pedido de entrega."));
        return new OrderDetailResponse.HistoricalAddress(
                address.getRecipient(),
                address.getPostalCode(),
                address.getStreet(),
                address.getNumber(),
                address.getComplement(),
                address.getNeighborhood(),
                address.getCity(),
                address.getState()
        );
    }

    private Map<Long, String> paymentResultsByOrderId(List<Order> orders) {
        List<Long> ids = orders.stream().map(Order::getId).toList();
        return simulatedPaymentRepository.findByOrderIdIn(ids).stream()
                .collect(Collectors.toMap(p -> p.getOrder().getId(), p -> p.getResult().name()));
    }

    private User requireUser(String email) {
        return userRepository.findByNormalizedEmail(email)
                .orElseThrow(() -> new AccountOperationException(401, "NAO_AUTENTICADO", "É necessário entrar na conta."));
    }

    private static void validatePositiveId(Long id) {
        if (id == null || id <= 0) {
            throw new InvalidIdException("O identificador informado deve ser um número inteiro positivo.");
        }
    }

    public static String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}

