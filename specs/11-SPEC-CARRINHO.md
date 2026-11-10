# SPEC 006 Carrinho de compras

Versão 1 para implementação após revisão do responsável. Escopo: HU06/HU07 e integração de navegação com HU01/HU02/HU04/HU05. Não inclui checkout, pedidos, cálculo de frete no carrinho, pagamento ou administração.

## Objetivo

Visitante ou cliente adiciona cupcakes, altera quantidades, remove itens e consulta subtotal calculado pelo servidor com preços atuais do catálogo. Login preserva o carrinho da mesma sessão. Logout encerra sessão e limpa carrinho.

## Organização e compatibilidade

- Manter stack, estrutura de pastas, autenticação/CSRF, endereços e catálogo existentes. Sem atualização geral de dependências.
- Java no módulo carrinho, com controller, service e dto conforme necessidade. Não criar repository ou tabela sem uso: o carrinho desta versão é da sessão.
- Classes e identificadores em inglês nas convenções já adotadas; pacotes existentes e contrato público em português. Comentários curtos em português, sem banners ou separadores decorativos.
- Front vanilla simples; static/paginas/compra/carrinho.html, scripts cliente e utilitários compartilhados. Não mudar favicon ou polir identidade visual.

## Armazenamento e ciclo de vida

- HttpSession armazena apenas mapa de produtoId para quantidade, sem entidades JPA, preços fixados, senhas ou dados de outra conta. Sessões distintas têm carrinhos distintos.
- Recarregar página mantém carrinho na sessão. Expiração de 30 minutos e logout descartam carrinho; reinício do servidor pode descartá-lo. Registrar esse comportamento; não exigir persistência do carrinho entre reinícios.
- Sessão visitante preserva mapa após login e renovação segura do identificador. Não substituir proteção contra session fixation por sessão insegura.
- Logout invalida sessão e deve remover qualquer estado visual anterior do carrinho. Novo usuário no mesmo navegador não herda carrinho da sessão encerrada.
- Processar alterações concorrentes da mesma sessão de forma atômica, evitando perda de inclusão em requisições simultâneas. Declarar limite desta estratégia para instância única; implantação distribuída exigiria revisar sessão compartilhada.
- Nenhuma migração é necessária nesta fatia; banco continua sendo fonte do catálogo. Não adicionar V4 apenas para criar marco numérico.

## Quantidades, preços e itens indisponíveis

- Quantidade inteira de 1 a 99 por produto; no máximo 50 produtos distintos por sessão. São limites de aplicação, não informação de estoque.
- Adicionar produto repetido soma quantidade na mesma linha; ultrapassar limite retorna 400 sem alteração parcial. Alterar quantidade substitui valor; zero não significa remoção implícita.
- ID inexistente em inclusão: 404 PRODUTO_NAO_ENCONTRADO. Produto indisponível em inclusão: 409 PRODUTO_INDISPONIVEL. Quantidade inválida: 400 QUANTIDADE_INVALIDA. Excesso de linhas: 400 LIMITE_CARRINHO_ATINGIDO.
- Consultar no banco os produtos atuais, preferencialmente em lote. Calcular valores com BigDecimal no servidor; cliente nunca define preço, nome, disponibilidade ou subtotal.
- Subtotal de linha = preço atual × quantidade. Subtotal do carrinho soma somente linhas válidas/disponíveis. Valores JSON são strings decimais com duas casas; interface formata em reais.
- Se produto do carrinho ficar indisponível ou desaparecer, preservar a linha identificável para remoção e marcar bloqueada. Não ocultar silenciosamente o problema ou autorizar compra parcial automática.
- Linha de produto inexistente: produtoId, nome "Produto não encontrado", quantidade, disponivel=false, motivoBloqueio=PRODUTO_NAO_ENCONTRADO, precoUnitario=null e subtotal=null. Produto existente indisponível mantém nome/preço atuais, mas subtotal da linha=null e motivoBloqueio=PRODUTO_INDISPONIVEL. Valores bloqueados não entram no subtotal geral.
- Alterar quantidade de linha bloqueada retorna 409 ITEM_BLOQUEADO; remover continua possível.
- Mudança no preço do catálogo aparece na próxima consulta, com indicação permanente na interface: "Valores sujeitos à confirmação na revisão do pedido". Não manter preço antigo como garantia. Checkout futuro terá revalidação própria.
- Proibir campos adicionais como preco, subtotal, usuarioId ou perfil nas entradas de mutação; retornar 400 sem mutação. Preço adulterado no navegador não altera cálculo do servidor.

## Contrato HTTP

| Método e caminho | Entrada | Sucesso |
| --- | --- | --- |
| GET /api/carrinho | Nenhuma | 200 estado atual, incluindo vazio |
| POST /api/carrinho/itens | JSON produtoId e quantidade | 200 estado atualizado; inclusão soma |
| PUT /api/carrinho/itens/{produtoId} | JSON quantidade | 200 estado atualizado; substitui quantidade |
| DELETE /api/carrinho/itens/{produtoId} | ID inteiro positivo | 204; idempotente mesmo quando não está no carrinho |

PUT de produto válido ausente no carrinho retorna 404 ITEM_NAO_ENCONTRADO. IDs inválidos retornam 400 ID_INVALIDO. JSON inválido retorna 400; mídia incompatível 415; CSRF inválido 403; falha inesperada 500 genérico. Erros preservam codigo e mensagem e não expõem dados internos. DELETE inválido não deve limpar outro item.

Objeto de estado: itens (array em ordem crescente por produtoId), quantidadeTotal (soma das unidades inclusive bloqueadas), subtotal (soma monetária de linhas válidas), possuiItensBloqueados (boolean), vazio (boolean). Linha válida: produtoId, nome, imagemUrl opcional, quantidade, disponivel=true, motivoBloqueio=null, precoUnitario e subtotal como strings. Carrinho vazio retorna [], 0, "0.00", false e true respectivamente.

Retornar Cache-Control: no-store. Não expor identificador de sessão em URL ou JSON.

## Segurança e login

- Liberar os endpoints de carrinho descritos e a página de carrinho para visitantes, mantendo CSRF em POST/PUT/DELETE. Não tornar públicos /api/conta/** ou /api/admin/** para atingir isso.
- Utilizar cookie/sessão e helper de API já existentes; nunca persistir carrinho autoritativo ou credenciais em localStorage.
- Não repetir automaticamente POST após falha de rede: ele soma quantidade, e o primeiro envio pode ter sido processado. Reconsultar o estado após reconexão e permitir nova ação consciente.
- Serializar envios no front enquanto uma operação está pendente; tratar resposta HTTP não aprovada sem atualizar a tela como se tivesse sucesso.
- Após login, reobter CSRF, preservando itens e permissões. Permitir link de login originado no carrinho com token de retorno fixo e permitido (por exemplo retorno=carrinho). Nunca aceitar URL externa ou caminho arbitrário como redirecionamento.

## Interface

- Catálogo e detalhes ganham ação Adicionar ao carrinho em produto disponível. No detalhe, permitir quantidade; no catálogo, inclusão de uma unidade é suficiente. Indisponível tem rótulo e ação desabilitada.
- Link Carrinho na navegação, com contagem de unidades caso implementada, atualizada por resposta do servidor, sem consultas em loop.
- Página apresenta linhas, quantidades editáveis, remover, subtotal de cada linha válida e subtotal geral. Estado vazio tem link para catálogo. Não mostrar frete incluso nem total final de pedido.
- Linhas bloqueadas mostram o motivo e ação de remoção; subtotal deve indicar que exclui itens bloqueados quando houver.
- Visitante pode entrar na conta sem perder carrinho; cliente autenticado recebe identificação normal da sessão. Não criar botão Finalizar que simula compra concluída: checkout ainda não existe. Informar discretamente que esta etapa contém revisão do carrinho; não inserir detalhes técnicos do projeto na interface.
- Carregamento, erro recuperável, tentativa novamente em consultas e mensagens de limite acessíveis. Campos e ações por teclado, foco visível e layout 360/1280 px.
- Saída ou expiração de sessão não mostra dados de carrinho anterior como atuais; nova consulta apresenta estado da sessão atual.

## Testes previstos

| ID | Verificação | Resultado |
| --- | --- | --- |
| CRT01 | Carrinho vazio e primeira inclusão | Valores e estado conforme contrato |
| CRT02 | Produto repetido e inclusão concorrente na sessão | Uma linha, soma correta e sem perda de atualização |
| CRT03 | Alterar quantidade, remover e repetir remoção | Substituição correta e remoção idempotente |
| CRT04 | Quantidade fracionária/zero/negativa, limites e IDs inválidos | Erro e estado anterior preservado |
| CRT05 | Preço/campo extra adulterado | Rejeição sem mutação e totais sempre do banco |
| CRT06 | Produto indisponível/inexistente e mudança após inclusão | Bloqueio conforme contrato; remoção viável; subtotal exclui bloqueados |
| CRT07 | Preço atual muda no banco isolado | Próxima consulta usa o novo preço sem alterar migrações |
| CRT08 | Duas sessões diferentes | Nenhum compartilhamento de carrinho |
| CRT09 | Visitante adiciona, faz login e consulta novamente | Carrinho preservado e sessão renovada |
| CRT10 | Logout e novo login no mesmo navegador | Carrinho anterior não herdado; autorização preservada |
| CRT11 | Operações sem CSRF e rotas de conta/admin | CSRF rejeitado e regras privadas intactas |
| CRT12 | Rede falha após possível processamento | Não repetir inclusão automaticamente; tela reconcilia por consulta |
| CRT13 | Navegador real com banco isolado, teclado e 360/1280 | Fluxos e estados observados com capturas |
| REG01 | Catálogo, autenticação e endereços | Regressões pertinentes aprovadas |

Usar PostgreSQL cupcakes_test com proteções atuais; mudanças temporárias em produtos são exclusivas do banco isolado. Não alterar produtos de cupcakes para produzir cenários. Distinguir testes de lógica/simulação e os executados com banco e servidor reais. Não substituir testes reais por capturas de APIs simuladas sem declarar a limitação.

## Verificação e entrega

- Criar roteiro de navegador e servidor isolado se necessário; documentar porta exclusiva e main-class real. Para classe de teste, usar test-compile spring-boot:test-run com main-class explícita.
- Persistência do catálogo e da conta continua exigida; carrinho é temporário por decisão explícita e não tem teste de sobrevivência a reinício.
- Atualizar modelo/arquitetura indicando carrinho em sessão e registro da quarta etapa com resultados reais. Capturas sem data, por exemplo HU06_CRT01_adicionar-produto.png.
- Informar arquivos, comandos testados, contagens e pendências sem expor senhas. Não repetir indiscriminadamente toda suíte se a alteração e o aceite não exigirem.
- Não fazer commit, push ou publicação nesta tarefa. Não implementar checkout, pedido, pagamento ou administração antecipadamente.
