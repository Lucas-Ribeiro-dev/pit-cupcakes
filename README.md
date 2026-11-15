# PIT Cupcakes

Loja virtual de cupcakes desenvolvida para o **Projeto Integrador Transdisciplinar em Engenharia de Software II**. O projeto reúne uma aplicação web responsiva para clientes, uma área administrativa e um banco de dados relacional, cobrindo o percurso de escolha dos produtos até o acompanhamento do pedido.

**[Acessar o site](https://pit-cupcakes.onrender.com)**

![Cupcakes apresentados na identidade visual da loja](src/main/resources/static/assets/img/hero-cupcakes.webp)

## Sobre o projeto

O PIT Cupcakes foi construído para aplicar, em uma solução integrada, levantamento de requisitos, modelagem, desenvolvimento de interfaces, regras de negócio, persistência, segurança e testes de software.

O cenário escolhido é o de uma pequena loja de cupcakes que precisa apresentar seus produtos, receber pedidos para entrega ou retirada e organizar o atendimento. A solução permite ao cliente realizar uma compra simulada e ao administrador gerenciar o catálogo e a evolução dos pedidos.

O pagamento é exclusivamente acadêmico: **nenhum valor é cobrado e não há coleta de dados de cartão**.

## Funcionalidades

### Para o cliente

- Consultar o catálogo, a descrição, o preço e a disponibilidade dos produtos.
- Criar uma conta, entrar e sair com autenticação por sessão.
- Cadastrar, editar e remover endereços, com auxílio opcional de preenchimento pelo ViaCEP.
- Adicionar produtos ao carrinho, alterar quantidades e remover itens.
- Escolher entrega em endereço cadastrado ou retirada gratuita.
- Consultar o frete conforme as faixas de CEP atendidas.
- Revisar os itens e valores antes de confirmar o pedido.
- Simular pagamento aprovado ou recusado.
- Consultar o histórico e os detalhes dos próprios pedidos.

O catálogo e o carrinho podem ser utilizados por visitantes. A autenticação é obrigatória para concluir o pedido.

### Para o administrador

- Cadastrar e editar produtos, imagens, preços e disponibilidade.
- Consultar os pedidos recebidos e seus detalhes.
- Atualizar o atendimento conforme as transições permitidas para entrega ou retirada.
- Cancelar pedidos nos estados autorizados pelo sistema.

A administração exige perfil próprio. O cadastro público cria exclusivamente contas de cliente.

## Evolução da solução

O desenvolvimento ocorreu de forma incremental, com entregas funcionais verificadas ao longo do processo:

1. **Catálogo e persistência:** consulta pública de produtos, página de detalhes, banco PostgreSQL e migrações de estrutura.
2. **Contas e segurança:** cadastro, autenticação, encerramento de sessão e separação de permissões entre cliente e administrador.
3. **Endereços e recebimento:** manutenção dos endereços, integração opcional com ViaCEP e cálculo de frete por faixa de CEP.
4. **Fluxo de compra:** carrinho em sessão, revisão do pedido, pagamento simulado e registro do histórico.
5. **Administração e qualidade:** gestão de produtos e pedidos, testes integrados e tratamento dos apontamentos de avaliação.
6. **Acabamento e publicação:** padronização visual das telas, imagens dos produtos, ajustes de responsividade e implantação no Render com banco no Neon.

Entre as melhorias de usabilidade estão a confirmação mais perceptível de inclusão no carrinho, a identificação das opções de pagamento simulado e a organização das informações em telas estreitas. O [laudo de qualidade](docs/entrega/avaliacao-participantes-e-laudo.md) registra o tratamento dos apontamentos, seus limites e as evidências de reteste.

## Tecnologias

| Camada | Tecnologias |
| --- | --- |
| Interface | HTML, CSS e JavaScript vanilla |
| Backend | Java 21 e Spring Boot 3.4.2 |
| Segurança | Spring Security, sessões HTTP, BCrypt e proteção CSRF |
| Persistência | Spring Data JPA, Hibernate e PostgreSQL |
| Evolução do banco | Flyway |
| Build | Maven Wrapper |
| Testes | JUnit, Spring Boot Test, MockMvc, H2, PostgreSQL e Playwright |
| Publicação | Docker e Render |
| Banco hospedado | Neon PostgreSQL |

## Arquitetura

A aplicação é um monólito modular organizado por domínio: catálogo, identidade, endereços, frete, carrinho e pedidos. Controllers recebem as requisições, services executam regras de negócio e repositories fazem o acesso ao banco. A interface utiliza os endpoints da aplicação para consultar e atualizar informações.

Os preços, subtotais, frete e total são calculados no servidor. A confirmação do pedido revalida os dados e utiliza transações e controle de repetição para evitar registros duplicados. Os pedidos preservam os dados históricos dos itens e do endereço usados na compra.

O carrinho fica na sessão HTTP. Usuários, endereços, produtos e pedidos são persistidos no PostgreSQL. As migrações Flyway versionam a estrutura do banco.

Detalhes em [visão arquitetural](docs/arquitetura/visao-geral.md) e [carrinho em sessão](docs/arquitetura/carrinho-em-sessao.md).

## Identidade visual

A interface utiliza Raspberry Pink (`#BC164D`), Pale Pink (`#FFD0DF`) e pequenos detalhes em Butter Yellow (`#FFE164`). Lilita One dá destaque aos títulos; Varela Round é usada nos textos e controles. A marca combina as duas famílias.

As telas compartilham padrões de navegação, campos, mensagens e ações. As imagens dos cupcakes são ilustrativas, produzidas para a demonstração acadêmica. As licenças das fontes acompanham seus arquivos.

## Executar localmente

### Requisitos

- JDK 21, com `JAVA_HOME` configurado para a instalação local.
- PostgreSQL disponível e um banco de desenvolvimento chamado `cupcakes`.
- Git para obter o repositório. O Maven é disponibilizado pelo wrapper do projeto.

Clone o repositório e abra sua pasta:

```powershell
git clone https://github.com/Lucas-Ribeiro-dev/pit-cupcakes.git
cd pit-cupcakes
```

No PowerShell, configure a conexão e inicie a aplicação:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/cupcakes'
$env:DB_USERNAME = Read-Host 'Usuário do PostgreSQL'
$dbCredential = Get-Credential -UserName $env:DB_USERNAME -Message 'Senha do PostgreSQL'
$env:DB_PASSWORD = $dbCredential.GetNetworkCredential().Password
$env:SPRING_PROFILES_ACTIVE = 'local,demo'
.\mvnw.cmd spring-boot:run
```

Acesse **http://localhost:8080**. O Flyway aplica as migrações pendentes; não é necessário criar as tabelas manualmente. O perfil `demo` carrega quatro produtos e duas faixas de frete quando as respectivas tabelas estão vazias.

O arquivo [.env.example](.env.example) documenta as variáveis disponíveis; ele não é carregado automaticamente pelo Spring Boot.

### Acesso administrativo

Para provisionar um administrador, configure simultaneamente `ADMIN_EMAIL`, `ADMIN_NAME` e `ADMIN_PASSWORD` no ambiente antes da inicialização. Não há credenciais administrativas públicas ou padrão. O provisionamento não promove uma conta de cliente já existente nem sobrescreve silenciosamente um administrador.

Após entrar com a conta administrativa, acesse `/admin/index.html`. Consulte o [manual](docs/manual/manual-usuario-e-publicacao.md) para configuração e operação.

## Qualidade e testes

A verificação final de 14/11/2026 registrou:

| Verificação | Resultado |
| --- | --- |
| H2 e MockMvc | 64 testes aprovados |
| Integração com PostgreSQL | 15 testes aprovados |
| Navegador Microsoft Edge | 28 cenários aprovados, sem erros JavaScript |

Os testes Java totalizaram **79 aprovações**, sem falhas, erros ou casos ignorados nas execuções registradas. O navegador verificou o fluxo de entrega em larguras de desktop e celular; a retirada também foi coberta pelas suítes de API e PostgreSQL. A consulta externa de CEP teve respostas controladas nos testes de navegador.

Os testes PostgreSQL utilizam exclusivamente `cupcakes_test`, com proteção de conexão. Não devem ser executados sobre o banco publicado ou o banco de desenvolvimento.

Consulte o [relatório final](docs/testes/resultados/verificacao-final-publicacao.md), os [roteiros de teste](docs/testes/casos/) e as [evidências](docs/testes/evidencias/). As instruções de reprodução estão no [manual](docs/manual/manual-usuario-e-publicacao.md).

## Publicação e limites da demonstração

A aplicação é executada em contêiner Docker no Render e utiliza PostgreSQL no Neon, com configuração por variáveis de ambiente. As credenciais ficam nos serviços de hospedagem.

- O serviço gratuito pode levar algum tempo para responder após um período de inatividade.
- Sessão e carrinho são temporários e podem ser perdidos após expiração ou reinício da aplicação; os pedidos gravados permanecem no banco.
- A entrega utiliza faixas de CEP demonstrativas de Brasília/DF, sem representar cobertura de toda a cidade. A retirada é gratuita.
- A disponibilidade dos produtos é apresentada como disponível ou indisponível, sem controle quantitativo de estoque.
- Pagamentos são simulados. Filtros de catálogo e estimativa de prazo de entrega são possibilidades de evolução.

## Autor

Desenvolvido por [Lucas Ribeiro](https://github.com/Lucas-Ribeiro-dev) como projeto acadêmico do curso de Engenharia de Software.
