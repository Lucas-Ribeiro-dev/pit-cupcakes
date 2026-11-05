# Registro da segunda etapa — autenticação

Implementação de HU03/HU04/HU05 conforme a [SPEC](../../../specs/09-SPEC-AUTENTICACAO.md). O registro inicial de pendências abaixo foi atualizado ao final desta etapa com os relatórios PostgreSQL atuais, a execução do navegador e a verificação manual complementar.

## Alterações

- [pom.xml](../../../pom.xml): starter Security e spring-security-test sob gerenciamento existente do Boot 3.4.2, que seleciona Security 6.4.2. Java permanece 21.
- [V2](../../../src/main/resources/db/migration/V2__criar_tabela_usuario.sql): usuário, identidade BIGINT, e-mail único, perfil com CHECK e instante com fuso. V1 preservada.
- [identidade](../../../src/main/java/br/com/pit/cupcakes/identidade): entidade User, Role, DTOs públicos, validação, cadastro transacional, repositório e carregamento de usuário para autenticação. Senha literal com limites Unicode/UTF-8, BCrypt delegante e tratamento da restrição concorrente de e-mail.
- [SecurityConfig](../../../src/main/java/br/com/pit/cupcakes/config/SecurityConfig.java): autenticação de formulário do Spring Security, handlers JSON, proteção CSRF padrão em sessão, rotação do identificador, logout POST e regras de CLIENTE/ADMIN. Nenhum endpoint administrativo foi criado.
- [GlobalExceptionHandler](../../../src/main/java/br/com/pit/cupcakes/compartilhado/exception/GlobalExceptionHandler.java): erros esperados 400/403/404/405/409/415 sem detalhes SQL ou valores de senha; falhas inesperadas continuam genéricas.
- [application.yml](../../../src/main/resources/application.yml) e [application-prod.yml](../../../src/main/resources/application-prod.yml): sessão de 30 minutos, HttpOnly, SameSite=Lax e Secure obrigatório em prod.
- [páginas da conta](../../../src/main/resources/static/paginas/conta), [scripts cliente](../../../src/main/resources/static/assets/js/cliente) e [api.js](../../../src/main/resources/static/assets/js/compartilhado/api.js): cadastro com confirmação, login, conta, navegação, sessão e CSRF. CSS mínimo de formulários e largura adaptável. Catálogo/detalhes continuam públicos.
- [Dicionário](../../banco-de-dados/dicionario-dados.md) e [README](../../../README.md): esquema V2, contratos e uso. Registro da primeira etapa atualizado com os snapshots fornecidos, sem novo teste de persistência do catálogo.

## Executado

Ambiente: Windows, JDK 21.0.7, Maven Wrapper 3.9.9, Boot 3.4.2, Security 6.4.2. A compilação principal e dos testes passou. Diagnósticos da IDE sem erros na consulta final. [Resumo sanitizado](autenticacao-automatizados.json) contém contagens e hashes dos relatórios consultados; relatórios brutos permanecem em target fora do Git.

| Classe | Testes | Falhas/erros/ignorados | Abrangência |
| --- | ---: | --- | --- |
| AuthenticationSecurityTest | 8 | 0/0/0 | Filtros reais com repositório simulado: cadastro, validação, erros, login, sessão, CSRF, rotação, logout, autorização e formatos |
| RegistrationServiceTest | 5 | 0/0/0 | Normalização, limites Unicode/UTF-8, hash literal, duplicação e concorrência simulada |
| ProductControllerTest | 7 | 0/0/0 | Regressão do catálogo com cadeia de segurança real |
| ProductServiceTest | 5 | 0/0/0 | Regressão de serviço |
| ProductRepositoryTest | 2 | 0/0/0 | Regressão H2 existente; não comprova PostgreSQL ou V2 |
| DemoDataLoaderTest | 2 | 0/0/0 | Carga e prevenção de duplicação |
| PostgresTestDatabaseGuardTest | 4 | 0/0/0 | Configuração e conexão efetiva, incluindo rejeição de cupcakes |

Total da última execução: **33 testes, zero falhas, erros ou ignorados; BUILD SUCCESS**. A primeira tentativa teve uma falha no contrato 415 do login; a identificação da rota no filtro foi corrigida e a suíte repetida passou. O erro interno provocado no teste do catálogo é intencional e não constitui falha da suíte.

### PostgreSQL efetivo

| Classe | Testes | Falhas/erros/ignorados | Evidência |
| --- | ---: | --- | --- |
| AuthenticationPostgresIntegrationTest | 1 | 0/0/0 | Persistência, encoder, duplicidade e perfil em `cupcakes_test` |
| AuthenticationRestartPostgresTest | 1 | 0/0/0 | Reinício, persistência, login, cookies, sessão e logout |
| ProductPostgresIntegrationTest | 5 | 0/0/0 | Regressão do catálogo em PostgreSQL |

Total PostgreSQL: **7 testes aprovados**, conforme os relatórios atuais em `target/surefire-reports` e a [captura do resumo](../evidencias/AUTH_integracao-postgresql_7-testes-aprovados.png). Esses relatórios são a comprovação automatizada final; não foram repetidos nesta consolidação.

Também executados: compilação sintática Python do roteiro de navegador e importação de Playwright; conferência dos módulos JavaScript com Node; comparação dos snapshots já fornecidos. Essas verificações não comprovam execução dos formulários no navegador.

## Aceite e limites da comprovação

| Casos | Situação |
| --- | --- |
| AUTH01–03 | **Aprovado**: cadastro, validação, hash, duplicação e erros em serviço/MockMvc, com persistência e restrições V2 cobertas pelo `AuthenticationPostgresIntegrationTest` |
| AUTH04–09 | **Aprovado**: login, normalização, rotação, sessão, erros genéricos, CSRF, logout e autorização em MockMvc; cookies e fluxo após reinício cobertos pelo `AuthenticationRestartPostgresTest` |
| AUTH10 | **Aprovado**: duas inicializações, persistência da conta, novo login e logout no relatório PostgreSQL atual |
| AUTH11 | **Aprovado**: regressão de API pública, integração PostgreSQL e detalhes públicos no navegador; evidência em [HU02_AUTH11_detalhes-publicos.png](../evidencias/HU02_AUTH11_detalhes-publicos.png) |
| AUTH12 | **Aprovado**: [15 verificações automatizadas](autenticacao-navegador.json), capturas de cadastro/login/recarga/logout, falha de rede, teclado e larguras 360/1280; e-mail duplicado confirmado manualmente com HTTP 409 |

Não foram reutilizados relatórios PostgreSQL antigos como aprovação da autenticação. Nenhuma conexão a cupcakes, limpeza de banco, Flyway clean, commit, push ou publicação foi executado. Arquivos inicialmente não rastreados foram preservados. Favicon e demais funcionalidades fora do escopo não foram alterados.

## Histórico de execução e reprodução

Use JDK 21 no PATH/JAVA_HOME. Configure no mesmo terminal as credenciais próprias de cupcakes_test por entrada protegida; não as envie no chat ou grave em arquivos versionados:

```powershell
$testDatabaseCredential = Get-Credential -Message 'Credenciais locais do banco cupcakes_test'
$env:DB_TEST_USERNAME = $testDatabaseCredential.UserName
$env:DB_TEST_PASSWORD = $testDatabaseCredential.GetNetworkCredential().Password
$env:DB_TEST_URL = 'jdbc:postgresql://localhost:5432/cupcakes_test'
Remove-Item Env:SPRING_PROFILES_ACTIVE -ErrorAction SilentlyContinue
.\mvnw.cmd '-Dtest=AuthenticationPostgresIntegrationTest,AuthenticationRestartPostgresTest,ProductPostgresIntegrationTest' test
```

[AuthenticationPostgresIntegrationTest](../../../src/test/java/br/com/pit/cupcakes/identidade/AuthenticationPostgresIntegrationTest.java) consulta a conta persistida, verifica o encoder e testa as restrições únicas/perfil. Usa V1/V2 e remove somente sua conta fictícia por e-mail exclusivo. [AuthenticationRestartPostgresTest](../../../src/test/java/br/com/pit/cupcakes/identidade/AuthenticationRestartPostgresTest.java) inicia/encerra dois contextos, verifica persistência da conta, login após reinício, cookies, sessão e logout em HTTP real. Todos usam a [proteção efetiva existente](../../../src/test/java/br/com/pit/cupcakes/catalogo/PostgresTestDatabaseGuard.java), antes de Flyway e de qualquer acesso. Falta de credenciais deve falhar; não deve ignorar testes. A regressão PostgreSQL existente limpa somente produtos em cupcakes_test.

Para repetir apenas os 33 casos já aprovados:

```powershell
.\mvnw.cmd '-Dtest=AuthenticationSecurityTest,RegistrationServiceTest,ProductControllerTest,ProductServiceTest,ProductRepositoryTest,DemoDataLoaderTest,PostgresTestDatabaseGuardTest' test
```

Para referência histórica, o navegador foi executado no [servidor exclusivo de testes](../../../src/test/java/br/com/pit/cupcakes/identidade/AuthenticationBrowserServer.java) com DB_TEST_* configuradas. Ele adiciona o perfil test-pg e a proteção de conexão efetiva, sem demo. O roteiro concluído não deve ser confundido com uma pendência:

```powershell
.\mvnw.cmd '-Dspring-boot.run.main-class=br.com.pit.cupcakes.identidade.AuthenticationBrowserServer' '-Dspring-boot.run.useTestClasspath=true' spring-boot:run
```

Em outro terminal, com Python e Playwright disponíveis e Microsoft Edge instalado, é possível reproduzir a verificação sem usar os dados da conta manual:

```powershell
python docs/testes/casos/verificar_autenticacao.py
```

O [roteiro](../casos/verificar_autenticacao.py) usou somente localhost:8091 e criou uma conta fictícia com e-mail UUID, mantida em cupcakes_test para inspeção. O catálogo usou os dados de teste existentes. As capturas aprovadas estão preservadas e foram inspecionadas; o JSON não contém senha. Não se deve copiar ou limpar cupcakes para reproduzir esse cenário.

Antes de disponibilização pública: proteção contra abuso, avaliação do custo BCrypt no ambiente final e validação de cookies em HTTPS. Confirmação por e-mail, recuperação/alteração de senha e preparação de administrador real ficam fora desta fatia.

## Atualização final da verificação

Conferi os relatórios atuais em `target/surefire-reports` no ambiente de teste `cupcakes_test`: **7 testes PostgreSQL aprovados**, sem falhas, erros ou ignorados. A composição é: `AuthenticationPostgresIntegrationTest` (1), `AuthenticationRestartPostgresTest` (1) e `ProductPostgresIntegrationTest` (5). A captura do resumo está em [AUTH_integracao-postgresql_7-testes-aprovados.png](../evidencias/AUTH_integracao-postgresql_7-testes-aprovados.png).

Executei [verificar_autenticacao.py](../casos/verificar_autenticacao.py) contra somente `http://localhost:8091`. O resultado automatizado está em [autenticacao-navegador.json](autenticacao-navegador.json): **15 verificações aprovadas**, com 12 capturas novas. Foram comprovados cadastro sem login automático, login inválido com mensagem genérica, sessão após recarregar em 360/1280 px, logout e redirecionamento de rota privada, falha de rede recuperável, foco por teclado em cadastro/login nos dois tamanhos e catálogo/detalhes públicos. As capturas foram inspecionadas visualmente; não há overflow horizontal nas larguras testadas.

A verificação de e-mail duplicado foi manual, complementar ao roteiro: usando a conta fictícia criada pelo roteiro, um novo cadastro fictício com o mesmo e-mail retornou HTTP 409 e a mensagem `E-mail já cadastrado.`. A conta manual do usuário não foi consultada, reutilizada, alterada ou removida. As verificações de PostgreSQL são automatizadas; a duplicidade via formulário foi manual; as demais verificações de interface listadas acima são automatizadas pelo Playwright.

Pendências reais desta etapa: nenhuma no cadastro e autenticação cobertos pelos relatórios e evidências acima. Permanecem fora do escopo proteção contra abuso, calibração do custo BCrypt no ambiente final, HTTPS, confirmação/recuperação de senha e administração real. Não foram adicionadas funcionalidades, não foi encerrado o servidor em `8091` e os arquivos de compilação continuam fora do Git.

## Fontes consultadas

- Documentação oficial no tag [6.4.2 — formulário](https://github.com/spring-projects/spring-security/blob/6.4.2/docs/modules/ROOT/pages/servlet/authentication/passwords/form.adoc) e [armazenamento de senha](https://github.com/spring-projects/spring-security/blob/6.4.2/docs/modules/ROOT/pages/features/authentication/password-storage.adoc).
- [CSRF da linha 6.4](https://docs.spring.io/spring-security/reference/6.4-SNAPSHOT/servlet/exploits/csrf.html): token diferido em sessão, handler padrão XOR e renovação após autenticação/logout. Nenhuma dependência SNAPSHOT foi adicionada; os testes exercitam os artefatos gerenciados 6.4.2.
