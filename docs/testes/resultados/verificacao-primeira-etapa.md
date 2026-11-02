# Registro da verificação da primeira etapa

Este marco abrange somente catálogo e detalhes de produtos, conforme a [SPEC](../../../specs/07-SPEC-CATALOGO.md). O [plano e resultados](plano-e-resultados.md) é o índice dos casos, evidências e pendências.

## Proteção da integração

- [ProductPostgresIntegrationTest](../../../src/test/java/br/com/pit/cupcakes/catalogo/ProductPostgresIntegrationTest.java): removida a condição que ignorava a execução por indisponibilidade da porta; conferência da conexão efetiva antes das limpezas.
- [PostgresTestDatabaseGuard](../../../src/test/java/br/com/pit/cupcakes/catalogo/PostgresTestDatabaseGuard.java): validação da configuração antes de inicializar o contexto; somente PostgreSQL localhost:5432/cupcakes_test, Flyway ativo e Hibernate validate. Cada conexão é conferida antes de ser entregue a Flyway, Hibernate ou repositórios, inclusive com `current_database()`.
- [application-test-pg.yml](../../../src/test/resources/application-test-pg.yml): sem senha padrão, limpeza Flyway desabilitada e sem baseline automático.
- [PostgresTestDatabaseGuardTest](../../../src/test/java/br/com/pit/cupcakes/catalogo/PostgresTestDatabaseGuardTest.java): quatro testes de aceitação/rejeição de configuração, credenciais e banco efetivo divergente.

A tentativa anterior do agente foi bloqueada por falta de credenciais, com cinco erros de preparação e zero ignorados. Esse resultado histórico foi superado pela execução posterior do responsável: os relatórios atuais registram os cinco testes PostgreSQL aprovados, sem falhas, erros ou ignorados. Nesta tarefa, os relatórios foram conferidos; não houve nova execução desses testes pelo agente.

## Resultado atual comprovado

- 16 testes unitários/web/H2 e quatro de proteção repetidos pelo agente após o ajuste de configuração, todos aprovados, sem falhas, erros ou ignorados. Os cinco PostgreSQL foram aprovados na execução do responsável, separada dessa repetição. [Resumo sanitizado com hashes](testes-automatizados.json).
- Log da integração confirma PostgreSQL 18.6, banco `cupcakes_test`, validação de uma migração e aplicação de V1 em `public`. Isso não comprova o histórico de `cupcakes`.
- 11 cenários de navegador aprovados na verificação já registrada: catálogo, detalhes, inexistente, IDs inválidos, disponibilidade, imagem substituta, falha de rede, recuperação, teclado e larguras 360/1280. [Resultado estruturado](verificacao-navegador.json) e capturas relacionadas no plano.
- Naquela verificação, index.html, produto.html e os módulos catalogo.js/detalhes.js servidos em localhost:8080 foram comparados byte a byte com o projeto. A captura móvel do catálogo também foi inspecionada visualmente.

## Migração e persistência de desenvolvimento

Permanecem pendentes de comprovação. Este processo não recebeu credenciais e não foi usada conexão alternativa. O [script de coleta](../casos/verificar_persistencia.ps1) usa `psql` em transação somente de leitura para registrar o histórico Flyway e todos os campos dos produtos antes/depois. A execução foi solicitada ao responsável no terminal já configurado, com reinício `local,demo` somente da aplicação.

A sintaxe PowerShell foi validada; a coleta autenticada ainda não foi executada pelo agente. Nenhum produto foi removido, banco apagado ou Flyway clean executado. TC02 visual em ambiente isolado também permanece pendente; o caso HTTP vazio já está aprovado na integração PostgreSQL.

## Preparação do marco no Git

A raiz e seus diretórios pais foram consultados antes da inicialização. Não havia repositório; foi inicializado um repositório local na raiz do projeto. Identidade Git já configurada foi preservada; nenhuma configuração global ou remoto foi criado/alterado.

A seleção inclui código, recursos, configurações públicas, migração, testes, Maven Wrapper, SPEC e documentação/evidências desta etapa. Contexto operacional, prompts, ferramentas locais, IDE, target, logs, temporários e segredos ficam fora. A captura do terminal PostgreSQL contém caminho pessoal da máquina e foi preservada localmente, excluída do marco; a comprovação versionada usa o resumo sanitizado dos relatórios. Capturas legadas e tentativas anteriores de teclado também foram preservadas fora da seleção.

O perfil local exige DB_PASSWORD explicitamente, sem senha padrão no arquivo versionado. O exemplo de ambiente contém somente valores fictícios. A coleta PowerShell foi validada sintaticamente e em simulação isolada dentro de target; essa simulação não é evidência de acesso PostgreSQL.

Nenhuma funcionalidade adicional, cadastro, login, carrinho ou publicação faz parte desta tarefa. O primeiro marco pode registrar as pendências reais sem declarar a aplicação completa.
