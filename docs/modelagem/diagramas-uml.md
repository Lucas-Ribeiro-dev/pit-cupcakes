# Diagramas UML — PIT Cupcakes

Modelagem consolidada e enxuta do sistema conforme o código-fonte implementado nas migrações V1–V4 e módulos Spring Boot.

## 1. Diagrama de Casos de Uso

```mermaid
flowchart LR
    V["Visitante"]
    C["Cliente Autenticado"]
    A["Administrador (ADMIN)"]

    subgraph Loja["PIT Cupcakes"]
        UC1["Consultar catálogo e detalhes (HU01, HU02)"]
        UC2["Gerenciar carrinho em sessão (HU08)"]
        UC3["Cadastrar conta e autenticar (HU03, HU04, HU05)"]
        UC4["Gerenciar endereços e cotar frete (HU06, HU07)"]
        UC5["Revisar checkout e simular pagamento (HU09, HU10)"]
        UC6["Consultar próprios pedidos (HU11)"]
        UC7["Cadastrar e editar produtos (HU12)"]
        UC8["Atender pedidos e atualizar status (HU13)"]
    end

    V --> UC1
    V --> UC2
    V --> UC3

    C --> UC1
    C --> UC2
    C --> UC3
    C --> UC4
    C --> UC5
    C --> UC6

    A --> UC3
    A --> UC7
    A --> UC8
```

## 2. Diagrama de Classes de Domínio e Persistência

```mermaid
classDiagram
    class User {
        +Long id
        +String name
        +String normalizedEmail
        +String passwordHash
        +Role role
        +Instant createdAt
    }

    class Product {
        +Long id
        +String name
        +String description
        +BigDecimal price
        +String imagePath
        +boolean available
        +Long version
    }

    class Address {
        +Long id
        +Long ownerId
        +String recipient
        +String postalCode
        +String street
        +String number
        +String complement
        +String neighborhood
        +String city
        +String state
        +Long version
    }

    class ShippingRange {
        +Long id
        +String startPostalCode
        +String endPostalCode
        +BigDecimal amount
        +boolean active
    }

    class Order {
        +Long id
        +Long userId
        +String revisionId
        +OrderMode mode
        +BigDecimal itemsSubtotal
        +BigDecimal shippingAmount
        +BigDecimal totalAmount
        +SimulatedPaymentResult paymentStatus
        +OrderStatus attendanceStatus
        +Instant createdAt
        +Instant updatedAt
        +Long version
    }

    class OrderItem {
        +Long id
        +Long productId
        +String productName
        +BigDecimal unitPrice
        +int quantity
        +BigDecimal subtotal
    }

    class OrderAddress {
        +Long id
        +String recipient
        +String postalCode
        +String street
        +String number
        +String complement
        +String neighborhood
        +String city
        +String state
    }

    class SimulatedPayment {
        +Long id
        +SimulatedPaymentResult result
        +String referenceCode
        +String message
        +Instant processedAt
    }

    User "1" --> "0..*" Address : possui
    User "1" --> "0..*" Order : realiza
    Order "1" *-- "1..*" OrderItem : fotografa itens
    Order "1" *-- "0..1" OrderAddress : fotografa endereço
    Order "1" *-- "1" SimulatedPayment : registra simulação
    Product "1" <-- "0..*" OrderItem : referencia produtoId
```

## 3. Diagrama de Sequência — Checkout, Confirmação Transacional e Atendimento

```mermaid
sequenceDiagram
    actor Cliente
    participant UI as Checkout / Pedidos JS
    participant API as CheckoutController / OrderController
    participant Svc as CheckoutService / OrderService
    participant Cart as CartService (HttpSession)
    participant DB as PostgreSQL (V1..V4)
    actor Admin as Administrador

    Cliente->>UI: Escolhe ENTREGA ou RETIRADA
    UI->>API: POST /api/conta/checkout/revisao
    API->>Svc: review(session, userId, request)
    Svc->>Cart: inspect(session)
    Svc->>DB: Valida produtos, endereço e faixa_frete
    Svc->>Cart: Salva CheckoutRevision (revisaoId) na sessão
    Svc-->>UI: 200 OK (revisaoId, itens, subtotal, frete, total)

    Cliente->>UI: Seleciona APROVADO ou RECUSADO e confirma
    UI->>API: POST /api/conta/pedidos (revisaoId, resultadoSimulado)
    API->>Svc: confirmOrder(session, userId, request)
    Svc->>DB: Consulta pedido existente por (usuario_id, revisao_id)
    alt Já confirmado anteriormente com mesmo resultado
        DB-->>Svc: Pedido existente
        Svc-->>UI: 200 OK (Idempotente, sem duplicar pedido)
    else Nova confirmação
        Svc->>DB: SELECT ... FOR UPDATE (produto, endereco, faixa_frete)
        Svc->>Svc: Revalida preços, disponibilidade, versão do endereço e frete
        Svc->>DB: INSERT pedido, item_pedido, endereco_pedido, pagamento_simulado
        DB-->>Svc: Commit transacional concluído
        opt Resultado APROVADO
            Svc->>Cart: clear(session) após commit
        end
        Svc-->>UI: 201 Created (OrderDetailResponse)
    end

    Admin->>API: PUT /api/admin/pedidos/{id}/status (statusAtendimento, versao)
    API->>Svc: updateStatusAsAdmin(orderId, request)
    Svc->>DB: Valida transição por modalidade e @Version
    DB-->>Svc: Atualiza status_atendimento e versao
    Svc-->>Admin: 200 OK
    Cliente->>API: GET /api/conta/pedidos/{id}
    API-->>Cliente: 200 OK (Status atualizado e histórico preservado)
```

## 4. Diagrama de Atividade — Fluxo Completo de Compra e Pagamento Simulado

```mermaid
flowchart TD
    A["Início: Navegação no Catálogo"] --> B["Adicionar cupcakes disponíveis ao carrinho"]
    B --> C{"Possui itens bloqueados ou carrinho vazio?"}
    C -- "Sim" --> D["Remover ou ajustar itens no carrinho"]
    D --> C
    C -- "Não" --> E{"Usuário autenticado?"}
    E -- "Não" --> F["Entrar ou Criar Conta (preserva carrinho na sessão)"]
    F --> G["Acessar Checkout"]
    E -- "Sim" --> G
    G --> H{"Escolher modalidade"}
    H -- "RETIRADA" --> I["Gerar Revisão no Servidor (frete R$ 0,00)"]
    H -- "ENTREGA" --> J["Selecionar endereço salvo em Brasília-DF"]
    J --> K["Gerar Revisão no Servidor (cotação por faixa ativa)"]
    I --> L["Escolher resultado do Pagamento Simulado"]
    K --> L
    L --> M["Confirmar Pedido (POST /api/conta/pedidos)"]
    M --> N{"Dados do carrinho/preço/endereço mudaram?"}
    N -- "Sim" --> O["Retorna 409/422 sem gravar pedido; cliente revisa novamente"]
    O --> G
    N -- "Não" --> P{"Resultado Simulado?"}
    P -- "RECUSADO" --> Q["Grava pedido RECUSADO / NAO_AUTORIZADO e mantém carrinho"]
    P -- "APROVADO" --> R["Grava pedido APROVADO / RECEBIDO e limpa carrinho após commit"]
    Q --> S["Fim: Consulta em Meus Pedidos ou nova tentativa"]
    R --> T["Atendimento pelo ADMIN até CONCLUIDO ou CANCELADO"]
    T --> S
```

