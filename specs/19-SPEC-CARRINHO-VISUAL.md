# SPEC UI 005 — Revitalização visual do carrinho

## Objetivo

Aplicar ao carrinho a identidade aprovada da página inicial, detalhes de produto e autenticação. Substituir a aparência de controles padrão do navegador por uma composição clara de itens e resumo, sem alterar regras de negócio.

Referência do estado atual: captura de carrinho enviada pelo responsável, com nome, preço unitário, subtotal do item, campo de quantidade, ações de alteração e remoção, subtotal do carrinho e acesso à revisão.

Entrega desta colaboração: somente especificação. Não inclui código ou imagem de implementação. Usar as SPECs 15 a 18 como base visual. O recorte enviado não comprova ausência de cabeçalho ou rodapé; preservar os componentes compartilhados existentes.

## Prévia visual de referência

![Composição proposta para o carrinho](design-system/cart-preview.png)

Imagem gerada como referência estética do estado de visitante com um item e quantidade 1. Não é captura da aplicação nem evidência de teste. O botão principal muda conforme o estado de autenticação descrito abaixo. A miniatura é condicional à disponibilidade dos dados existentes; não exige novo contrato. Cores sólidas, arquivos oficiais de fonte e medidas da SPEC prevalecem sobre aproximações, texturas ou variações introduzidas pela geração.

## Identidade obrigatória

| Uso | Definição |
| --- | --- |
| Marca, títulos, valores e ações principais | Raspberry Pink #BC164D |
| Superfícies suaves e destaques secundários | Pale Pink #FFD0DF |
| Pequeno detalhe da marca | Butter Yellow #FFE164 |
| Fundo da página | #FFF9FB |
| Painéis e campos | Branco #FFFFFF |
| Texto principal | #301523 |
| Texto secundário | #725763 |
| Título da página, título do resumo e valores em destaque | Lilita One, regular 400 |
| Nomes dos produtos, rótulos, instruções e controles | Varela Round, regular 400 |

Não usar botões cinza padrão do navegador nem links azuis ou roxos por falta de estilo. Evitar grandes blocos amarelos, gradientes, sombras fortes e decoração que concorra com os itens.

## Composição da página

Conteúdo centralizado, com largura máxima aproximada de 1120–1200 px. Margens laterais mínimas de 16 px em celular. Alinhar retorno, título e conteúdo ao mesmo eixo.

Acima do conteúdo, ação discreta “Voltar ao catálogo”. Usar o asset de seta para a esquerda aprovado, quando estiver disponível no projeto, com rótulo textual; não usar a imagem como único nome da ação. Preservar proporções do asset e evitar que sua margem transparente provoque desalinhamento. A imagem deve ser decorativa para leitores de tela.

Título principal “Carrinho”, Lilita One Raspberry, aproximadamente 36 px no desktop e 30 px em celular. Deixar cerca de 24 px antes da área dos itens.

### Desktop

Duas colunas com intervalo aproximado de 24–32 px:

- Lista de itens à esquerda, ocupando aproximadamente dois terços da largura.
- Resumo à direita, com aproximadamente 320–360 px, alinhado ao topo da lista.

Não manter todo o conteúdo comprimido numa coluna estreita como no estado atual. Não criar tabela rígida que exija rolagem horizontal.

### Celular e telas intermediárias

Quando as colunas não couberem confortavelmente, empilhar lista e resumo, nessa ordem. A transição pode ocorrer em torno de 900 px, ajustada conforme o espaço real do conteúdo.

O resumo aparece depois dos itens; não usar painel fixo que cubra o conteúdo ou botão flutuante nesta etapa. Preservar a rolagem natural e a navegação compartilhada.

## Painel de cada item

Fundo branco, borda rosa suave, cantos de aproximadamente 20 px e preenchimento de 20–24 px. Separar itens por cerca de 16 px. Em listas longas, manter organização consistente sem excesso de sombras.

### Hierarquia do conteúdo

1. Nome completo do produto, 18–20 px, Varela Round, texto principal.
2. Preço unitário com rótulo discreto “por unidade”, 14 px, cor secundária.
3. Controle de quantidade e ações.
4. Subtotal do item, com rótulo explícito e valor destacado em Raspberry.

Em desktop, subtotal pode ficar à direita da linha principal ou da área de ações. Manter a associação clara com o item. Em celular, colocá-lo em linha própria quando necessário.

Não truncar nomes para preservar alinhamento. Não juntar preço unitário e subtotal numa única frase difícil de escanear.

### Miniatura

Miniatura pequena, aproximadamente 88–104 px, é permitida somente se a imagem já estiver disponível pelos dados ou mecanismos existentes, sem novo contrato de API ou consulta individual adicional por item. Usar o recurso real e o fallback existente.

Se essa condição não for atendida, usar o painel textual bem organizado. Não acrescentar backend, dados fixos ou foto inventada para satisfazer uma escolha estética.

## Quantidade e atualização

Rótulo visível “Quantidade”, associado a um campo com nome acessível que identifique o produto. Evitar repetir o nome longo inteiro no rótulo visual ao lado de cada controle quando isso causar colisão.

Seletor compacto com diminuir, valor editável e aumentar, seguindo a composição da SPEC 17. Altura aproximada de 48 px, borda com contraste suficiente e cantos de 12 px. Manter entrada direta por teclado e teclado numérico apropriado em celular.

### Preservação da confirmação explícita

O estado atual possui “Alterar quantidade”. Nesta etapa, preservar a confirmação explícita da alteração: o seletor modifica o valor proposto e “Atualizar” envia a alteração pelo fluxo existente.

- Usar rótulo visual “Atualizar”, com nome acessível que identifique o produto quando necessário.
- Não alternar silenciosamente para atualização automática a cada clique.
- Enquanto o valor não estiver confirmado, não apresentar subtotal ou resumo como se o servidor já tivesse aceitado a mudança.
- Pode exibir aviso discreto “Quantidade alterada. Atualize para confirmar.” quando houver diferença pendente.
- Não permitir prosseguir à revisão com alteração pendente sem tratamento: informar que a quantidade precisa ser atualizada e manter o foco no fluxo relevante. Não descartar a intenção do usuário silenciosamente.

Botão “Atualizar” secundário, fundo branco, texto e borda Raspberry, cantos de 12 px e área de interação confortável. Pode ficar ao lado do seletor no desktop e abaixo dele no celular.

Preservar valores mínimos, máximos e validações existentes. Não inventar limite ou permitir quantidade zero como atalho de remoção sem regra já existente. Ao receber quantidade inválida, mostrar mensagem junto ao item e preservar os valores efetivamente confirmados no resumo.

## Remoção

Exibir ação “Remover” com texto Raspberry e tratamento terciário discreto. Pode usar ícone de lixeira se já houver recurso consistente, mas não tornar o ícone a única informação da ação.

Manter a operação real existente. Não introduzir modal de confirmação, lixeira global ou mecanismo de desfazer nesta tarefa.

Após sucesso, remover o painel correspondente, atualizar resumo e contador real e anunciar o resultado de forma acessível. Se o elemento com foco for removido, encaminhar o foco para o próximo controle lógico, ou para o estado de carrinho vazio quando for o último item.

Após falha, manter o item e mostrar erro útil. Não apresentar remoção como concluída antes da confirmação real.

## Resumo do carrinho

Painel branco com borda rosa suave, cantos de aproximadamente 20 px e preenchimento de 24 px. Título “Resumo”, Lilita One Raspberry, aproximadamente 26 px.

Linha principal com rótulo “Subtotal dos produtos” e valor em Lilita One Raspberry, aproximadamente 28–32 px. Usar o subtotal real calculado no servidor.

Texto de apoio:

> Valores sujeitos à confirmação na revisão do pedido.

Pode complementar com “Entrega ou retirada serão definidas na revisão.” somente se isso refletir o fluxo real existente. Não exibir frete zero, total final, desconto, prazo ou promessa de entrega antes de sua definição efetiva.

Não tratar subtotal como total final. Não criar campo de CEP, cotação, cupom ou seleção de pagamento dentro do carrinho nesta etapa.

### Ação principal e autenticação

Para cliente autenticado, ação principal preenchida Raspberry e texto branco “Revisar e finalizar pedido”, respeitando o fluxo existente. A ação leva à revisão; não apresenta a compra como concluída.

Para visitante, organizar o acesso à autenticação sem a sequência de links azuis separados por barra que aparece no estado atual:

- Ação principal “Entrar e revisar pedido”, utilizando o destino existente que autentica e retorna ao fluxo de revisão.
- Texto discreto: “Entre na sua conta para continuar com o pedido.”
- Acesso secundário “Criar conta” somente se o fluxo existente oferecer esse caminho e preservar o carrinho e a continuidade. Não inventar retorno automático ausente.

Não exibir simultaneamente dois botões concorrentes para a mesma intenção de entrar e revisar. Se o comportamento atual de revisão já encaminhar para login, reutilizar esse fluxo em vez de criar uma segunda implementação.

Botão principal com largura total, altura aproximada de 48 px e cantos de 12 px. Impedir avanço durante atualização ou remoção pendente e explicar o motivo quando houver interação bloqueada relevante.

Preservar o carrinho em sessão e o comportamento real de login/logout. Não criar tabela de carrinho, autenticação paralela ou persistência nova.

## Estados de interface

### Carrinho vazio

Manter título e apresentar bloco simples com “Seu carrinho está vazio.” e ação principal “Escolher cupcakes”, levando ao catálogo real. Não mostrar resumo de subtotal nem ação de revisão como se houvesse itens.

Evitar grandes ilustrações ou estados decorativos que exijam novos assets. Se uma remoção esvaziar o carrinho, mostrar esse estado sem recarregamento desnecessário quando a integração existente permitir.

### Carregamento e processamento

Mostrar indicação legível de carregamento inicial. Durante atualização ou remoção, indicar processamento no item afetado e impedir pedidos conflitantes. Preservar consistência do resumo e evitar requisições concorrentes capazes de sobrescrever um estado mais recente.

### Erros e indisponibilidade

Falhas de rede, validação e indisponibilidade devem usar mensagens existentes compreensíveis, com tratamento visual consistente e anúncio acessível apropriado.

Se houver item indisponível ou alteração de preço, preservar a validação atual no servidor e o tratamento existente antes da conclusão. Não recalcular preços no navegador, fixar valores da captura ou garantir disponibilidade com base apenas no estado visual.

Confirmações de atualização e remoção devem ser perceptíveis sem depender de toast temporário. Não anunciar sucesso antes da resposta real.

## Acessibilidade e responsividade

- Foco visível em todos os links, campos e botões; ordem de teclado acompanhando a leitura dos itens e depois do resumo.
- Controles de toque de pelo menos 44 × 44 px; ações principais com aproximadamente 48 px de altura.
- Usar rótulos e nomes acessíveis específicos por produto em ações repetidas.
- Cor não pode ser a única indicação de erro, disponibilidade, processamento ou alteração pendente.
- Texto comum com contraste previsto na SPEC 15. Não usar branco sobre Pale Pink ou Butter Yellow.
- Conferir nomes longos, vários itens, valores maiores e quebras naturais de linha.
- Verificar 360, 390, 768 e 1440 px, além de zoom de 200%, sem recortes ou rolagem horizontal.
- Respeitar redução de movimento; não adicionar animações de entrada ou deslocamento decorativo de cards.

## Critérios de aceite

| ID | Cenário | Resultado esperado |
| --- | --- | --- |
| CART-UI-01 | Comparar com a loja | Mesmas fontes, cores, cantos e hierarquia das telas aprovadas |
| CART-UI-02 | Carrinho com um ou vários itens | Painéis legíveis; preço unitário e subtotal identificados sem ambiguidade |
| CART-UI-03 | Alterar quantidade | Edição compacta, confirmação explícita e atualização real pelo servidor |
| CART-UI-04 | Quantidade pendente ou inválida | Mensagem clara e ausência de revisão com valores visuais não confirmados |
| CART-UI-05 | Remover item | Operação real, resumo e contador atualizados, foco preservado de forma lógica |
| CART-UI-06 | Remover último item | Estado vazio e retorno ao catálogo; sem revisão disponível |
| CART-UI-07 | Visitante continuar | Login real com continuidade existente e sem duplicação de ações concorrentes |
| CART-UI-08 | Cliente continuar | Acesso à revisão real, sem confirmação antecipada de compra |
| CART-UI-09 | Falha de atualização ou remoção | Nenhum falso sucesso; estado confirmado permanece coerente e nova tentativa é possível |
| CART-UI-10 | Valores | Subtotais recebidos do servidor; sem frete ou total final inventados |
| CART-UI-11 | Produto indisponível | Tratamento e bloqueios funcionais existentes preservados |
| CART-UI-12 | Tela estreita e teclado | Controles utilizáveis, sem colisão de texto, foco visível e resumo após os itens |

## Entrega e verificação da implementação

Entregar capturas reais em desktop e celular, com carrinho preenchido, alteração de quantidade, confirmação e estado vazio. Usar dados de teste e ambiente protegido para operações que alteram dados; nunca inserir credenciais em evidências.

Executar os roteiros existentes pertinentes a carrinho, autenticação e continuidade da revisão. Como esta SPEC envolve controles e apresentação de estado, não limitar o aceite à comparação de cores. Registrar os resultados reais, pendências e impactos compartilhados. Não repetir testes PostgreSQL sem relação com a alteração por mero polimento visual.

Respeitar a organização de pastas e a tecnologia vanilla. Preservar rotas, contratos, seletores e atributos consumidos por scripts e testes; adaptar consumidores e verificações se uma mudança de marcação for indispensável.

Nomes em inglês conforme as convenções da linguagem. Comentários necessários em português, curtos e sem banners ou linhas ornamentadas.

Não fazer commit, push, publicação ou remoção de skills e SPECs nesta tarefa. Não alterar migrações, regras de cálculo ou estrutura do banco para atender ao design.
