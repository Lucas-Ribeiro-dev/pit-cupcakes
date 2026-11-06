# PIT Cupcakes — catálogo, autenticação e endereços

Loja virtual acadêmica com catálogo público, detalhes e segunda etapa de cadastro, login, sessão e logout. A implementação de autenticação segue a [SPEC](specs/09-SPEC-AUTENTICACAO.md); os relatórios atuais registram sete testes PostgreSQL aprovados e a verificação real no navegador está documentada no [registro da segunda etapa](docs/testes/resultados/verificacao-segunda-etapa.md). A terceira etapa implementa endereços e cotação conforme a [SPEC de endereços e frete](specs/10-SPEC-ENDERECOS-FRETE.md), com resultados no [registro da terceira etapa](docs/testes/resultados/verificacao-terceira-etapa.md). Carrinho, checkout e administração ficam para etapas futuras. O sistema completo não está concluído.

Java 21, Spring Boot 3.4.2, Maven 3.9.9 pelo Wrapper, Spring Data JPA, Bean Validation, PostgreSQL, Flyway e HTML/CSS/JavaScript vanilla. A integração conferida usou PostgreSQL 18.6. Dependências e versões estão fixadas no [pom.xml](pom.xml) e nas [propriedades do Wrapper](.mvn/wrapper/maven-wrapper.properties).

## Preparação local

São necessários JDK 21 e PostgreSQL acessível em localhost:5432. Configure `JAVA_HOME` para o JDK instalado e inclua sua pasta `bin` no `PATH` deste terminal; não há caminho de uma máquina específica no projeto.

```powershell
java -version
.\mvnw.cmd -version
```

Os bancos `cupcakes` e `cupcakes_test` já existem no ambiente verificado. Em outro ambiente, crie-os uma única vez, com uma conta local autorizada:

```sql
CREATE DATABASE cupcakes;
CREATE DATABASE cupcakes_test;
```

A conexão de desenvolvimento usa somente `cupcakes`; a integração deve usar somente `cupcakes_test`. Não apague bancos, não execute Flyway clean e não altere migrações já aplicadas.

O Spring Boot lê variáveis do processo. Arquivos `.env` não são carregados automaticamente; [.env.example](.env.example) contém apenas valores fictícios. As variáveis de um terminal podem não estar disponíveis em outro.

No PowerShell, configure credenciais por entrada protegida, sem gravá-las no histórico:

```powershell
$databaseCredential = Get-Credential -Message 'Credenciais locais do banco cupcakes'
$env:DB_USERNAME = $databaseCredential.UserName
$env:DB_PASSWORD = $databaseCredential.GetNetworkCredential().Password
$env:DB_URL = 'jdbc:postgresql://localhost:5432/cupcakes'
```

## Executar catálogo e detalhes

Sem carga de demonstração:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'local'
.\mvnw.cmd spring-boot:run
```

Com carga explícita dos quatro produtos fictícios:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'local,demo'
.\mvnw.cmd spring-boot:run
```

O perfil `local` habilita Flyway e Hibernate com `ddl-auto: validate`. A [migração V1](src/main/resources/db/migration/V1__criar_tabela_produto.sql) cria a tabela `produto` e seu índice. O perfil `demo` insere quatro cupcakes somente se não houver produtos: três disponíveis, um indisponível e pelo menos um sem imagem. A prevenção de duplicação tem teste unitário aprovado; a verificação do reinício real no banco de desenvolvimento deve seguir o registro de evidências, sem presumir sua conclusão.

- [Catálogo](http://localhost:8080/index.html)
- [Detalhes de exemplo](http://localhost:8080/produto.html?id=1), se o ID existir na base.
- [API do catálogo](http://localhost:8080/api/produtos)

Para parar, use Ctrl+C somente no terminal dessa execução. Não encerre todos os processos Java.

## Testes automatizados

Unitários, camada web, proteção da integração e repositório H2, sem PostgreSQL externo:

```powershell
.\mvnw.cmd '-Dtest=ProductControllerTest,ProductServiceTest,ProductRepositoryTest,DemoDataLoaderTest,PostgresTestDatabaseGuardTest' test
```

Integração PostgreSQL, no terminal com credenciais próprias do banco de teste:

```powershell
$testDatabaseCredential = Get-Credential -Message 'Credenciais locais do banco cupcakes_test'
$env:DB_TEST_USERNAME = $testDatabaseCredential.UserName
$env:DB_TEST_PASSWORD = $testDatabaseCredential.GetNetworkCredential().Password
$env:DB_TEST_URL = 'jdbc:postgresql://localhost:5432/cupcakes_test'
.\mvnw.cmd '-Dtest=ProductPostgresIntegrationTest' test
```

O relatório histórico da primeira etapa confirma cinco aprovações de catálogo, sem falhas, erros ou ignorados. Os relatórios atuais do marco de autenticação acrescentam `AuthenticationPostgresIntegrationTest` e `AuthenticationRestartPostgresTest`, totalizando sete testes PostgreSQL aprovados; o detalhamento está no [registro da segunda etapa](docs/testes/resultados/verificacao-segunda-etapa.md). O perfil `test-pg` mantém Flyway habilitado, Hibernate `validate` e proíbe substituição por H2. A proteção exige a configuração autorizada e consulta `current_database()` nas conexões antes de permitir migrações e limpezas. Conexão ou configuração inválida causa erro de execução; os testes não são ignorados silenciosamente.

`./mvnw test` ou `.\mvnw.cmd test` inclui a integração e exige suas credenciais. Os testes H2 não comprovam comportamento PostgreSQL. Relatórios brutos ficam em `target/surefire-reports`, fora do Git; o [resumo sanitizado](docs/testes/resultados/testes-automatizados.json) registra contagens, casos e hashes dos relatórios conferidos, sem propriedades locais ou credenciais.

## Verificação do navegador e persistência

O [plano e resultados](docs/testes/resultados/plano-e-resultados.md) separa as execuções e relaciona capturas aos casos. O [registro da primeira etapa](docs/testes/resultados/verificacao-primeira-etapa.md) descreve ajustes e pendências. Há 11 cenários de navegador aprovados na execução já registrada; isso não representa uma nova execução nesta tarefa.

Para repetir a automação visual, são necessários Python com Playwright e Microsoft Edge instalado. O script preserva capturas existentes e salva as novas com outro nome:

```powershell
python -m pip install playwright
python docs/testes/casos/verificar_catalogo.py
```

O TC02 visual deve ser testado em ambiente isolado. Não esvazie `cupcakes` para simulá-lo.

Para registrar migração e produtos no desenvolvimento, use a [coleta somente de leitura](docs/testes/casos/verificar_persistencia.ps1) no terminal com `DB_USERNAME` e `DB_PASSWORD` já configurados. O script usa `psql` do PATH ou da instalação PostgreSQL padrão e grava snapshots sem credenciais:

```powershell
.\docs\testes\casos\verificar_persistencia.ps1 -Stage Before
$env:DB_URL = 'jdbc:postgresql://localhost:5432/cupcakes'
$env:SPRING_PROFILES_ACTIVE = 'local,demo'
.\mvnw.cmd spring-boot:run
```

Após iniciar, Ctrl+C apenas nessa execução e repita `.\mvnw.cmd spring-boot:run`. Após a segunda inicialização, Ctrl+C e execute:

```powershell
.\docs\testes\casos\verificar_persistencia.ps1 -Stage After
```

A comparação confere todos os campos dos produtos, IDs, quantidades e histórico Flyway. Não executa migrações ou limpeza; o banco é consultado em transação somente de leitura. A execução desse roteiro ainda deve ser comprovada pelos snapshots e pela confirmação do reinício; instruções não são evidências.

## Contrato e organização

`GET /api/produtos` retorna array ordenado por ID; o catálogo vazio retorna `[]`. `GET /api/produtos/{id}` retorna um produto, 400 para ID inválido, 404 para inexistente e 500 com mensagem genérica para falha inesperada. Campos públicos: `id`, `nome`, `descricao`, `preco` como string decimal com duas casas, `imagemUrl` e `disponivel`. Erros usam `codigo` e `mensagem`, sem detalhes internos.

- [SPEC técnica](specs/07-SPEC-CATALOGO.md)
- [Dicionário de dados](docs/banco-de-dados/dicionario-dados.md)
- [Código principal](src/main/), [testes](src/test/) e [evidências](docs/testes/evidencias/)

Builds, logs, segredos, configurações de IDE, contexto e prompts operacionais permanecem locais. Este marco não publica a aplicação nem cria repositório remoto.

## Cadastro, login e minha conta

- [Criar conta](http://localhost:8080/paginas/conta/cadastro.html): nome, e-mail, senha e confirmação somente na interface. O cadastro cria CLIENTE e não inicia sessão automaticamente.
- [Entrar](http://localhost:8080/paginas/conta/login.html): usa e-mail normalizado e senha literal, incluindo espaços. Não há recuperação de senha ou confirmação por e-mail nesta etapa.
- [Minha conta](http://localhost:8080/paginas/conta/minha-conta.html): nome/e-mail da sessão atual. Sair exige POST com CSRF; atualizar a conta após sair direciona ao login.

Spring Security 6.4.2 é gerenciado pelo Boot 3.4.2. O encoder delegante grava `{bcrypt}` com custo padrão 10. Sessão usa cookie HttpOnly, SameSite=Lax e inatividade de 30 minutos. Ative `prod` junto da configuração do ambiente HTTPS para exigir cookie Secure. Não use `prod` no HTTP local para os testes de navegador.

| Endpoint | Entrada | Sucesso |
| --- | --- | --- |
| GET `/api/autenticacao/csrf` | — | Token, nome do cabeçalho e parâmetro |
| POST `/api/usuarios` | JSON `nome`, `email`, `senha`; CSRF | 201; `id`, `nome`, `email`, `perfil` |
| POST `/api/autenticacao/login` | Form URL encoded `email`, `senha`; CSRF | 200; dados públicos e sessão |
| GET `/api/autenticacao/sessao` | Cookie de sessão | 200; somente usuário atual; 401 visitante |
| POST `/api/autenticacao/logout` | Cookie e CSRF | 204; invalida sessão e remove cookie |

Busque o token antes do primeiro POST e envie o cabeçalho informado. Busque outro após login/logout. O módulo [api.js](src/main/resources/static/assets/js/compartilhado/api.js) faz isso, recupera CSRF inválido sem repetir automaticamente o POST e usa apenas memória para o token. APIs retornam erros JSON com `codigo` e `mensagem`; não redirecionam fetch ao login. Conteúdo privado usa `Cache-Control: no-store`.

`/api/admin/**` exige ADMIN e `/api/conta/**` exige autenticação. Não há operações administrativas nem credencial padrão. Visitante recebe 401; cliente sem permissão recebe 403; ADMIN pode ultrapassar a regra e receber 404 em rota ainda inexistente.

Antes de disponibilização pública, continuam pendentes proteção contra abuso, calibração do custo de senha no ambiente final e validação HTTPS. Carrinho será retomado em outra etapa. Não há checkout nem alteração de perfil nesta entrega.

## Endereços e consulta de frete

[Meus endereços](http://localhost:8080/paginas/conta/enderecos.html) fica ligado à Minha conta. Permite vários endereços, sem padrão automático, revisão manual, edição e remoção confirmada. Entrada pela tela privada retorna ao endereço após login por destino interno fixo. Conflito de versão mantém o texto digitado para revisão.

ViaCEP é auxílio de preenchimento, com limite de espera e tratamento de falha. Não comprova o imóvel nem a disponibilidade de entrega; a [decisão DT01](docs/decisoes/DT01-viacep.md) descreve os limites. O frete é calculado no servidor pelo endereço salvo da conta. Brasília–DF é a referência, sem comparação literal do nome da cidade. Retirada é gratuita e não é escolhida automaticamente.

A [V3](src/main/resources/db/migration/V3__criar_endereco_faixa_frete.sql) cria endereços e faixas; V1/V2 permanecem intactas. Não insere cobertura ou taxas comerciais. **Faixas e valores finais estão pendentes**: sem configuração ativa, entrega responde 422 ENTREGA_NAO_CONFIGURADA. O [dicionário](docs/banco-de-dados/dicionario-dados.md) documenta restrições e configuração controlada. Nenhuma migração desta tarefa foi aplicada a cupcakes.

Todos os contratos abaixo exigem sessão; POST/PUT/DELETE exigem CSRF. Respostas privadas usam no-store. Campos e erros detalhados estão na [SPEC](specs/10-SPEC-ENDERECOS-FRETE.md).

| Endpoint | Entrada | Sucesso |
| --- | --- | --- |
| GET `/api/conta/enderecos` | — | Array ordenado por ID |
| GET `/api/conta/enderecos/{id}` | ID positivo | Endereço próprio |
| POST `/api/conta/enderecos` | destinatario, cep, logradouro, numero, complemento opcional, bairro, cidade, uf | 201; endereço e versao |
| PUT `/api/conta/enderecos/{id}` | Campos e versao atual | 200; endereço atualizado |
| DELETE `/api/conta/enderecos/{id}?versao=N` | Versão não negativa | 204 |
| GET `/api/conta/cep/{cep}` | Oito dígitos ou XXXXX-XXX | Campos disponíveis |
| POST `/api/conta/frete/cotacao` | modalidade ENTREGA com enderecoId, ou RETIRADA sem endereço | modalidade, valor decimal como string; enderecoId/versaoEndereco para entrega |

Endereço alheio e inexistente retornam o mesmo 404. Versão divergente retorna 409 ENDERECO_DESATUALIZADO. A API rejeita proprietário, ID, versão na criação e taxa enviados pelo cliente. GET de CEP distingue 404, 502 e 504; consulta indisponível não bloqueia salvamento manual válido.

Testes sem PostgreSQL externo, incluindo regressão:

```powershell
.\mvnw.cmd '-Dtest=AddressValidationTest,AddressApiTest,PostalCodeClientTest,AuthenticationSecurityTest,RegistrationServiceTest,ProductControllerTest,ProductServiceTest,ProductRepositoryTest,DemoDataLoaderTest,PostgresTestDatabaseGuardTest' test
```

Com DB_TEST_* configuradas localmente para cupcakes_test:

```powershell
.\mvnw.cmd '-Dtest=AddressShippingPostgresTest,AddressRestartPostgresTest,AuthenticationPostgresIntegrationTest,AuthenticationRestartPostgresTest,ProductPostgresIntegrationTest' test
```

As faixas 70000001–70000009 a 12.34 e demais valores dos testes são **fixtures fictícias**, sem significado comercial. Testes H2 verificam o motor e contratos; somente PostgreSQL pode comprovar a V3 e a exclusão GiST concorrente. O guard continua obrigatório e bloqueia falta de credenciais antes de Flyway.

Servidor de navegador isolado, com classe explícita e mecanismo spring-boot:test-run (porta 8092, preservando o servidor anterior):

```powershell
.\mvnw.cmd '-Dspring-boot.run.main-class=br.com.pit.cupcakes.endereco.AddressBrowserServer' spring-boot:test-run
python docs/testes/casos/verificar_enderecos.py
```

Esse servidor exige DB_TEST_* e ativa somente test-pg, com proteção de conexão efetiva e sem carga demo. A verificação utilizou o servidor já iniciado pelo responsável em 8092, sem reiniciá-lo. Os relatórios PostgreSQL atuais confirmam **13 testes aprovados**, conferidos sem repetição. O navegador real concluiu **18 cenários e 16 capturas**, com APIs reais de autenticação, endereços e frete; somente GET /api/conta/cep/* foi interceptado para controlar o ViaCEP. O roteiro cria conta fictícia exclusiva, remove seu endereço ao concluir e verifica logout. Detalhes no [registro da terceira etapa](docs/testes/resultados/verificacao-terceira-etapa.md) e no [resultado do navegador real](docs/testes/resultados/enderecos-navegador.json). Não use cupcakes para reproduzir.

Para verificar apenas comportamento da interface sem conexão de banco, o modo abaixo usa arquivos estáticos e APIs inteiramente simuladas. Relatório e capturas identificam a simulação, que não comprova persistência, autenticação real ou migrações:

```powershell
python docs/testes/casos/verificar_enderecos.py --simulate-api
```
