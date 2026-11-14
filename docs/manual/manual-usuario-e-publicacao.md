# Manual de Uso, Operação e Publicação — PIT Cupcakes

Guia consolidado para utilização da loja virtual por clientes e administradores, execução local, geração de artefato executável, política de frete acadêmica e preparação para publicação gratuita em **Render Free + Neon Free PostgreSQL**. Este documento não declara ambiente remoto já publicado.

## 1. Fluxo do Cliente

1. **Catálogo e Detalhes** (`/index.html` e `/produto.html?id=N`):
   - Navegue pelos cupcakes disponíveis e consulte preço, descrição e disponibilidade.
   - Adicione unidades ao carrinho diretamente pelo catálogo ou pela página de detalhes. Ao adicionar, uma faixa visual destacada (`.cart-feedback-success`) confirma `"Produto adicionado ao carrinho."` com o link direto `"Conferir itens"` para abrir o carrinho.
2. **Carrinho de Compras** (`/paginas/compra/carrinho.html`):
   - Ajuste quantidades (`1` a `99`) ou remova itens.
   - Caso um item fique indisponível ou seja alterado no catálogo, o servidor marca a linha como bloqueada ou atualiza o preço na revisão.
   - Clique em **Revisar e finalizar pedido**. Se ainda não estiver autenticado, você será direcionado ao login/cadastro preservando o carrinho na sessão.
3. **Endereços de Entrega** (`/paginas/conta/enderecos.html`):
   - Cadastre, revise, edite ou remova endereços próprios. O campo de CEP declara `inputmode="numeric"` e o preenchimento automático via ViaCEP é apenas um auxílio opcional, permitindo revisão e preenchimento manual.
4. **Checkout e Pagamento Acadêmico Simulado** (`/paginas/compra/checkout.html`):
   - Escolha a modalidade **Retirada na loja** (frete `R$ 0,00`) ou **Entrega em endereço cadastrado** (calculada pelas faixas ativas de Brasília–DF) e clique em **Gerar revisão do pedido**.
   - Confira a revisão gerada pelo servidor (`Subtotal dos itens`, `Frete` e `Total do pedido`), válida por **10 minutos**.
   - Na seção **3. Pagamento simulado (ambiente acadêmico)**, escolha explicitamente o resultado:
     - **Simular pagamento aprovado (APROVADO)**: grava o pedido com status `RECEBIDO`, limpa o carrinho após o commit e exibe caixa de confirmação aprovada.
     - **Simular pagamento recusado (RECUSADO)**: grava a tentativa recusada (`NAO_AUTORIZADO`) para histórico, preserva os itens no carrinho para nova tentativa e exibe alerta destacado de recusa.
5. **Meus Pedidos** (`/paginas/conta/pedidos.html` e `/paginas/conta/pedido.html?id=N`):
   - Consulte todos os pedidos realizados pela sua conta, incluindo fotografia histórica dos itens, endereço congelado da compra, resultado da simulação de pagamento (`APROVADO` ou `RECUSADO`) e evolução do status de atendimento.
   - O cancelamento de pedido não faz parte das ações do cliente (fora do escopo); pertence exclusivamente ao fluxo administrativo de atendimento nos estados permitidos.

## 2. Fluxo do Administrador (`ROLE_ADMIN`)

1. **Provisionamento Seguro do Administrador**:
   - Nenhuma conta administrativa ou credencial padrão é criada em migrações Flyway nem embutida na imagem Docker. Credenciais de teste ou segredos de provisionamento não entram neste manual público nem no Git.
   - Para provisionar o administrador na inicialização da aplicação, defina no ambiente do processo as três variáveis `ADMIN_EMAIL`, `ADMIN_NAME` e `ADMIN_PASSWORD` por entrada protegida antes de iniciar o Spring Boot:
     ```powershell
     $adminCred = Get-Credential -Message 'Credenciais do administrador (UserName = e-mail)'
     $env:ADMIN_EMAIL = $adminCred.UserName
     $env:ADMIN_NAME = 'Administrador PIT Cupcakes'
     $env:ADMIN_PASSWORD = $adminCred.GetNetworkCredential().Password
     ```
   - Se o e-mail já pertencer a um `CLIENTE`, o provisionamento falha explicitamente sem promover a conta. Se já pertencer a um `ADMIN`, a rotina é idempotente quando a senha confere e falha caso uma senha divergente seja informada, evitando sobrescrita silenciosa.
2. **Administração do Catálogo** (`/admin/produtos.html`):
   - Cadastre novos cupcakes informando nome (`1–150` caracteres), descrição (`1–2000` caracteres), preço (`0.01` a `99999999.99`), imagem da lista permitida de recursos estáticos SVG/WebP em `/assets/img/` (ou substituta padrão) e disponibilidade.
   - Edite produtos existentes com controle de concorrência otimista (`versao`); em caso de conflito (`409`), os dados digitados são preservados na tela.
3. **Atendimento de Pedidos** (`/admin/pedidos.html`):
   - Visualize todos os pedidos recebidos, dados do cliente (nome e e-mail, sem expor hashes), itens históricos, endereço de entrega e resultado da simulação de pagamento.
   - Atualize o status de atendimento conforme a modalidade:
     - **Entrega (`APROVADO`)**: `RECEBIDO` → `EM_PREPARACAO` → `SAIU_PARA_ENTREGA` → `CONCLUIDO` (cancelamento permitido em `RECEBIDO` ou `EM_PREPARACAO`).
     - **Retirada (`APROVADO`)**: `RECEBIDO` → `EM_PREPARACAO` → `PRONTO_PARA_RETIRADA` → `CONCLUIDO` (cancelamento permitido até `PRONTO_PARA_RETIRADA`).
     - Pedidos com pagamento `RECUSADO` permanecem em `NAO_AUTORIZADO` e não entram na fila de preparo.

## 3. Política de Frete da Demonstração Pública

Esta aplicação representa loja acadêmica, com pagamento simulado e sem entrega comercial real. A configuração de demonstração proposta e descrita no projeto ([DemoShippingRangeLoader](../../src/main/java/br/com/pit/cupcakes/frete/demo/DemoShippingRangeLoader.java)) é:

| Modalidade ou faixa | Valor |
| --- | --- |
| Retirada | R$ 0,00 |
| CEP 70000000 a 70999999, UF DF | R$ 8,00 |
| CEP 71000000 a 71999999, UF DF | R$ 12,00 |

Os limites são inclusivos; faixas ativas não podem se sobrepor (restrição GiST no PostgreSQL). Endereços fora das faixas ou da UF `DF` não permitem entrega e apresentam retirada como alternativa. Esses parâmetros não comprovam cobertura postal ou operacional de todo o Distrito Federal.

Para carregar de forma idempotente os 4 produtos de demonstração e as 2 faixas de frete acima quando o banco estiver vazio, basta ativar o perfil `demo` junto ao perfil principal (`SPRING_PROFILES_ACTIVE=prod,demo` ou `local,demo`), ou aplicar manualmente o script controlado [configurar-frete.sql](../banco-de-dados/configurar-frete.sql).

## 4. Artefato Executável, Perfil `prod` e Container Docker

1. **Geração do pacote executável (`.jar`)**:
   Configure `JAVA_HOME` apontando para o JDK 21 instalado na máquina (incluindo `JAVA_HOME\bin` no `PATH`) e execute:
   ```powershell
   .\mvnw.cmd clean package -DskipTests
   ```
   Conforme `<artifactId>cupcakes</artifactId>` e `<version>0.0.1-SNAPSHOT</version>` no [pom.xml](../../pom.xml), o artefato executável realmente produzido pelo Maven é:
   - `target/cupcakes-0.0.1-SNAPSHOT.jar`

   *Observação*: uma compilação com `-DskipTests` só é aceitável para empacotamento após o aceite da rodada de testes registrado ([verificacao-quinta-etapa.md](../testes/resultados/verificacao-quinta-etapa.md)); não constitui evidência de validação funcional.

2. **Perfil `prod` autossuficiente ([application-prod.yml](../../src/main/resources/application-prod.yml))**:
   - **Conexão por variáveis de ambiente**: `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`, sem valores padrão locais embutidos. Pool HikariCP enxuto (`maximum-pool-size: ${DB_POOL_MAX_SIZE:5}`, `minimum-idle: 1`) dimensionado para conexões gerenciadas e memória limitada.
   - **Migrações e validação de esquema**: `spring.flyway.enabled=true`, `baseline-on-migrate=true`, `clean-disabled=true`, `locations=classpath:db/migration` (`V1` a `V4`) e `spring.jpa.hibernate.ddl-auto=validate` com `open-in-view=false`.
   - **Proxy reverso HTTPS e cookies seguros**: `server.forward-headers-strategy=framework` (reconhece `X-Forwarded-Proto: https` enviado pelo balanceador do Render) e `server.servlet.session.cookie.secure=${SESSION_COOKIE_SECURE:true}`, além de `HttpOnly=true` e `SameSite=Lax` herdados de [application.yml](../../src/main/resources/application.yml).

3. **Imagem Docker multi-stage Java 21 ([Dockerfile](../../Dockerfile) e [.dockerignore](../../.dockerignore))**:
   - **Estágio de build**: `eclipse-temurin:21-jdk-alpine`, compila o projeto via `./mvnw -B clean package -DskipTests` gerando `target/cupcakes-0.0.1-SNAPSHOT.jar`.
   - **Estágio de runtime**: `eclipse-temurin:21-jre-alpine`, executa com usuário sem privilégios de root (`appuser`), respeita a porta fornecida pelo ambiente (`-Dserver.port=${PORT:-8080}`) e aplica flags de memória adequadas ao limite de `512 MB RAM` do plano gratuito (`JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+UseSerialGC -Djava.security.egd=file:/dev/./urandom"`).
   - **Sem segredos na imagem**: nenhuma senha, URL de banco ou conta administrativa padrão consta no `Dockerfile`; `.dockerignore` exclui `target/`, `.git/`, `.env*`, `*.local.*`, `operacional/` e `docs/`.

4. **Comportamento de Suspensão, Reinício e Persistência**:
   - **Suspensão por inatividade (*Cold Start*)**: no plano Render Free, o contêiner entra em suspensão após **15 minutos** sem requisições HTTP; no Neon Free, o compute suspende após **5 minutos** sem consultas SQL. O primeiro acesso após inatividade leva cerca de **30 a 60 segundos** para reativar o contêiner e o pool JDBC.
   - **Perda de sessão e carrinho em reinício**: como a `HttpSession` (incluindo o carrinho de compras temporário, a revisão de checkout de 10 minutos e o contador `LoginAttemptGuard`) reside na memória RAM da instância única, qualquer reinício ou suspensão do contêiner encerra as sessões ativas e esvazia carrinhos ainda não confirmados — exigindo novo login e nova adição de itens.
   - **Persistência integral no banco PostgreSQL**: todos os registros confirmados no banco (`produto`, `usuario`, `endereco`, `faixa_frete`, `pedido`, `item_pedido`, `endereco_pedido` e `pagamento_simulado`), bem como o histórico de migrações Flyway (`flyway_schema_history`), permanecem **persistidos de forma durável no PostgreSQL (Neon)** e não são afetados por suspensão ou reinício do contêiner no Render.

## 5. Hospedagem Gratuita — Comparativo Oficial e Passo a Passo Render + Neon

### 5.1 Comparativo das Opções Gratuitas Atuais (Termos Oficiais)

| Componente / Provedor | Custo e Cartão | Capacidade e Persistência | Suspensão por Inatividade (*Cold Start*) | Expiração | Adequação ao PIT Cupcakes |
| --- | --- | --- | --- | --- | --- |
| **Aplicação Java**: **Render Free Web Service** (`render.com/docs/free`) | `$0/mês`; até 750h/mês (suficiente para 1 instância contínua). HTTPS automático incluso. | `512 MB RAM`, `0.1 CPU` compartilhada. Disco local efêmero (adequado pois os dados persistentes ficam no PostgreSQL). | **Sim**: hiberna após **15 minutos** sem tráfego HTTP. A primeira requisição após repouso leva **30 a 60 segundos** para subir a JVM/Spring Boot. | Sem expiração de 30 dias para o Web Service. | **Recomendado** (instância única com Docker e HTTPS nativo). |
| **Banco PostgreSQL (Opção A)**: **Neon Free Plan** (`neon.tech/pricing`) | `$0/mês`; **não exige cartão de crédito**; sem cobrança automática de excedente. | PostgreSQL gerenciado com **1 GB** de armazenamento persistente por projeto e **100 CU-hours/mês** (~400h ativas a 0.25 CU). Suporta extensão/GiST nativo e exige TLS (`sslmode=require`). | **Scale-to-zero** após **5 minutos** sem queries SQL; reativa automaticamente em **< 1 segundo** na primeira conexão JDBC. | **Não expira** após 30 dias. | **Recomendado** (persistência contínua durante todo o período de avaliação). |
| **Banco PostgreSQL (Opção B)**: **Supabase Free Plan** (`supabase.com/pricing`) | `$0/mês`; não exige cartão de crédito. | PostgreSQL dedicado com **500 MB** de armazenamento por projeto (até 2 projetos ativos). Exige TLS. | **Pausa** o projeto após **7 dias** sem consultas SQL; exige clique manual (*Resume*) no painel se ficar 1 semana sem uso. | Não expira em 30 dias (dados retidos por 1 ano se pausado). | **Viável** (requer acesso semanal para evitar pausa). |
| **Banco PostgreSQL (Opção C)**: **Render Free Postgres** (`render.com/docs/free`) | `$0/mês`. | **1 GB** de armazenamento, `256 MB RAM`, até 100 conexões. | Não hiberna a cada 5 minutos enquanto válido. | **Expira em 30 dias** após a criação (14 dias de carência antes da exclusão total). | **Com ressalva** (usar apenas se criado a menos de 30 dias do fim da correção acadêmica). |

### 5.2 Instruções Exatas de Publicação (Neon Free + Render Free)

Execute os passos abaixo após enviar (`git push`) o repositório revisado para o GitHub:

#### Passo 1 — Criar o banco PostgreSQL no Neon (`neon.tech`)
1. Acesse o painel do **Neon** e crie um novo projeto no plano **Free** (PostgreSQL 16/17, região próxima à do Render, ex.: `AWS us-east-1` ou `us-east-2`), com nome do banco `cupcakes`.
2. No painel **Connection Details**, selecione o formato **Java / JDBC** (conexão direta ou pooled com `sslmode=require`) e anote separadamente (sem salvar em arquivos do repositório):
   - **URL JDBC** (formato: `jdbc:postgresql://<host-neon>/<dbname>?sslmode=require`)
   - **Usuário** do banco
   - **Senha** do banco

#### Passo 2 — Criar o Web Service no Render (`render.com`)
1. No painel do **Render**, clique em **New +** → **Web Service** e conecte o repositório GitHub do projeto `pit-cupcakes`.
2. Configure o serviço:
   - **Language / Runtime**: `Docker` (utiliza automaticamente o `Dockerfile` na raiz do repositório)
   - **Branch**: `master` (ou a branch principal publicada)
   - **Region**: mesma região escolhida para o banco no Neon (ex.: `Virginia (US East)` ou `Ohio (US East)`)
   - **Instance Type**: `Free` (`512 MB RAM`, `0.1 CPU`, **1 instância**)
3. Na seção **Environment Variables**, cadastre as variáveis abaixo (substituindo os valores secretos diretamente no painel do Render):

| Nome da Variável | Obrigatória? | Valor / Formato no Painel do Render (sem segredos no código) |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | Sim | `prod,demo` *(aplica migrações `V1`–`V4` e carrega de forma idempotente os 4 cupcakes e as 2 faixas de frete acadêmicas se o banco estiver vazio; pode ser alterado para `prod` após a carga inicial)* |
| `DB_URL` | Sim | `jdbc:postgresql://<HOST_DO_NEON>/cupcakes?sslmode=require` |
| `DB_USERNAME` | Sim | `<USUARIO_DO_BANCO_NEON>` |
| `DB_PASSWORD` | Sim (Secret) | `<SENHA_DO_BANCO_NEON>` |
| `ADMIN_EMAIL` | Opcional* | `<EMAIL_DO_ADMINISTRADOR>` |
| `ADMIN_NAME` | Opcional* | `Administrador PIT Cupcakes` |
| `ADMIN_PASSWORD` | Opcional* (Secret) | `<SENHA_FORTE_DO_ADMINISTRADOR_8_A_64_CARACTERES>` |

*\*Nota sobre o administrador*: informe simultaneamente `ADMIN_EMAIL`, `ADMIN_NAME` e `ADMIN_PASSWORD` (ou omita as três). Na primeira inicialização, `AdminProvisioningRunner` cria a conta `ADMIN` de forma idempotente sem sobrescrever contas existentes.

4. Conclua a criação clicando em **Deploy Web Service** e aguarde o término do build Docker e a mensagem `Started CupcakesApplication` nos logs do Render.

#### Passo 3 — Smoke Test Remoto Pós-Deploy
Com a URL HTTPS pública (`https://<servico>.onrender.com`) ativa, valide no navegador:
1. Abertura do catálogo (`/index.html`) com os 4 cupcakes iniciais e detalhes (`/produto.html?id=1`).
2. Adição ao carrinho, cadastro de conta de cliente (`/paginas/conta/cadastro.html`) e login (`/paginas/conta/login.html`), verificando o cookie `JSESSIONID` com atributo `Secure` ativo em HTTPS.
3. Cadastro de endereço em Brasília–DF (`70000-000` ou `71000-000`), cotação de frete (`R$ 8,00` / `R$ 12,00`) e geração da revisão no checkout (`/paginas/compra/checkout.html`).
4. Simulação de pagamento `RECUSADO` (verificando preservação do carrinho) seguida de nova revisão e pagamento `APROVADO`, consulta em **Meus pedidos** e atendimento na área `/admin`.

## 6. Pendências Externas que Requerem Intervenção Humana

1. **Contas externas e publicação**: realizar o `git push` para o repositório GitHub público, criar o projeto no **Neon Free** e o serviço no **Render Free** inserindo os valores secretos das variáveis listadas na seção 5.2, e executar o *smoke test* remoto na URL HTTPS gerada.
2. **Validação em smartphone físico (`AV02`)**: tocar no campo CEP (`/paginas/conta/enderecos.html`) em um aparelho celular real (Android/iOS) para confirmar visualmente a abertura do teclado numérico nativo.
3. **Documento acadêmico final (DOC) e vídeo**: registrar a URL do GitHub, a URL HTTPS do Render e o link do vídeo demonstrativo no documento de entrega até **17/11/2026 às 23h59 BRT**.

## 7. Participantes

Os cinco relatos de avaliação já foram fornecidos pelo responsável, transcritos no DOC e consolidados tecnicamente em [avaliacao-participantes-e-laudo.md](../entrega/avaliacao-participantes-e-laudo.md) (mantendo o anexo nominal apenas localmente fora do repositório público). Não são exigidas novas notas ou questionários que o modelo da disciplina não solicita; campos não coletados não são calculados ou inventados.
