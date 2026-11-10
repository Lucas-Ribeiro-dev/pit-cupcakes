# Manual de Uso, Operação e Publicação — PIT Cupcakes

Guia consolidado para utilização da loja virtual por clientes e administradores, execução local, geração de artefato executável e requisitos para ambiente de publicação.

## 1. Fluxo do Cliente

1. **Catálogo e Detalhes** (`/index.html` e `/produto.html?id=N`):
   - Navegue pelos cupcakes disponíveis e consulte preço, descrição e disponibilidade.
   - Adicione unidades ao carrinho diretamente pelo catálogo ou pela página de detalhes.
2. **Carrinho de Compras** (`/paginas/compra/carrinho.html`):
   - Ajuste quantidades (`1` a `99`) ou remova itens.
   - Caso um item fique indisponível ou seja alterado no catálogo, o servidor marca a linha como bloqueada ou atualiza o preço na revisão.
   - Clique em **Avançar para checkout**. Se ainda não estiver autenticado, você será direcionado ao login/cadastro preservando o carrinho na sessão.
3. **Endereços de Entrega** (`/paginas/conta/enderecos.html`):
   - Cadastre, revise, edite ou remova endereços próprios. O preenchimento automático por CEP (ViaCEP) é opcional e permite revisão manual.
4. **Checkout e Pagamento Acadêmico Simulado** (`/paginas/compra/checkout.html`):
   - Escolha a modalidade **Retirada na loja** (frete `R$ 0,00`) ou **Entrega no endereço selecionado** (calculada pelas faixas ativas de Brasília–DF).
   - Confira a revisão gerada pelo servidor (`Subtotal dos itens`, `Frete` e `Total do pedido`).
   - Escolha explicitamente o resultado do **Pagamento Acadêmico Simulado**:
     - **Simular pagamento APROVADO**: grava o pedido com status `RECEBIDO`, limpa o carrinho e redireciona ao comprovante do pedido.
     - **Simular pagamento RECUSADO**: grava a tentativa recusada (`NAO_AUTORIZADO`) para histórico, mantendo os itens no carrinho para nova tentativa.
5. **Meus Pedidos** (`/paginas/conta/pedidos.html` e `/paginas/conta/pedido.html?id=N`):
   - Consulte todos os pedidos realizados pela sua conta, incluindo fotografia histórica dos itens, endereço congelado da compra, código da simulação de pagamento e evolução do status de atendimento.

## 2. Fluxo do Administrador (`ROLE_ADMIN`)

1. **Provisionamento Seguro do Administrador**:
   - Nenhuma conta real de administrador ou senha padrão é gravada em migrações Flyway.
   - Para provisionar o administrador na inicialização da aplicação, defina no terminal as três variáveis `ADMIN_EMAIL`, `ADMIN_NAME` e `ADMIN_PASSWORD` antes de iniciar o Spring Boot:
     ```powershell
     $adminCred = Get-Credential -Message 'Credenciais do administrador (UserName = e-mail)'
     $env:ADMIN_EMAIL = $adminCred.UserName
     $env:ADMIN_NAME = 'Administrador PIT Cupcakes'
     $env:ADMIN_PASSWORD = $adminCred.GetNetworkCredential().Password
     ```
   - Se o e-mail já pertencer a um `CLIENTE`, o provisionamento falha explicitamente sem promover a conta. Se já pertencer a um `ADMIN`, a rotina é idempotente quando a senha confere e falha caso uma senha divergente seja informada, evitando sobrescrita silenciosa.
2. **Administração do Catálogo** (`/admin/produtos.html`):
   - Cadastre novos cupcakes informando nome (`1–150` caracteres), descrição (`1–2000` caracteres), preço (`0.01` a `99999999.99`), imagem estática permitida (`/assets/img/*.svg` ou substituta padrão) e disponibilidade.
   - Edite produtos existentes com controle de concorrência otimista (`versao`); em caso de conflito (`409`), os dados digitados são preservados na tela.
3. **Atendimento de Pedidos** (`/admin/pedidos.html`):
   - Visualize todos os pedidos recebidos, dados do cliente (nome e e-mail, sem expor hashes), itens históricos, endereço de entrega e resultado da simulação de pagamento.
   - Atualize o status de atendimento conforme a modalidade:
     - **Entrega (`APROVADO`)**: `RECEBIDO` → `EM_PREPARACAO` → `SAIU_PARA_ENTREGA` → `CONCLUIDO` (cancelamento permitido em `RECEBIDO` ou `EM_PREPARACAO`).
     - **Retirada (`APROVADO`)**: `RECEBIDO` → `EM_PREPARACAO` → `PRONTO_PARA_RETIRADA` → `CONCLUIDO` (cancelamento permitido até `PRONTO_PARA_RETIRADA`).
     - Pedidos com pagamento `RECUSADO` permanecem em `NAO_AUTORIZADO` e não entram na fila de preparo.

## 3. Artefato Executável e Ambiente de Publicação

1. **Geração do pacote executável (`.jar`)**:
   ```powershell
   $env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
   $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
   .\mvnw.cmd clean package -DskipTests
   ```
   O artefato executável é gerado em `target/pit-cupcakes-0.0.1-SNAPSHOT.jar`.
2. **Requisitos para Publicação em Produção (`prod`)**:
   - **Banco de Dados PostgreSQL**: aplicar migrações Flyway `V1` a `V4` automaticamente no startup (`spring.flyway.enabled=true`, `ddl-auto=validate`), usando credenciais exclusivas via variáveis de ambiente (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`).
   - **HTTPS e Cookies Seguros**: ativar o perfil `prod` (`SPRING_PROFILES_ACTIVE=prod`) atrás de proxy reverso TLS/HTTPS, garantindo `server.servlet.session.cookie.secure=true`, `HttpOnly=true` e `SameSite=Lax`.
   - **Proteção contra Abuso de Login**: o filtro `LoginAttemptGuard` está ativo por padrão (`LOGIN_MAX_ATTEMPTS=10`, `LOGIN_WINDOW_SECONDS=300`) e bloqueia rajadas de tentativas inválidas com `HTTP 429`.
   - **Topologia de Instância Única**: como o carrinho e a revisão de checkout utilizam `HttpSession` em memória, a publicação deve operar em instância única (ou com afinidade estrita de sessão).

