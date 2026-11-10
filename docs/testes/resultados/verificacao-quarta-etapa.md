# Registro da quarta etapa — carrinho

Implementação conforme a [SPEC](../../../specs/11-SPEC-CARRINHO.md). Carrinho temporário em sessão, sem migração ou tabela. Não foram implementados checkout, pedidos, pagamento ou administração.

## Implementação

- [Serviço e contrato](../../../src/main/java/br/com/pit/cupcakes/carrinho) guardam apenas IDs e quantidades por sessão. Limites de 1–99 unidades por linha e 50 produtos diferentes; inclusões repetidas somam. Operações da mesma sessão são sincronizadas.
- Catálogo PostgreSQL/H2 é consultado a cada resposta. Preço e disponibilidade atuais determinam linhas e subtotal no servidor. Linha removida ou indisponível fica bloqueada e removível, sem entrar no subtotal. Entrada JSON com campos extras é rejeitada.
- [Segurança](../../../src/main/java/br/com/pit/cupcakes/config/SecurityConfig.java) libera somente rotas e tela de carrinho a visitantes, mantendo CSRF para mutações e regras de conta/admin. Login conserva os atributos da sessão enquanto renova seu ID; logout invalida a sessão. O prazo existente é 30 minutos.
- [Tela](../../../src/main/resources/static/paginas/compra/carrinho.html), [catálogo](../../../src/main/resources/static/assets/js/cliente/catalogo.js), [detalhes](../../../src/main/resources/static/assets/js/cliente/detalhes.js) e navegação usam JavaScript vanilla. Inclusões não são repetidas automaticamente após falha. O subtotal informa que exclui linhas bloqueadas e que os valores podem mudar.
- O [modelo de sessão](../../arquitetura/carrinho-em-sessao.md) registra o limite de instância única e o ciclo de vida. Não foi criada V4.

## Testes executados

Comando executado em 06/11/2026, com `JAVA_HOME` apontando para JDK 21.0.7:

```powershell
.\mvnw.cmd -q '-Dtest=CartApiTest,AuthenticationSecurityTest,AddressApiTest,ProductControllerTest' test
```

A execução conjunta concluiu **29 testes, 0 falhas, 0 erros, 0 ignorados**. Após acrescentar a verificação de novo login, executei somente `-Dtest=CartApiTest#keepsCartThroughLoginAndClearsItOnLogout`: **1 teste aprovado**. Essa execução focada substituiu o XML local de `CartApiTest`; os XML atuais, portanto, não somam 29, embora a execução conjunta tenha passado.

| Classe | Testes | Ambiente e alcance |
| --- | ---: | --- |
| CartApiTest | 7 | MockMvc, segurança real, persistência H2; vazio, inclusão, soma, concorrência, alteração, remoção, preço atual, bloqueio, limites, sessões, login, rotação, logout e CSRF |
| AuthenticationSecurityTest | 8 | Regressão de autenticação com repositório simulado |
| AddressApiTest | 7 | Regressão de endereços com H2 e ViaCEP controlado |
| ProductControllerTest | 7 | Regressão do catálogo com dependências simuladas |

O primeiro teste focado do carrinho encontrou uma fixture JPA com versão desatualizada após salvar preço. A fixture passou a usar a entidade retornada por `saveAndFlush`; a execução final acima aprovou todos os casos. `test-compile` também concluiu sem erros. O [roteiro Playwright](../casos/verificar_carrinho.py) passou por `--help` e `py_compile`, mas não foi executado contra servidor.

## Situação dos cenários

| Casos | Situação |
| --- | --- |
| CRT01–05 | Contratos essenciais aprovados no CartApiTest/H2, incluindo soma, concorrência, preço do servidor, limites e remoção idempotente. Verificação do navegador permanece pendente. |
| CRT06–08 | Bloqueio por indisponibilidade/remoção, preço atualizado e isolamento de sessões aprovados no CartApiTest/H2. Repetição no PostgreSQL pendente. |
| CRT09–11 | Carrinho preservado após login e troca de ID, limpeza após logout, CSRF e permissões aprovados no CartApiTest/H2. |
| CRT12 | Cliente não repete automaticamente POST e reconsulta o carrinho após erro; cenário de falha após processamento ainda não foi observado em navegador. |
| CRT13 | Roteiro preparado; navegador real, capturas, teclado e 360/1280 px pendentes de execução. |
| REG01 | Regressão focada de autenticação, endereços e catálogo aprovada em H2/simulações conforme a tabela; PostgreSQL pendente. |

## Pendências de integração real

`DB_TEST_USERNAME` e `DB_TEST_PASSWORD` não estavam presentes no processo desta execução. Por isso **CartPostgresIntegrationTest não foi executado**, e nenhum resultado PostgreSQL novo é declarado. A [classe de integração](../../../src/test/java/br/com/pit/cupcakes/carrinho/CartPostgresIntegrationTest.java) usa o guard existente antes de acessar a conexão, exige `current_database() = cupcakes_test`, cria somente seu produto fictício e remove apenas seu ID. O [servidor de navegador](../../../src/test/java/br/com/pit/cupcakes/carrinho/CartBrowserServer.java) aplica o mesmo guard, usa porta 8093 e cria fixture identificável, removida no encerramento normal.

Uma tentativa de iniciar Java pela ferramenta de depuração falhou no build. Após corrigir a única falha de compilação indicada pelo editor, a segunda tentativa também falhou sem diagnóstico adicional; `test-compile` via Maven com `JAVA_HOME` explícito passou. A porta 8093 não foi iniciada e nenhuma captura CRT foi produzida. O servidor anterior em 8092 não foi encerrado. Não há evidência de navegação real desta etapa.

Para executar localmente a parte pendente, forneça `DB_TEST_USERNAME` e `DB_TEST_PASSWORD` **somente no ambiente do processo**, para uma conta restrita ao banco `cupcakes_test`. O perfil já define a URL exata `jdbc:postgresql://localhost:5432/cupcakes_test`; o guard bloqueia outra URL, banco efetivo, perfil demo ou falta de credenciais. Com `JAVA_HOME` apontando ao JDK 21:

```powershell
.\mvnw.cmd '-Dtest=CartPostgresIntegrationTest' test
.\mvnw.cmd test-compile '-Dspring-boot.run.main-class=br.com.pit.cupcakes.carrinho.CartBrowserServer' spring-boot:test-run
C:/Python313/python.exe docs/testes/casos/verificar_carrinho.py --mode postgresql
```

Inicie o servidor em um terminal e rode o roteiro em outro. Inspecione as capturas CRT e registre os resultados executados. Não use `cupcakes`, não execute `Flyway clean` e não copie credenciais para arquivos ou evidências. Falta também observar CRT12 em navegador com a resposta perdida após processamento, distinguindo qualquer controle de rede dos endpoints reais.

Alterações prévias do README, do registro da primeira etapa e suas evidências foram preservadas. Não houve commit, push ou publicação.

## Atualização na rodada consolidada da quinta etapa (10/11/2026)

Conforme previsto na [SPEC de conclusão funcional](../../../specs/12-SPEC-CONCLUSAO-FUNCIONAL.md), as pendências acima foram executadas e fechadas na rodada única consolidada registrada na [verificação da quinta etapa](verificacao-quinta-etapa.md):

- **Integração PostgreSQL (`CartPostgresIntegrationTest`)**: executada contra `cupcakes_test` (PostgreSQL 18.6) e **aprovada** (`1` teste, `0` falhas, `0` erros).
- **Navegador real (`CRT01`, `CRT03`, `CRT08`, `CRT09`, `CRT13`)**: executados e **aprovados** em `http://localhost:8094` ([fluxo-completo-navegador.json](fluxo-completo-navegador.json)), incluindo inclusão pelo catálogo (`HU06_CRT01_adicionar-produto.png`), carrinho visitante (`HU06_CRT01_carrinho-visitante.png`), alteração de quantidade (`HU07_CRT03_quantidade.png`), recarga de sessão (`CRT08`), navegação por teclado e responsividade em `360px` e `1280px` (`HU06_CRT13_carrinho-360-teclado.png`, `HU06_CRT13_carrinho-1280-teclado.png`), remoção e estado vazio (`HU07_CRT03_removido.png`) e preservação do carrinho no login rumo ao checkout (`CHK01_checkout-apos-login-1280.png`).

