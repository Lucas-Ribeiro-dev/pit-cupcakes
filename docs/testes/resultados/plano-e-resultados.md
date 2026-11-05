# Plano e resultados da primeira etapa

Entrega limitada ao catálogo público e aos detalhes dos produtos, conforme a [SPEC](../../../specs/07-SPEC-CATALOGO.md). Não equivale à conclusão integral de HU01/HU02 nem da aplicação.

## Base de comprovação

Os relatórios atuais de `target/surefire-reports` foram lidos nesta tarefa. A integração foi executada pelo responsável no terminal configurado; não foi executada novamente pelo agente. Os 20 testes sem PostgreSQL externo foram repetidos pelo agente após retirar a senha padrão do perfil local: BUILD SUCCESS, sem falhas, erros ou ignorados. As evidências de navegador pertencem à execução já registrada, separada abaixo. O [resumo sanitizado](testes-automatizados.json) preserva contagens, nomes dos casos, instantes de modificação e hashes SHA-256 dos XML consultados; os arquivos brutos, com propriedades de máquina, ficam fora do Git.

A informação antiga de PostgreSQL ausente está superada: o log da integração registra PostgreSQL 18.6 em `jdbc:postgresql://localhost:5432/cupcakes_test`, validação de uma migração e aplicação de `V1__criar_tabela_produto.sql` no esquema `public`. Isso comprova o banco de teste, sem comprovar o histórico de `cupcakes`.

## Testes automatizados conferidos

| Categoria / classe | Aprovados | Falhas | Erros | Ignorados | Limite da evidência |
| --- | ---: | ---: | ---: | ---: | --- |
| Camada web — ProductControllerTest | 7 | 0 | 0 | 0 | MockMvc com serviço simulado; não consulta PostgreSQL |
| Serviço — ProductServiceTest | 5 | 0 | 0 | 0 | Repositório simulado |
| Demonstração — DemoDataLoaderTest | 2 | 0 | 0 | 0 | Carga e prevenção de duplicação com mocks |
| Repositório — ProductRepositoryTest | 2 | 0 | 0 | 0 | H2; não valida migração PostgreSQL |
| Proteção — PostgresTestDatabaseGuardTest | 4 | 0 | 0 | 0 | Configuração e conexão simuladas |
| Integração — ProductPostgresIntegrationTest | 5 | 0 | 0 | 0 | PostgreSQL efetivo cupcakes_test, Flyway e Hibernate validate |

São **25 casos distintos aprovados**, sem falhas, erros ou ignorados nos relatórios atuais, provenientes de execuções distintas. Desse total, 16 são unitários/web/H2, quatro são de proteção e cinco são de integração PostgreSQL. Não representam uma única execução de 25 testes nesta tarefa.

Os cinco casos PostgreSQL comprovados são catálogo populado, catálogo vazio em banco isolado, consulta por ID existente, ID inexistente e ID inválido. A condição antiga de ignorar testes por indisponibilidade da porta foi removida; uma execução explicitamente solicitada deve falhar se a conexão/configuração não for válida.

## Navegador: execução efetivamente registrada

Automação Playwright Python com Microsoft Edge headless, consultando localhost:8080. Quatro recursos servidos foram comparados byte a byte com os arquivos do projeto na verificação anterior. Os cenários abaixo não foram repetidos nesta tarefa. [Resultado estruturado](verificacao-navegador.json).

| Caso | Verificação aprovada | Evidências |
| --- | --- | --- |
| TC01 | Catálogo com quatro produtos, ordenação e campos públicos | [Catálogo 1280](../evidencias/HU01_TC01_catalogo-1280-verificacao.png) |
| TC03 | Detalhes de produto existente e preço em reais | [Detalhes](../evidencias/HU02_TC03_detalhes-1280-verificacao.png) |
| TC04 | API 404 e mensagem visual de produto inexistente | [Inexistente](../evidencias/HU02_TC04_produto-inexistente-verificacao.png) |
| TC05 | ID ausente, alfabético, zero e negativo sem fetch inválido; API 400 | [ID inválido](../evidencias/HU02_TC05_id-invalido-verificacao.png) |
| TC06 | Rótulo de indisponível e imagem substituta carregada | [Indisponibilidade](../evidencias/HU02_TC06_indisponibilidade-verificacao.png), [imagem substituta](../evidencias/HU02_TC06_imagem-substituta-verificacao.png) |
| TC07 | Catálogo: requisição abortada, erro distinto de vazio e tentar novamente | [Falha](../evidencias/HU01_TC07_catalogo-falha-rede-verificacao.png), [recuperação](../evidencias/HU01_TC07_catalogo-recuperado-verificacao.png) |
| TC07 | Detalhes: requisição abortada, mensagem e tentar novamente | [Falha](../evidencias/HU02_TC07_detalhes-falha-rede-verificacao.png), [recuperação](../evidencias/HU02_TC07_detalhes-recuperado-verificacao.png) |
| TC09 | Catálogo 360 px: sem rolagem horizontal, foco visível, Tab e Enter | [360 px](../evidencias/HU01_TC09_catalogo-360-teclado-verificacao-reexecucao1.png) |
| TC09 | Detalhes 360 px: sem rolagem horizontal, foco visível, Tab e Enter | [360 px](../evidencias/HU02_TC09_detalhes-360-teclado-verificacao.png) |
| TC09 | Catálogo 1280 px: sem rolagem horizontal, foco visível, Tab e Enter | [1280 px](../evidencias/HU01_TC09_catalogo-1280-teclado-verificacao-reexecucao1.png) |
| TC09 | Detalhes 1280 px: sem rolagem horizontal, foco visível, Tab e Enter | [1280 px](../evidencias/HU02_TC09_detalhes-1280-teclado-verificacao.png) |

Resultado final registrado: **11 cenários aprovados, zero falhos**. A primeira tentativa dos quatro cenários de teclado falhou na automação por não aguardar navegação; a espera foi corrigida e os quatro foram repetidos com sucesso. Capturas de tentativas e capturas legadas permanecem locais; a seleção do marco usa as evidências da verificação aprovada. Nenhuma captura existente foi apagada.

## Desenvolvimento: migração e persistência

**Aprovado pelo responsável e incorporado na segunda etapa, sem repetição do teste.** As coletas [antes](persistencia-antes.json) e [depois](persistencia-depois.json) conferem o banco `cupcakes`, V1 bem-sucedida com checksum 39887744 e quatro produtos. A comparação dos JSON confirmou histórico e todos os campos dos produtos idênticos, IDs preservados e nenhuma duplicação.

O [roteiro de coleta somente de leitura](../casos/verificar_persistencia.ps1) fica disponível para referência. A [captura do reinício](../evidencias/TC08_persistencia-apos-reinicio.png) e a confirmação do responsável completam TC08. Isso não comprova V2 nem autenticação, verificadas separadamente no [registro da segunda etapa](verificacao-segunda-etapa.md).

## Pendências reais

- TC02 no navegador: mensagem de catálogo vazio em ambiente isolado ainda não executada. O contrato HTTP vazio já passou no PostgreSQL de teste; isso não comprova a tela vazia.
- Correspondência direta entre o catálogo visual e uma leitura SQL de `cupcakes`: pendente da coleta.

Não há falhas nos relatórios automatizados atuais ou nos resultados finais do navegador registrado. A falta de evidência das pendências não deve ser registrada como reprovação funcional.

## Índice da segunda etapa

O marco posterior de cadastro e autenticação está consolidado no [registro da segunda etapa](verificacao-segunda-etapa.md). Ele relaciona os relatórios atuais de **7 testes PostgreSQL aprovados**, o resultado automatizado de navegador em [autenticacao-navegador.json](autenticacao-navegador.json), a verificação manual de e-mail duplicado e as capturas de evidência. Os cinco testes PostgreSQL de catálogo descritos acima permanecem como histórico da primeira etapa; os dois testes de autenticação PostgreSQL são acrescentados no registro posterior.
