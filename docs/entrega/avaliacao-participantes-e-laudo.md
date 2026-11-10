# Avaliações recebidas e laudo de qualidade

## Registro dos relatos

Relatos e datas informados pelo responsável. As verificações técnicas abaixo referem-se ao estado atual do projeto; não estabelecem a versão usada em cada avaliação nem a data de uma correção. As imagens produzidas pela equipe são evidências técnicas, não capturas fornecidas pelos participantes.

| Registro | Avaliador (sem identificação nominal no repositório público) | Data informada | Síntese do apontamento |
| --- | --- | --- | --- |
| AV01 | Participante 1 | 04/11/2026 | Confirmação de inclusão no carrinho pouco perceptível; não identificou a recusa simulada |
| AV02 | Participante 2 | 06/11/2026 | Teclado alfanumérico no campo de CEP em aparelho móvel |
| AV03 | Participante 3 | 07/11/2026 | Sugestão de estimativa de entrega; sem falha funcional relatada |
| AV04 | Participante 4 | 09/11/2026 | Não conseguiu provocar recusa; cancelamento pelo cliente não testado |
| AV05 | Participante 5 | 10/11/2026 | Sugestão de filtros; não identificou a recusa simulada |

Os cinco formulários individuais no documento de entrega preservam o conteúdo recebido. Não atribuir notas, duração, taxa de conclusão ou respostas adicionais não coletadas. Não converter um cenário descrito como não testado em cenário aprovado.

## Pendências remanescentes das avaliações

1. Organizar os relatos recebidos nos cinco blocos do formulário. O responsável informou já tê-los transcrito no DOC; conferir se este arquivo e o DOC coincidem. Não manter a afirmação de que todos os formulários ainda estão em branco.
2. Executar e registrar retestes dos apontamentos AV01, AV02 e AV04 na versão atual, com referência ao commit e evidência própria. Caso uma falha não se reproduza, registrar esse resultado em vez de inventar correção.
3. Validar especificamente o teclado do CEP em dispositivo móvel real para concluir AV02. inputmode no código e viewport de 360 px não comprovam o teclado nativo do aparelho.
4. Consolidar o laudo após a rodada final, substituindo somente pendências resolvidas por resultados efetivamente observados.
5. Concluir configuração pública de demonstração, hospedagem gratuita HTTPS e verificações no ambiente publicado, conforme o complemento do manual.

## Laudo de Qualidade

O sistema PIT Cupcakes foi submetido a testes de catálogo, conta, autenticação, endereços, carrinho, frete, checkout, pagamento simulado, consulta de pedidos e administração. A verificação utilizou testes automatizados de regras e contratos, integração com PostgreSQL isolado e execução de fluxos em navegador. Os relatos dos cinco avaliadores foram organizados como observações de uso e sugestões de melhoria.

Na conferência dos relatórios atuais, foram identificados 79 testes automatizados aprovados, sem falhas, erros ou testes ignorados, distribuídos entre 64 testes de API/regras/regressão e 15 testes de integração PostgreSQL. As execuções são distintas; esse total não representa cobertura integral do código. O registro de navegador informa 21 cenários aprovados e 20 capturas. O arquivo estruturado ainda indica execução parcial; sua coerência deve ser revisada antes de declarar encerramento irrestrito da rodada.

Os registros da integração descrevem correções em três problemas do ambiente de teste: divergência entre parâmetros documentados e aceitos pelo roteiro PowerShell, interferência de variáveis administrativas na inicialização do perfil de testes e resíduos de dados fictícios que causavam conflitos de chave estrangeira ou sobreposição de faixas. Foram registrados ajuste dos parâmetros do roteiro, restrição do provisionamento administrativo nos perfis de teste e organização da limpeza de fixtures em banco isolado. A rodada posterior registra os 15 testes PostgreSQL aprovados. Essas ocorrências pertencem ao processo de teste e não são atribuídas aos avaliadores.

Quanto ao carrinho, os avaliadores solicitaram confirmação de inclusão mais perceptível. A versão atual apresenta a mensagem “Produto adicionado ao carrinho”, documentada em captura própria. Isso comprova a presença do retorno visual atual, mas não determina se ele foi introduzido depois da avaliação. A rodada final verificará clareza, foco e leitura dessa mensagem e registrará eventual melhoria adicional.

No cadastro de endereço, foi relatada abertura de teclado alfanumérico no celular. O código atual declara inputmode="numeric" para CEP. A conclusão desse apontamento depende de verificação em aparelho real; a declaração do atributo e a simulação de largura móvel não substituem a observação do teclado. Caso o comportamento seja confirmado como inadequado, serão aplicados os ajustes necessários e registrado o reteste.

No pagamento, parte dos avaliadores não identificou como selecionar recusa. A versão atual dispõe de opções explícitas de aprovação e recusa, sem campos de cartão. A evidência técnica de recusa mostra o pedido registrado como não autorizado e o carrinho preservado. O reteste final verificará a visibilidade da escolha e a distinção entre os resultados. Não se atribui a uma alteração posterior a existência dessas opções sem registro correspondente.

As sugestões de filtros por categorias e estimativa de prazo de entrega foram classificadas como melhorias futuras. Não serão apresentadas como implementadas nesta entrega sem desenvolvimento e validação. O cancelamento pelo cliente não integra o escopo atual; o cancelamento disponível pertence ao fluxo administrativo e respeita os estados permitidos do pedido.

O parecer atual é favorável ao funcionamento local dos fluxos cobertos pelos testes registrados, com ressalvas para os retestes de usabilidade, coerência do registro de navegador e validação da publicação HTTPS. O pagamento permanece simulado e o frete utiliza parâmetros acadêmicos de demonstração. O encerramento do laudo exige preencher os resultados da rodada final e registrar as pendências que efetivamente permanecerem.

## Evidências técnicas já disponíveis

Caminhos relativos à raiz do repositório; conferir os arquivos ao copiar este documento para docs/entrega.

| ID | Evidência | O que sustenta |
| --- | --- | --- |
| EV01 | docs/testes/evidencias/HU06_CRT01_adicionar-produto.png | Mensagem atual de inclusão no carrinho |
| EV02 | docs/testes/evidencias/CHK03_pagamento-recusado.png | Recusa simulada, pedido não autorizado e preservação do carrinho |
| EV03 | docs/testes/evidencias/CHK04_pagamento-aprovado.png | Resultado aprovado e confirmação do pedido |
| EV04 | docs/testes/evidencias/CHK02_revisao-entrega.png | Revisão de entrega e composição de valores |
| EV05 | docs/testes/evidencias/PED02_detalhe-pedido.png | Dados do pedido registrado |
| EV06 | docs/testes/evidencias/ADM01_cliente-acesso-negado.png | Restrição da área administrativa ao cliente |
| EV07 | docs/testes/evidencias/ADM03_admin-atendimento-1280.png | Fluxo administrativo de atendimento |
| EV08 | docs/testes/evidencias/PED03_acompanhamento-concluido.png | Consulta do estado final pelo cliente |
| EV09 | docs/testes/resultados/persistencia-antes.json e persistencia-depois.json | Comparação de produtos/histórico após reinício do catálogo |
| EV10 | docs/testes/resultados/verificacao-quinta-etapa.md e relatórios Surefire conferidos | Resultados técnicos de integração e problemas de teste registrados |

## Registro do reteste final a preencher pela execução

Para cada AV: versão/commit, cenário, esperado, obtido, classificação (reproduzido, não reproduzido, melhoria ou fora do escopo), alteração efetiva quando houver, evidência e resultado do reteste. Para AV02, registrar dispositivo e navegador reais; se não disponível, manter a pendência. Não preencher os resultados por antecipação.
