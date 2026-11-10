# Manual de Uso, Operação e Publicação — PIT Cupcakes

Guia consolidado para utilização da loja virtual por clientes e administradores, execução local, geração de artefato executável, política de frete acadêmica e proposta de hospedagem gratuita. Este documento não declara ambiente remoto publicado.

## 1. Fluxo do Cliente

1. **Catálogo e Detalhes** (`/index.html` e `/produto.html?id=N`):
   - Navegue pelos cupcakes disponíveis e consulte preço, descrição e disponibilidade.
   - Adicione unidades ao carrinho diretamente pelo catálogo ou pela página de detalhes. Ao adicionar, uma faixa visual destacada confirma `"Produto adicionado ao carrinho."` com atalho direto (`Conferir itens`) para abrir o carrinho.
2. **Carrinho de Compras** (`/paginas/compra/carrinho.html`):
   - Ajuste quantidades (`1` a `99`) ou remova itens.
   - Caso um item fique indisponível ou seja alterado no catálogo, o servidor marca a linha como bloqueada ou atualiza o preço na revisão.
   - Clique em **Revisar e finalizar pedido**. Se ainda não estiver autenticado, você será direcionado ao login/cadastro preservando o carrinho na sessão.
3. **Endereços de Entrega** (`/paginas/conta/enderecos.html`):
   - Cadastre, revise, edite ou remova endereços próprios. O campo de CEP declara `inputmode="numeric"` e o preenchimento automático via ViaCEP é apenas um auxílio opcional, permitindo revisão e preenchimento manual.
4. **Checkout e Pagamento Acadêmico Simulado** (`/paginas/compra/checkout.html`):
   - Escolha a modalidade **Retirada na loja** (frete `R$ 0,00`) ou **Entrega em endereço cadastrado** (calculada pelas faixas ativas de Brasília–DF) e clique em **Gerar revisão do pedido**.
   - Confira a revisão gerada pelo servidor (`Subtotal dos itens`, `Frete` e `Total do pedido`).
   - Na seção **3. Pagamento simulado (ambiente acadêmico)**, escolha explicitamente o resultado:
     - **Simular pagamento aprovado (APROVADO)**: grava o pedido com status `RECEBIDO`, limpa o carrinho após o commit e exibe caixa de confirmação aprovada.
     - **Simular pagamento recusado (RECUSADO)**: grava a tentativa recusada (`NAO_AUTORIZADO`) para histórico, preserva os itens no carrinho para nova tentativa e exibe alerta destacado de recusa.
5. **Meus Pedidos** (`/paginas/conta/pedidos.html` e `/paginas/conta/pedido.html?id=N`):
   - Consulte todos os pedidos realizados pela sua conta, incluindo fotografia histórica dos itens, endereço congelado da compra, código da simulação de pagamento e evolução do status de atendimento.
   - O cancelamento de pedido não faz parte das ações do cliente (fora do escopo); pertence exclusivamente ao fluxo administrativo de atendimento nos estados permitidos.

## 2. Fluxo do Administrador (`ROLE_ADMIN`)

1. **Provisionamento Seguro do Administrador**:
   - Nenhuma conta administrativa ou credencial padrão é criada em migrações Flyway. Credenciais de teste ou segredos de provisionamento não entram neste manual público nem no Git.
   - Para provisionar o administrador na inicialização da aplicação, defina no ambiente do processo as três variáveis `ADMIN_EMAIL`, `ADMIN_NAME` e `ADMIN_PASSWORD` por entrada protegida antes de iniciar o Spring Boot:
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

## 3. Política de Frete da Demonstração Pública

Esta aplicação representa loja acadêmica, com pagamento simulado e sem entrega comercial real. A configuração de demonstração proposta e descrita no projeto ([DemoShippingRangeLoader](../../src/main/java/br/com/pit/cupcakes/frete/demo/DemoShippingRangeLoader.java)) é:

| Modalidade ou faixa | Valor |
| --- | --- |
| Retirada | R$ 0,00 |
| CEP 70000000 a 70999999, UF DF | R$ 8,00 |
| CEP 71000000 a 71999999, UF DF | R$ 12,00 |

Os limites são inclusivos; faixas ativas não podem se sobrepor (restrição GiST no PostgreSQL). Endereços fora das faixas ou da UF `DF` não permitem entrega e apresentam retirada como alternativa. Esses parâmetros não comprovam cobertura postal ou operacional de todo o Distrito Federal.

Não há definição comercial real a inventar para concluir um protótipo acadêmico. A pendência é confirmar e carregar conscientemente a configuração final da demonstração no ambiente publicado, validar limites e valores e documentar a limitação. Uma futura operação comercial exigiria revisão própria.

## 4. Artefato Executável e Configuração de Produção (`prod`)

1. **Geração do pacote executável (`.jar`)**:
   Configure `JAVA_HOME` apontando para o JDK 21 instalado na máquina (incluindo `JAVA_HOME\bin` no `PATH`) e execute:
   ```powershell
   .\mvnw.cmd clean package -DskipTests
   ```
   Conforme `<artifactId>cupcakes</artifactId>` e `<version>0.0.1-SNAPSHOT</version>` no [pom.xml](../../pom.xml), o artefato executável realmente produzido pelo Maven é:
   - `target/cupcakes-0.0.1-SNAPSHOT.jar`

   *Observação*: uma compilação com `-DskipTests` só é aceitável para empacotamento após o aceite da rodada de testes registrado ([verificacao-quinta-etapa.md](../testes/resultados/verificacao-quinta-etapa.md)); não constitui evidência de validação.
2. **Requisitos Técnicos de Publicação (`prod`)**:
   - **Instância Única**: publicar em instância única porque o carrinho (`HttpSession`) e o limitador de tentativas de login (`LoginAttemptGuard`) operam em memória.
   - **Segredos por Ambiente**: configurar `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `ADMIN_EMAIL`, `ADMIN_NAME` e `ADMIN_PASSWORD` exclusivamente por variáveis de ambiente no provedor, sem armazenar senhas no Git.
   - **Perfil `prod` e HTTPS**: ativar `SPRING_PROFILES_ACTIVE=prod` (ou `prod,demo` para carga inicial idempotente da vitrine e faixas acadêmicas se o banco estiver vazio) atrás de proxy HTTPS com `server.servlet.session.cookie.secure=true`, `HttpOnly=true` e `SameSite=Lax` ([application-prod.yml](../../src/main/resources/application-prod.yml)).

## 5. Hospedagem Gratuita — Pesquisa Oficial e Proposta Viável

Restrição confirmada pelo responsável: **hospedagem gratuita ($0/mês)**. O provedor de aplicação, o provedor PostgreSQL e o endereço HTTPS final ainda não estão publicados; abaixo está a análise comparativa das páginas oficiais atuais e a arquitetura recomendada para a publicação.

### 5.1 Comparativo das Opções Gratuitas Atuais (Termos Oficiais)

| Componente / Provedor | Custo e Cartão | Capacidade e Persistência | Suspensão por Inatividade (*Cold Start*) | Expiração | Adequação ao PIT Cupcakes |
| --- | --- | --- | --- | --- | --- |
| **Aplicação Java**: **Render Free Web Service** (`render.com/docs/free`) | `$0/mês`; até 750h/mês (suficiente para 1 instância contínua). HTTPS automático incluso. | `512 MB RAM`, `0.1 CPU` compartilhada. Disco local efêmero (adequado pois os dados persistentes ficam no PostgreSQL). | **Sim**: hiberna após **15 minutos** sem tráfego HTTP. A primeira requisição após repouso leva **30 a 60 segundos** para subir a JVM/Spring Boot. | Sem expiração de 30 dias para o Web Service. | **Recomendado** (instância única com HTTPS nativo). |
| **Banco PostgreSQL (Opção A)**: **Neon Free Plan** (`neon.tech/pricing`) | `$0/mês`; **não exige cartão de crédito**; sem cobrança automática de excedente. | PostgreSQL gerenciado com **1 GB** de armazenamento persistente por projeto e **100 CU-hours/mês** (~400h ativas a 0.25 CU). Suporta extensão/GiST nativo e exige TLS (`sslmode=require`). | **Scale-to-zero** após **5 minutos** sem queries SQL; reativa automaticamente em **< 1 segundo** na primeira conexão JDBC. | **Não expira** após 30 dias. | **Recomendado** (persistência contínua durante todo o período de avaliação). |
| **Banco PostgreSQL (Opção B)**: **Supabase Free Plan** (`supabase.com/pricing`) | `$0/mês`; não exige cartão de crédito. | PostgreSQL dedicado com **500 MB** de armazenamento por projeto (até 2 projetos ativos). Exige TLS. | **Pausa** o projeto após **7 dias** sem consultas SQL; exige clique manual (*Resume*) no painel se ficar 1 semana sem uso. | Não expira em 30 dias (dados retidos por 1 ano se pausado). | **Viável** (requer acesso semanal para evitar pausa). |
| **Banco PostgreSQL (Opção C)**: **Render Free Postgres** (`render.com/docs/free`) | `$0/mês`. | **1 GB** de armazenamento, `256 MB RAM`, até 100 conexões. | Não hiberna a cada 5 minutos enquanto válido. | **Expira em 30 dias** após a criação (14 dias de carência antes da exclusão total). | **Com ressalva** (usar apenas se criado a menos de 30 dias do fim da correção acadêmica). |

### 5.2 Proposta Viável Recomendada

1. **Combinação proposta**: **Render Free Web Service** (executando `target/cupcakes-0.0.1-SNAPSHOT.jar` em instância única com JDK 21) conectado via JDBC/TLS (`?sslmode=require`) ao **Neon Free PostgreSQL**.
2. **Vantagens para a avaliação acadêmica**:
   - Custo zero real, sem necessidade de cartão no banco de dados e sem risco de exclusão do banco após 30 dias.
   - HTTPS fornecido automaticamente pelo domínio `*.onrender.com`, compatível com `server.servlet.session.cookie.secure=true` do perfil `prod`.
   - Suporte integral às migrações Flyway `V1`–`V4` (incluindo a restrição `EXCLUDE USING gist` sobre `int8range` na `V3`).
3. **Aviso obrigatório aos avaliadores sobre retomada (*Cold Start*)**:
   - No plano gratuito do Render, após 15 minutos de inatividade, o primeiro acesso ao endereço HTTPS pode aguardar **30 a 60 segundos** enquanto o contêiner Java e o pool HikariCP inicializam; os acessos seguintes respondem imediatamente.

## 6. Pendências Remanescentes para Publicação e Entrega

1. Selecionar aplicação e PostgreSQL em planos gratuitos verificados nas páginas oficiais e registrar limitações relevantes até o prazo da entrega.
2. Confirmar faixas acadêmicas e carregar dados necessários por rotina controlada/idempotente, sem alterar migrações já aplicadas nem sobrescrever usuários/produtos existentes.
3. Preparar configuração `prod`, variáveis e artefato executável (`target/cupcakes-0.0.1-SNAPSHOT.jar`); validar TLS até o banco conforme exigência do provedor e HTTPS na aplicação.
4. Registrar endereço público, provedores, comandos reais, procedimento seguro de administrador e eventual suspensão do plano gratuito.
5. Executar *smoke test* remoto: catálogo, cadastro, login, endereço, revisão, pagamento simulado, consulta e logout; conferir persistência e cookies no domínio HTTPS.
6. Atualizar formulário de entrega com GitHub, URL da solução e vídeos. Repositório público não torna a aplicação automaticamente hospedada.

## 7. Participantes

Os cinco relatos de avaliação já foram fornecidos pelo responsável, transcritos no DOC e consolidados tecnicamente em [avaliacao-participantes-e-laudo.md](../entrega/avaliacao-participantes-e-laudo.md) (mantendo o anexo nominal apenas localmente fora do repositório público). Não são exigidas novas notas ou questionários que o modelo da disciplina não solicita; campos não coletados não são calculados ou inventados.
