# Registro da terceira etapa — endereços e frete

Implementação de HU17/HU18 e consulta de frete necessária à HU08 conforme a [SPEC](../../../specs/10-SPEC-ENDERECOS-FRETE.md). Verificação consolidada: **48 testes independentes de PostgreSQL já aprovados, 13 testes PostgreSQL atuais conferidos sem repetição e 18 cenários de navegador aprovados com servidor real**. Brasília–DF é a referência confirmada. Faixas e valores comerciais finais permanecem pendentes.

## Alterações

- [V3](../../../src/main/resources/db/migration/V3__criar_endereco_faixa_frete.sql) cria endereco e faixa_frete sem carga de faixas. V1/V2 preservadas. FK e índice por usuário; CHECKs de formato, tamanho, obrigatoriedade, UF, versão e valores. Restrição EXCLUDE GiST sobre intervalo inclusivo impede faixas ativas sobrepostas, inclusive entre transações concorrentes, sem extensão adicional.
- [Módulo de endereços](../../../src/main/java/br/com/pit/cupcakes/endereco) obtém proprietário pelo usuário persistido da sessão; filtra detalhe/alteração/remoção por ID e proprietário. DTOs rejeitam propriedades indevidas. CEP preserva zeros iniciais; textos são aparados; número aceita S/N; UF é normalizada. PUT/DELETE verificam versão e usam @Version na escrita efetiva. Alheio e inexistente têm o mesmo 404.
- [Cliente ViaCEP](../../../src/main/java/br/com/pit/cupcakes/endereco/integration/PostalCodeClient.java) usa host/caminho fixos, sem redirecionamento ou repetição automática. Conexão limitada a dois segundos e espera total do corpo a quatro segundos. Somente CEP é enviado; erros 404/502/504 são distintos e não expõem conteúdo externo. A [DT01](../../decisoes/DT01-viacep.md) registra a decisão e seus limites.
- [Módulo de frete](../../../src/main/java/br/com/pit/cupcakes/frete) calcula taxa no servidor a partir do endereço próprio salvo, UF DF e exatamente uma faixa ativa. Não compara literalmente cidade. RETIRADA é 0.00, sem endereço e sem seleção automática. Sem faixas não há entrega gratuita implícita. Cotação inclui versão do endereço e não garante preço futuro.
- [Tela](../../../src/main/resources/static/paginas/conta/enderecos.html) e [JavaScript](../../../src/main/resources/static/assets/js/cliente/enderecos.js): lista, vazio, inclusão, edição, cancelamento, remoção confirmada e consulta de frete. Dados entram por textContent/value. Mudanças invalidam cotação visual. Conflito mantém texto digitado. ViaCEP cancela/ignora requisições antigas e preserva campos editados após o início da consulta. Salvamento manual segue disponível após falha.
- Minha conta liga a endereços. Login admite apenas o destino interno fixo `destino=enderecos`; não recebe URL de retorno arbitrária. A página valida sessão no carregamento e esconde conteúdo privado ao sair. APIs continuam sob a cadeia de segurança, CSRF e no-store existentes. Não há alteração de cadastro, catálogo, perfil, favicon ou operações de compra.
- [Dicionário](../../banco-de-dados/dicionario-dados.md), [README](../../../README.md) e [script controlado de configuração](../../banco-de-dados/configurar-frete.sql) atualizados. Ajustes comerciais não devem alterar migrações aplicadas. O script não foi executado.

## Preservação

Estado inicial na branch master: README e registro da primeira etapa modificados; SPEC, cinco capturas anteriores e duas coletas de persistência não rastreadas. Essas alterações foram preservadas. README recebeu alterações pontuais para descrever a nova etapa; o registro da primeira etapa e as evidências anteriores não foram editados nesta tarefa.

Não foram executados commit, push, publicação, Flyway clean ou acesso/limpeza de cupcakes. A tentativa inicial de PostgreSQL foi bloqueada antes da conexão; o responsável posteriormente executou a suíte e iniciou o servidor isolado com cupcakes_test. Os relatórios atuais foram conferidos e o navegador usou esse servidor já ativo em 8092, sem iniciá-lo, reiniciá-lo ou encerrá-lo. A proteção existente de URL/perfil, credenciais e current_database() permanece intacta. Os servidores em 8091/8092 foram preservados. Nesta consolidação houve alterações somente em roteiros de verificação, resultados, capturas e documentação; nenhum código da aplicação foi alterado.

## Verificações automatizadas

Ambiente de execução: Windows, JDK 21.0.7, Maven Wrapper 3.9.9, Spring Boot 3.4.2, Python 3.13.3, Playwright e Microsoft Edge headless. Os [resultados sanitizados](enderecos-automatizados.json) registram contagens, casos, instantes e hashes dos XML conferidos, sem propriedades locais ou credenciais. Relatórios brutos ficam em target, fora do Git.

| Classe | Testes | Falhas/erros/ignorados | Abrangência |
| --- | ---: | --- | --- |
| AddressValidationTest | 3 | 0/0/0 | Normalização, acentos, zeros iniciais, S/N, CEP estrito, versão, UF e limites |
| AddressApiTest | 7 | 0/0/0 | CRUD, propriedade, versão, campos indevidos, limites, sessão/CSRF, formatos, retirada, frete e CEP simulado; persistência H2 |
| PostalCodeClientTest | 5 | 0/0/0 | Campos vazios, mapeamento, URL/headers, resposta inválida, inexistência, erro HTTP/conexão, timeout e corpo que não termina; HTTP simulado |
| AuthenticationSecurityTest | 8 | 0/0/0 | Regressão de filtros, sessão, cadastro, login, logout, CSRF e autorização |
| RegistrationServiceTest | 5 | 0/0/0 | Regressão de cadastro |
| ProductControllerTest | 7 | 0/0/0 | Regressão da API pública e segurança |
| ProductServiceTest | 5 | 0/0/0 | Regressão de serviço |
| ProductRepositoryTest | 2 | 0/0/0 | Regressão de repositório H2 |
| DemoDataLoaderTest | 2 | 0/0/0 | Regressão de carga demo |
| PostgresTestDatabaseGuardTest | 4 | 0/0/0 | Rejeição de configuração/conexão não autorizada, incluindo cupcakes |

Total final: **48 testes, zero falhas, erros ou ignorados; BUILD SUCCESS**. Não representa aprovação de migrações PostgreSQL. O motor foi exercitado com limites fictícios inclusivos 70000001–70000009 e valor 12.34 em H2, além de CEP fora, faixa inativa e UF divergente. Essas fixtures não representam cobertura comercial.

A execução inicial de 45 testes passou. A ampliação para 48 revelou erro na configuração repetida de um mock de exceção; corrigida para doThrow, sem alteração no comportamento de produção. O roteiro de navegador inicialmente detectou que a consulta por blur precisava manter o disparo ao navegar para Buscar CEP. A correção mantém o blur e evita duplicação enquanto a mesma consulta está pendente. Os resultados finais abaixo refletem as reexecuções.

### PostgreSQL

Os cinco relatórios TXT/XML atuais em target/surefire-reports confirmam **13 testes, zero falhas, erros ou ignorados**. Foram lidos e seus hashes atualizados no [resumo sanitizado](enderecos-automatizados.json), sem repetir Maven ou iniciar novos contextos. Os arquivos PostgreSQL foram produzidos em 05/11/2026, entre 17:45:13 e 17:45:43, no horário local. A leitura usa contagens e casos dos XML, sem publicar suas propriedades locais.

| Classe | Testes aprovados | Abrangência conferida |
| --- | ---: | --- |
| AddressShippingPostgresTest | 5 | Limites/valores fictícios, ausência de faixa/propriedade, persistência/versões, CHECK/FK/exclusão e sobreposição concorrente |
| AddressRestartPostgresTest | 1 | Persistência de endereço em dois contextos |
| AuthenticationPostgresIntegrationTest | 1 | Regressão de autenticação PostgreSQL |
| AuthenticationRestartPostgresTest | 1 | Reinício, sessão e cookies |
| ProductPostgresIntegrationTest | 5 | Regressão de catálogo PostgreSQL |

Testes de faixas PostgreSQL usam somente cupcakes_test, com fixtures explicitamente fictícias e guard antes de Flyway. Casos transacionais revertem alterações. O teste de sobreposição concorrente usa duas conexões, confirma uma fixture e remove somente seu ID ao final. O teste de reinício remove apenas o endereço e usuário fictícios próprios. Os relatórios aprovam a execução com Flyway/Hibernate validate, a V3, as restrições GiST e o reinício; o bloqueio inicial por credenciais não é mais uma pendência. O histórico inicial teve 13 erros de inicialização por ausência de DB_TEST_*; esse resultado foi superado pelos relatórios atuais, sem usar credenciais de desenvolvimento como alternativa.

## Interface com APIs simuladas — execução anterior

[Roteiro](../casos/verificar_enderecos.py) executado com `--simulate-api`: **15 cenários aprovados, 14 capturas da execução concluída**, conforme [JSON](enderecos-interface-simulada.json). API inteira simulada: autenticação, dados em memória, CEP e frete. Arquivos estáticos reais foram servidos em porta local efêmera somente durante o roteiro, sem iniciar aplicação Java ou conectar banco. Capturas contêm aviso explícito de simulação. Uma captura de lista vazia da tentativa inicial foi preservada com nome distinto.

Foram verificados vazio, retorno de CEP, preservação de campo editado durante consulta, troca de CEP com resposta antiga, erros 404/502/504 recuperáveis, preenchimento manual após falha, texto HTML mostrado literalmente, criação, recarga em 360/1280, foco por Tab, retirada, entrega não configurada, edição, invalidação de cotação, conflito sem perda de texto, cancelamento e confirmação de remoção. Não houve erros JavaScript nem rolagem horizontal nos tamanhos testados.

Capturas representativas inspecionadas visualmente:

- [360 px e teclado](../evidencias/ADR09_enderecos-360-teclado-api-simulada.png)
- [1280 px e teclado](../evidencias/ADR09_enderecos-1280-teclado-api-simulada.png)
- [Conflito preservando texto](../evidencias/ADR04_conflito-preserva-texto-api-simulada.png)
- [Retirada gratuita](../evidencias/FRT04_retirada-api-simulada.png) e [entrega não configurada](../evidencias/FRT04_sem-configuracao-api-simulada.png) constam no roteiro; representam respostas simuladas.

Esses resultados permanecem identificados como simulação e não foram sobrescritos. Não são utilizados como comprovação de persistência ou autenticação reais; a execução abaixo fornece essa verificação integrada.

## Navegador com servidor real

Executado `python docs/testes/casos/verificar_enderecos.py`, sem --simulate-api, contra o AddressBrowserServer já disponível em **http://localhost:8092**, com cupcakes_test. Resultado final: **18 cenários aprovados, 16 capturas**, registrados no [JSON do navegador real](enderecos-navegador.json).

Autenticação, cadastro de usuário, sessão/CSRF, listagem/detalhe, criação, alteração, remoção e cotação usaram APIs reais. Não houve interceptação dessas APIs nem configuração de faixas. Apenas `GET /api/conta/cep/*` foi interceptado pelo Playwright para simular o auxílio externo ViaCEP: resposta válida, atraso de 700 ms e erros 404/502/504. Esse controle verifica a interface; o cliente HTTP do servidor tem seus próprios testes determinísticos. Não houve consulta real de fumaça ao ViaCEP.

Foi criada conta fictícia exclusiva por UUID em cada tentativa, sem consultar ou alterar contas anteriores. A tentativa inicial chegou ao conflito, mas falhou porque o roteiro digitava no campo antes de terminar o GET de edição. O teste passou a aguardar o número salvo antes de digitar; nenhum código da aplicação foi alterado. O [resultado parcial inicial](enderecos-navegador-tentativa-inicial.json), suas capturas e sua conta fictícia foram preservados. Na execução concluída, somente o endereço criado por essa conta foi removido pelo fluxo normal; a conta permanece no banco de teste. Não foi realizada limpeza de banco ou exclusão de contas.

Verificações concluídas:

- Cadastro e login reais com retorno ao destino interno de endereços; sessão após recarga; cookie HttpOnly/SameSite=Lax e no-store na resposta privada; visitante recebe 401 e POST sem CSRF recebe 403.
- Lista vazia; cadastro manual após falha do CEP; texto com marcação HTML exibido literalmente; consulta/listagem após recarga em 360/1280 px; foco por Tab e ausência de rolagem horizontal.
- Edição persistida; cotação visual invalidada ao editar; alteração adicional pela API real produz versão nova; PUT com versão antiga retorna conflito e mantém o texto digitado. DELETE com versão antiga retorna 409 ENDERECO_DESATUALIZADO e não remove nem sobrescreve o endereço.
- Retirada gratuita e entrega não configurada obtidas do servidor, com a mensagem associada ao endereço salvo. Nenhuma cobertura ou taxa foi inventada para o navegador. Limites de faixa, valores fictícios e recusa por UF/CEP são comprovados na suíte PostgreSQL acima.
- Cancelamento da confirmação de remoção preserva o endereço; confirmação remove; detalhe posterior retorna 404. Logout encerra a sessão, GET de sessão retorna 401 e acesso à tela privada redireciona ao login com destino interno.
- ViaCEP controlado preserva campo editado durante espera, ignora CEP anterior e permite continuar manualmente após inexistência, falha ou demora. Não houve erros JavaScript.

As **16 capturas da execução concluída foram inspecionadas visualmente**. Rótulos, mensagens, foco, quebra de texto e estado final correspondem aos cenários. Seleção de evidências reais:

| Verificação | Captura |
| --- | --- |
| Cadastro de endereço | [ADR01](../evidencias/ADR01_endereco-salvo-reexecucao1.png) |
| Sessão autenticada | [ADR05](../evidencias/ADR05_sessao-autenticada.png) |
| CEP atrasado/campo preservado | [ADR07](../evidencias/ADR07_campo-preservado-reexecucao1.png) |
| Falha recuperável de CEP controlado | [ADR06](../evidencias/ADR06_cep-70000005-reexecucao1.png) |
| 360 px e teclado | [ADR09 — 360](../evidencias/ADR09_enderecos-360-teclado-reexecucao1.png) |
| 1280 px e teclado | [ADR09 — 1280](../evidencias/ADR09_enderecos-1280-teclado-reexecucao1.png) |
| Retirada gratuita real | [FRT04 — retirada](../evidencias/FRT04_retirada-reexecucao1.png) |
| Entrega não configurada real | [FRT04 — entrega](../evidencias/FRT04_sem-configuracao-reexecucao1.png) |
| Edição persistida | [ADR04 — edição](../evidencias/ADR04_endereco-editado-reexecucao1.png) |
| Conflito preservando o texto | [ADR04 — conflito](../evidencias/ADR04_conflito-preserva-texto.png) |
| Remoção confirmada | [ADR04 — remoção](../evidencias/ADR04_endereco-removido.png) |
| Logout e retorno ao login | [ADR05 — logout](../evidencias/ADR05_logout-rota-privada.png) |

## Aceite por cenário

| Casos | Situação nesta execução |
| --- | --- |
| ADR01–05 | Aprovados em H2/filtros, PostgreSQL e fluxos reais de navegador; propriedade entre usuários coberta pelos testes automatizados |
| ADR06 | Aprovado com cliente HTTP e respostas simuladas; API e preenchimento manual cobertos; fumaça real não executada |
| ADR07 | Aprovado no servidor real, com apenas o auxílio de CEP controlado |
| FRT01–02 | Aprovados com fixtures fictícias em cupcakes_test; não configuram cobertura comercial |
| FRT03 | Aprovado no PostgreSQL, incluindo exclusão de faixas concorrentes |
| FRT04 | Aprovado em PostgreSQL e navegador real para retirada/entrega sem configuração; endereço alheio coberto automaticamente |
| ADR08 | Aprovado no relatório de reinício PostgreSQL atual |
| ADR09 | Aprovado no servidor real: 360/1280 px, teclado e capturas inspecionadas |
| REG01 | 33 testes independentes de PostgreSQL previamente aprovados; regressão PostgreSQL atual aprovada; cadastro/login/sessão/logout reais exercitados no roteiro de endereços |

## Reprodução e pendências

Os comandos de reprodução estão no [README](../../../README.md). Os relatórios atuais já confirmam aprovação PostgreSQL; não há necessidade de repetir a suíte para esta consolidação. Futuras execuções exigem DB_TEST_* no mesmo processo e DB_TEST_URL exclusivamente em `jdbc:postgresql://localhost:5432/cupcakes_test`. Não copiar dados de cupcakes, desabilitar o guard, editar V1/V2 ou executar clean.

Para o navegador, usar a [classe de teste isolada](../../../src/test/java/br/com/pit/cupcakes/endereco/AddressBrowserServer.java) com o mecanismo spring-boot:test-run e main-class explícita já indicado na SPEC:

```powershell
.\mvnw.cmd '-Dspring-boot.run.main-class=br.com.pit.cupcakes.endereco.AddressBrowserServer' spring-boot:test-run
python docs/testes/casos/verificar_enderecos.py
```

Nesta consolidação o servidor já estava iniciado pelo responsável. Não foi executado comando de inicialização ou encerramento; a reprodução acima usa o mecanismo spring-boot:test-run indicado na SPEC. Credenciais, V3, restrições concorrentes, reinício e fluxo real de navegador deixaram de ser pendências de comprovação. Consulta real de fumaça ao ViaCEP continua não executada, por ser opcional e externa aos testes determinísticos. O modo --simulate-api anterior não foi repetido após a ampliação das verificações exclusivas do servidor real.

Faixas e valores finais de Brasília–DF permanecem **pendentes de definição comercial**. Sem essa configuração, o sistema deve informar entrega não configurada e oferecer retirada gratuita. Nenhum valor fictício foi configurado como comercial. Carrinho, pedidos, checkout, administração e polimento visual não foram implementados.
