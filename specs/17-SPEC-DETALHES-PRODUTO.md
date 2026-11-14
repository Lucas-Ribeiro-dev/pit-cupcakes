# SPEC UI 003 — Detalhes do produto

## Objetivo

Refinar a página de detalhes existente, preservando as funcionalidades e a identidade aprovada do PIT Cupcakes. A tela deve apresentar o produto com clareza e tornar a seleção de quantidade e a inclusão no carrinho visualmente coerentes com o restante da loja.

Esta entrega é somente uma especificação. Não inclui código nem comprovação de implementação. Usar como referência a captura de detalhes enviada pelo responsável e as SPECs 15 e 16. Não redesenhar o catálogo ou a identidade da loja nesta etapa.

## Prévia visual de referência

![Composição proposta para os detalhes do produto](design-system/product-detail-preview.png)

Prévia criada por geração de imagem com a página inicial aprovada e a tela atual como referências. Serve para avaliar composição e consistência estética; não é captura da aplicação nem comprova fontes, cores ou comportamento implementados. Na implementação, os hex, arquivos oficiais de fonte, tamanhos e controles acessíveis definidos nas SPECs prevalecem sobre aproximações da imagem. Manter cores sólidas, sem reproduzir eventual textura, sombra ou variação de cor introduzida pela geração.

## Problemas visuais observados

- O campo de quantidade ocupa toda a coluna, com borda, altura e tipografia padrão do navegador.
- A imagem domina o bloco enquanto o conteúdo da direita fica concentrado na parte superior.
- O estado de disponibilidade usa verde, destoando do tratamento Raspberry definido para o catálogo.
- O preço pode ganhar hierarquia e respiro, separando melhor apresentação e ação de compra.

O espaço em branco não precisa ser totalmente preenchido. Corrigir proporções e alinhamento, sem adicionar informações fictícias ou funcionalidades para ocupar espaço.

## Direção visual obrigatória

| Papel | Valor |
| --- | --- |
| Principal | Raspberry Pink #BC164D |
| Superfície suave | Pale Pink #FFD0DF |
| Acento pequeno | Butter Yellow #FFE164 |
| Fundo da página | #FFF9FB |
| Fundo do painel | #FFFFFF |
| Texto principal | #301523 |
| Texto secundário | #725763 |
| Título e preço | Lilita One, regular 400 |
| Descrição, rótulos e controles | Varela Round, regular 400 |

Não usar amarelo em grandes áreas. Não adicionar gradientes, animações de entrada, sombras fortes ou elementos de marketing não solicitados.

## Composição desktop

Manter o cabeçalho compartilhado da loja, com conta e carrinho na disposição aprovada. O recorte de referência não demonstra o cabeçalho inteiro e não autoriza sua remoção.

Centralizar o conteúdo em largura máxima aproximada de 1120–1200 px. Antes do painel, posicionar o link “Voltar ao catálogo”, Raspberry, com seta discreta e área de interação confortável.

Painel branco com borda rosa suave, cantos de aproximadamente 24 px e preenchimento de 24–32 px. Organizar duas colunas com intervalo de 32–48 px:

- Imagem: aproximadamente 48% da área útil, proporção próxima de quadrada, altura de referência de 420–480 px em desktop amplo.
- Informações: aproximadamente 52%, com alinhamento vertical central em relação à imagem quando o conteúdo for curto. Se o conteúdo crescer, permitir expansão natural do painel.

Não fixar altura de modo que nomes, descrição, mensagens ou controles sejam cortados. Evitar a grande área vazia sob a ação de compra vista no estado atual.

## Imagem

Preservar o recurso real do produto, suas proporções e a imagem substituta existente quando necessário. Não recolorir fotos, distorcer a ilustração ou inventar foto de produto ausente.

Área de fundo suave, cantos de aproximadamente 16–20 px e imagem centralizada. A imagem nunca invade a coluna de informações. Não adicionar galeria, zoom ou miniaturas nesta etapa.

Texto alternativo apropriado ao recurso. Imagem substituta identificada sem fazer parecer que uma foto real foi carregada.

## Informações do produto

Ordem visual:

1. Disponibilidade em selo discreto.
2. Nome completo do produto.
3. Descrição real.
4. Preço unitário.
5. Separação visual e área de compra.

### Disponibilidade

Disponível: selo com fundo Pale Pink e texto Raspberry, podendo conter pequeno ponto decorativo. Texto “Disponível”.

Indisponível: tratamento neutro e texto explícito “Indisponível no momento”. Não usar cor como única indicação. Não criar indicação de estoque em unidades.

### Nome e descrição

Nome em Lilita One e Raspberry, aproximadamente 32–36 px em desktop e 28–30 px em celular. Permitir quebra natural de nomes longos, sem reticências ou perda de informação.

Descrição em Varela Round, 16 px, entrelinha aproximada de 1,6, cor secundária. Respiro de 16 px após o título e largura de leitura confortável. Não inserir ingredientes, peso, validade, informações de alergênicos ou alegações que não estejam cadastradas e verificadas.

### Preço

Apresentar o preço real no formato brasileiro, em Lilita One Raspberry, aproximadamente 36 px em desktop e 32 px em celular. Associar o texto “por unidade” em Varela Round menor e cor secundária.

Não exibir preço riscado, desconto, subtotal adicional ou informação promocional nesta etapa. A cotação e os totais continuam nos fluxos funcionais existentes.

## Área de compra

Separar visualmente da descrição por espaço de 24–32 px e, se útil, uma linha rosa discreta. Evitar um segundo card dentro do painel.

Rótulo “Quantidade” acima de um seletor compacto. Em desktop, alinhar o seletor e o botão de inclusão lado a lado, com cerca de 16 px entre eles. O seletor ocupa aproximadamente 140–156 px; o botão usa o espaço restante.

### Seletor de quantidade

Compor um único controle com botão de diminuir, campo numérico central e botão de aumentar. Altura aproximada de 48 px, cantos de 12 px e borda com contraste suficiente. Cor e tipografia da interface, sem aparência crua de campo padrão.

- Manter entrada direta pelo teclado; não tornar a quantidade apenas texto entre dois botões.
- Valor inicial e limites seguem as regras reais já existentes. Não criar novo máximo arbitrário.
- Não permitir quantidade menor que o mínimo funcional. No mínimo, desabilitar a ação de diminuir quando aplicável.
- Rejeitar valores vazios ou inválidos na submissão, com mensagem clara. Não alterar silenciosamente uma quantidade inválida para um valor diferente do solicitado.
- Botões com nomes acessíveis “Diminuir quantidade” e “Aumentar quantidade”.
- Rótulo associado ao campo e teclado numérico apropriado em celular.
- Garantir áreas de interação de pelo menos 44 × 44 px.

### Inclusão no carrinho

Botão principal Raspberry com texto branco “Adicionar ao carrinho”, altura mínima aproximada de 48 px e cantos de 12 px. Texto de pelo menos 16 px sempre que o espaço permitir.

Enviar produto e quantidade selecionados ao fluxo real existente. Não substituir a integração por uma simulação visual. Durante o processamento, indicar “Adicionando…” e impedir submissão duplicada enquanto a operação estiver em andamento.

Após sucesso, mostrar confirmação perceptível junto da área de compra e ação “Ver carrinho”. Atualizar o contador real do cabeçalho usando a convenção existente. Não deslocar o foco automaticamente para a mensagem. Usar anúncio acessível apropriado.

Após falha, apresentar explicação útil, preservar a seleção quando apropriado e permitir nova tentativa. Não exibir confirmação ou incrementar contador sem sucesso real.

Quando indisponível, bloquear inclusão e ações de quantidade conforme o fluxo real; manter descrição, preço e acesso ao catálogo. Se a disponibilidade mudar durante a operação, respeitar a resposta do servidor e atualizar a apresentação.

## Celular e telas intermediárias

Empilhar imagem e informações quando as duas colunas deixarem de ter espaço confortável; referência inicial de transição em torno de 768 px, ajustável pela necessidade do conteúdo.

Em celular:

- Manter pelo menos 16 px de margem lateral e painel com preenchimento de 20–24 px.
- Reduzir a área da imagem para aproximadamente 260–320 px de altura, sem esconder conteúdo essencial.
- Apresentar imagem antes das informações e preservar a hierarquia de nome, descrição e preço.
- Posicionar o seletor abaixo do rótulo e o botão principal em uma linha própria, com largura total.
- Não fixar botão flutuante ou barra de compra sobre o conteúdo nesta etapa.
- Manter carrinho e navegação do cabeçalho totalmente acessíveis.

## Estados e preservação funcional

Manter carregamento, produto inexistente, identificador inválido, falha de rede e recuperação previstos na aplicação. Cada estado deve receber o tratamento visual do design system, sem expor mensagens técnicas ao usuário.

Preservar regras de sessão e autenticação. Se o carrinho atual aceitar visitante, manter esse comportamento; a mudança visual não antecipa a obrigatoriedade de cadastro para esta tela.

Não mudar rotas, contratos de API, limites de quantidade, preços, cálculos, banco ou regras de disponibilidade nesta etapa. Preservar seletores e atributos usados pela integração e pelos testes, ou adaptar seus consumidores quando a mudança de marcação for indispensável.

## Aceite e evidências

| ID | Verificação | Resultado esperado |
| --- | --- | --- |
| DET-01 | Comparação visual | Identidade coerente com catálogo; proporções equilibradas e sem o campo padrão ocupando a coluna inteira |
| DET-02 | Nome e descrição extensos | Texto completo, sem sobreposição, truncamento ou painel cortado |
| DET-03 | Alterar quantidade | Aumentar, diminuir e digitar funcionam conforme os limites existentes |
| DET-04 | Quantidade inválida | Validação clara e ausência de inclusão indevida |
| DET-05 | Adicionar disponível | Quantidade correta enviada; confirmação e contador refletem sucesso real |
| DET-06 | Indisponível | Inclusão bloqueada e informação textual compreensível |
| DET-07 | Falha ao adicionar | Sem falso sucesso; seleção preservada quando aplicável e nova tentativa possível |
| DET-08 | Imagem ausente | Substituta existente preservada, com proporções e texto alternativo adequados |
| DET-09 | Navegação | Voltar, conta e carrinho mantêm os destinos e o estado corretos |
| DET-10 | Teclado e zoom | Foco visível, controles nomeados e conteúdo utilizável a 200% |
| DET-11 | Larguras | Verificar 360, 390, 768 e 1440 px sem rolagem horizontal |

Coletar capturas reais da página implementada em desktop e celular, do seletor e da confirmação de inclusão. Registrar o que foi efetivamente executado. Capturas de protótipo não comprovam integração funcional.

Executar regressão de navegador e testes pertinentes aos controles e fluxos afetados. Reutilizar os roteiros existentes; não repetir testes de banco sem relação com a alteração apenas por mudança de estilo. Usar o ambiente de testes protegido nos cenários que criam ou alteram dados.

## Orientações à IA implementadora

Respeitar as pastas e tecnologias existentes. Nomes em inglês conforme convenções da linguagem. Comentários necessários em português, curtos, sem banners ou linhas ornamentadas.

Entregar resumo de arquivos alterados, verificações reais, capturas e pendências. Não fazer commit, push, publicação ou limpeza de skills e SPECs nesta tarefa.
