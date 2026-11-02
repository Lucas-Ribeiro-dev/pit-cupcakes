# SPEC 003 Primeira fatia de catálogo

Estado: primeira fatia implementada; resultados e pendências de verificação estão em [plano e resultados](../docs/testes/resultados/plano-e-resultados.md). Abrange HU01 e HU02 parcialmente: catálogo e detalhes; inclusão no carrinho será feita na fatia própria. Esta SPEC define o contrato técnico e não comprova execução de testes.

## Resultado esperado

Aplicação local executa com PostgreSQL e apresenta produtos persistidos em páginas vanilla. Não implementar cadastro, checkout ou administração nesta fatia. Não mostrar botão de compra com ação inexistente.

## Regras obrigatórias de organização e escrita

- Respeitar a estrutura existente. Antes de editar, ler instruções locais e identificar arquivos vazios, arquivos preenchidos e alterações do responsável. Não substituir projeto inteiro por saída de gerador.
- Pacote raiz: br.com.pit.cupcakes. Preservar módulos existentes em português, como catalogo, config e compartilhado; não renomear pastas apenas por preferência.
- Novas classes, interfaces, records, métodos, variáveis e atributos Java/JavaScript usam nomes descritivos em inglês. Classes/interfaces/records em UpperCamelCase (ProductController, ProductService, ProductRepository, Product, ProductResponse); métodos e variáveis em lowerCamelCase; constantes em UPPER_SNAKE_CASE. Não misturar nomes de classes em português e inglês para a mesma convenção. Pacotes ficam em minúsculas e seguem a organização existente.
- Rotas HTTP e campos JSON permanecem exatamente em português conforme o contrato desta SPEC. Nomes internos em inglês não autorizam mudar o contrato público. Usar mapeamento explícito quando necessário. Banco desta fatia usa tabela produto e colunas definidas nesta SPEC.
- Todo comentário escrito no código deve estar em português, ser curto e explicar uma decisão ou razão útil. Não comentar código óbvio nem inserir comentários de preenchimento.
- Proibidos banners decorativos de comentários: linhas de sinais de igual, hífens ou asteriscos, molduras, títulos em ASCII art, emojis ou separadores ornamentais. A regra vale para Java, JavaScript, HTML, CSS, SQL e configurações.
- Textos da interface, mensagens de validação, README e explicações ao responsável ficam em português. Nomes oficiais de bibliotecas e comandos não são traduzidos.
- Alterar a estrutura apenas por necessidade técnica concreta. Informar o motivo e o caminho afetado antes da alteração; se implicar mover conteúdo preenchido, mudar arquitetura ou descartar trabalho, aguardar o responsável. Ajustes mecânicos de inicialização sem perda de conteúdo podem ser feitos e relatados.
- Conferir existência dos erros anteriormente encontrados: ,gitignore em vez de .gitignore; mcnw.cmd em vez de mvnw.cmd; src/main/test em vez de src/test; static/páginas em vez de static/paginas. Não presumir que ainda existam nem sobrescrever arquivos conflitantes.
- pom.xml, mvnw, mvnw.cmd e a estrutura .mvn precisam ser válidos, não arquivos vazios. Gerar Maven Wrapper por mecanismo oficial, preservando o projeto existente. Nunca baixar e executar scripts de fonte desconhecida.

## Tecnologias da primeira implementação

Java 21, Spring Boot estável, Maven Wrapper, Spring Web MVC, Spring Data JPA, Bean Validation, PostgreSQL Driver, Flyway e infraestrutura de testes compatível. Conferir a versão e coordenadas atuais no Spring Initializr e fixá-las no projeto; nenhuma versão snapshot. Registrar versão exata em README. Spring Security será configurado na fatia de identidade; nesta fatia não há endpoints mutáveis ou dados privados. Não criar configuração que desative segurança global para reutilização futura.

Front-end: HTML, CSS e JavaScript vanilla em recursos estáticos da mesma aplicação. Sem Thymeleaf. Serviços e persistência separados dos controllers. Banco configurado por ambiente, sem senha real no repositório. Incluir exemplo de configuração e instruções de preparação no README.

## Localização dos arquivos

```text
src/main/java/br/com/pit/cupcakes/
  CupcakesApplication.java
  catalogo/
    controller/ProductController.java
    service/ProductService.java
    repository/ProductRepository.java
    entity/Product.java
    dto/ProductResponse.java
  compartilhado/exception/
src/main/resources/
  application.yml
  application-local.yml
  db/migration/
  static/
    index.html
    produto.html
    assets/css/
    assets/js/compartilhado/
    assets/js/cliente/
    assets/img/
src/test/java/br/com/pit/cupcakes/
src/test/resources/
```

docs/testes recebe resultados e evidências efetivos; docs/banco-de-dados recebe o dicionário desta fatia. specs armazena especificações; operacional guarda contexto e prompts, separados dos documentos acadêmicos. Não criar diretório templates. Manter demais pastas existentes mesmo que ainda vazias. Não implementar páginas administrativas agora.

Usar perfis local, demo e test com finalidades documentadas. Se já existir application-test.yml fora dos recursos de teste, verificar duplicidade antes de organizar. Carregar dados de demonstração somente quando demo for explicitamente ativado junto ao ambiente escolhido. Documentar exatamente como fornecer variáveis: Spring Boot não lê arquivo .env automaticamente. Não adicionar carregamento de .env sem necessidade.

## Persistência

Criar somente produto, conforme modelo de dados: id, nome, descricao, preco, imagem_caminho opcional, disponivel e versao. Restrições no banco: preço positivo e campos obrigatórios; usar Flyway e validação de mapeamento, sem geração destrutiva de esquema.

Dados de demonstração: quatro cupcakes com preços em reais, sendo três disponíveis e um indisponível, pelo menos um sem imagem. Identificar dados como fictícios e carregar somente por rotina/perfil de demonstração documentado. Não inserir automaticamente novos registros a cada inicialização nem colocar dados de teste em migração de esquema de produção.

## Contrato HTTP

| Requisição | Sucesso | Falhas |
| --- | --- | --- |
| GET /api/produtos | 200 com array de produtos, ordenado por id crescente; vazio é [] | 500 com mensagem genérica, sem stack trace |
| GET /api/produtos/{id} | 200 com um produto | 400 se id não é inteiro positivo; 404 se não existe; 500 para falha inesperada |

Objeto público de produto: id, nome, descricao, preco, imagemUrl opcional e disponivel. Preço no JSON é string decimal com duas casas, por exemplo "12.50"; exibição em BRL no navegador. Não expor entidade JPA diretamente nem atributos de controle interno.

Erro tem campos codigo e mensagem; por exemplo PRODUTO_NAO_ENCONTRADO. Log interno pode conter diagnóstico técnico, mas resposta pública não contém consulta SQL, credencial ou stack trace. Definir códigos estáveis para ID_INVALIDO e ERRO_INTERNO.

## Telas

- /index.html: título descritivo da loja, catálogo em cartões com nome, imagem ou substituta, preço, rótulo textual de disponibilidade e link para detalhes.
- /produto.html?id=ID: nome, descrição, imagem, preço, disponibilidade e link para voltar.
- Em ambas: estado de carregamento, mensagem de falha com ação de tentar novamente e mensagem adequada para vazio/não encontrado. Não confundir erro de conexão com catálogo vazio.
- Em detalhes, validar parâmetro antes da chamada; não chamar API com id ausente, não numérico ou não positivo.
- Consultar produtos via fetch; usar textContent ou mecanismo equivalente seguro para textos recebidos. Sem dados do catálogo fixados no JavaScript como substituto da consulta real.
- Acessibilidade básica: HTML semântico, foco visível, links utilizáveis por teclado, texto alternativo e indicação de disponibilidade que não dependa só da cor.
- Layout simples sem rolagem horizontal em 360 e 1280 pixels. Sem animações ou identidade visual elaborada nesta fatia.

## Casos de verificação previstos

| ID | Verificação | Resultado esperado |
| --- | --- | --- |
| TC01 | Consultar catálogo populado | 200 e produtos correspondentes ao PostgreSQL, sem campos internos |
| TC02 | Consultar catálogo sem produtos em banco isolado | 200 com []; tela informa catálogo vazio |
| TC03 | Consultar ID existente | Produto correto e preço formatado |
| TC04 | Consultar ID positivo inexistente | 404 e mensagem de produto não encontrado |
| TC05 | Consultar ID inválido na API e na página | API retorna 400; página apresenta mensagem sem requisição inválida |
| TC06 | Mostrar produto indisponível e produto sem imagem | Rótulo correto e imagem substituta; nenhuma compra fictícia |
| TC07 | Simular falha de rede | Mensagem de erro e tentativa novamente; não mostrar catálogo vazio |
| TC08 | Reiniciar aplicação com mesmo banco | Dados preservados e rotina de demonstração não duplica registros |
| TC09 | Navegar por teclado e conferir 360/1280 px | Conteúdo utilizável e sem rolagem horizontal |

Automatizar contrato HTTP e persistência com PostgreSQL de teste isolado. Usar Testcontainers se Docker estiver disponível; caso contrário, fornecer configuração de banco de teste separado e instruções. Nunca limpar banco de trabalho para comprovar TC02. Cenários visuais e de rede podem ser manuais com evidência. Registrar o que realmente foi executado; testes não executados permanecem pendentes.

## Critério de aceite da fatia

Responsável consegue preparar o banco e executar o projeto pelas instruções; migração cria a tabela; catálogo e detalhes usam dados persistidos; casos acima são verificados e pendências são explícitas. Código e evidências devem ser revisados antes de marcar HU01/HU02 como concluídas integralmente.
