# PIT Cupcakes — catálogo e detalhes

Primeiro marco de uma loja virtual acadêmica: catálogo público e página de detalhes de produtos. HU01 e HU02 estão atendidas parcialmente; cadastro, login, carrinho, checkout e administração ficam fora desta entrega. O sistema completo não está concluído.

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

O comando da integração foi executado pelo responsável; os relatórios atuais confirmam cinco aprovações, sem falhas, erros ou ignorados. O perfil `test-pg` mantém Flyway habilitado, Hibernate `validate` e proíbe substituição por H2. A proteção exige a configuração autorizada e consulta `current_database()` nas conexões antes de permitir migrações e limpezas. Conexão ou configuração inválida causa erro de execução; os testes não são ignorados silenciosamente.

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
