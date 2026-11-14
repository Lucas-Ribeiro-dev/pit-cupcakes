# SPEC UI 006 — Revisão e confirmação do pedido

## Objetivo

Refinar o checkout existente com a mesma identidade das SPECs 15 a 19, tornando modalidade, endereço, revisão e pagamento simulado fáceis de identificar. Entrega desta colaboração: especificação e imagem de referência, sem código.

Referência funcional: captura enviada pelo responsável. Ela mostra o início do fluxo; não permite concluir que as seções posteriores estejam ausentes. Inspecionar o fluxo atual completo antes de implementar e preservar suas regras.

## Prévia visual

![Revisão do pedido com as três etapas organizadas](design-system/checkout-preview.png)

Imagem gerada para referência estética de cliente autenticado e revisão já gerada. Endereço e valores são ilustrativos: subtotal R$ 12,50, frete R$ 8,00 e total R$ 20,50 não constituem configuração comercial ou dados a fixar na aplicação. A imagem não define resultado inicial do pagamento, nem obriga adicionar miniatura se os dados atuais não a disponibilizarem. Preservar a progressão funcional real e mostrar etapas posteriores somente quando válidas. Fontes oficiais, cores sólidas, medidas e regras desta SPEC prevalecem sobre aproximações da imagem. Não é evidência de teste integrado.

## Problemas observados

- Radios de modalidade e endereço excessivamente grandes, com indicador azul padrão e contorno oval ocupando quase toda a largura.
- Links de navegação sem o tratamento visual aprovado.
- Instrução longa concentrada no topo, difícil de consultar durante a conclusão.
- Seções e mensagens de invalidação de revisão precisam de hierarquia e posicionamento mais claros.

## Identidade e escopo

Raspberry #BC164D nas ações e títulos; Pale Pink #FFD0DF em seleções e avisos suaves; Butter Yellow #FFE164 somente em pequenos detalhes. Fundo #FFF9FB, painéis brancos, texto #301523 e apoio #725763. Lilita One regular 400 nos títulos e valores em destaque; Varela Round regular 400 na interface.

Preservar tecnologias, organização, sessão, autenticação, CSRF, propriedade dos endereços, cotação no servidor, revisão temporária, revalidação, idempotência e resultados de pagamento existentes. Não alterar banco, migrações, limites, cobertura ou valores para atender à composição visual.

Não adicionar cobrança real, dados de cartão, cupom, estimativa de entrega, campos de endereço dentro do checkout ou etapas fictícias. A retirada gratuita continua conforme a regra existente; entrega depende de endereço e faixa atendida, não apenas de o endereço ser Brasília/DF.

## Composição

Preservar cabeçalho e rodapé compartilhados. Conteúdo centralizado em largura máxima de aproximadamente 1120–1200 px e margens mínimas de 16 px em celular.

Antes do título, ação terciária “Voltar ao carrinho”, com o asset de seta aprovado quando disponível. “Catálogo” pode permanecer como ação secundária discreta, sem separador de barra vertical e sem competir com o retorno principal.

Título “Revisão e confirmação do pedido”, Lilita One Raspberry, aproximadamente 32–36 px em desktop e 28–30 px em celular. Permitir quebra natural.

Desktop: coluna principal flexível à esquerda e resumo de aproximadamente 320–360 px à direita, com intervalo de 24–32 px. Celular e larguras sem espaço confortável: coluna única, com resumo após os dados que ele representa e antes da confirmação final.

Não usar painel fixo que cubra controles. Resumo pode acompanhar a rolagem somente se houver espaço suficiente e sem ocultar conteúdo com zoom; não é requisito desta etapa.

## Aviso de simulação

Substituir o bloco extenso inicial por aviso conciso em Pale Pink, texto escuro e borda discreta:

> Pagamento simulado. Nenhum valor será cobrado.

Complemento curto permitido: “Após gerar a revisão, escolha um resultado de pagamento para testar.”

As instruções específicas devem ficar junto da ação correspondente. Não ocultar o aviso nem tornar a recusa difícil de descobrir. Não repetir todo o roteiro de três etapas em um parágrafo inicial.

## Etapa 1 — Recebimento

Painel branco com borda rosa suave, cantos de aproximadamente 20 px e preenchimento de 24 px. Título “1. Recebimento”, 24–26 px, Lilita One Raspberry. A numeração representa a sequência real, não decoração.

### Modalidade

Apresentar duas opções com rótulo inteiro clicável:

- “Retirada na loja”, apoio “Gratuita”.
- “Entrega”, apoio “Em endereço cadastrado; sujeita à área atendida”.

Desktop: opções lado a lado se couberem. Celular: empilhadas. Cada opção tem painel branco, borda, cantos de 12 px e preenchimento de 16 px. Seleção usa borda Raspberry e fundo Pale Pink ou tonalidade suave, mantendo contraste legível.

Indicador radio compacto, aproximadamente 18–20 px, alinhado ao início do rótulo. O alvo clicável é a opção inteira, com no mínimo 48 px de altura. Nunca ampliar o radio para preencher o painel nem aplicar largura total, borda de campo ou cantos de botão ao indicador.

Preservar controles nativos ou semântica equivalente completa, nome do grupo, exclusividade, operação por teclado e foco visível. A seleção deve ser reconhecível por indicador e texto/contexto, não apenas cor.

### Endereços

Mostrar seleção de endereços apenas para entrega. Usar opções verticais com radio compacto à esquerda e texto à direita: identificação disponível do destinatário/endereço e linhas legíveis de logradouro, número, complemento quando existente, bairro, cidade/UF e CEP formatado.

Não cortar endereços longos ou omitir informação necessária para distinguir opções. Não exibir detalhes de outros usuários. Usar dados fictícios nas imagens de referência; não reproduzir o endereço pessoal presente na captura.

Ação “Gerenciar endereços” com estilo secundário discreto, usando o destino e retorno existentes. Sem endereço: mensagem clara e acesso ao cadastro existente; não permitir gerar revisão de entrega sem seleção válida.

Ao selecionar retirada, esconder ou desativar seleção de endereço conforme o fluxo existente e apresentar orientações reais de coleta quando disponíveis. Não inventar endereço comercial, horário ou prazo de retirada.

### Geração da revisão

Botão principal “Gerar revisão do pedido”, Raspberry e texto branco, altura aproximada de 48 px, cantos de 12 px. Posicionar após modalidade/endereço, com respiro de 16–24 px.

Durante a operação, indicar processamento e impedir submissões duplicadas. Cobertura, modalidade, endereço e valores são confirmados pelo servidor. Falhas devem aparecer junto à etapa, com mensagem útil e possibilidade de correção.

## Etapa 2 — Revisão

Exibir a revisão real recebida do servidor, sem criar uma confirmação visual antes da geração. Título “2. Confira o pedido”. Apresentar itens, quantidades, preços e subtotais, modalidade e endereço ou dados reais de retirada conforme o fluxo atual.

O resumo lateral apresenta “Resumo do pedido” e os valores correspondentes à revisão válida: subtotal dos produtos, frete ou retirada e total. Usar formato monetário brasileiro, rótulos à esquerda e valores alinhados à direita. Total em Lilita One Raspberry, aproximadamente 28–32 px.

Antes de revisão válida, mostrar apenas valores confirmados que o estado atual realmente disponibilizar, com rótulo de subtotal; onde não houver cálculo, usar “A calcular na revisão”. Nunca antecipar frete gratuito, preço final ou cobertura. Não somar valores no navegador para substituir o servidor.

Mudanças em modalidade, endereço, carrinho, preço ou condições relevantes mantêm a invalidação existente. Aviso contextual:

> Os dados foram alterados. Gere uma nova revisão para continuar.

Mostrar apenas quando houver alteração real, junto da ação de gerar nova revisão. Não deixar mensagens antigas visíveis após sucesso ou permitir confirmação de revisão inválida/expirada.

Preservar validade temporária e revalidações existentes. Não criar temporizador decorativo ou nova duração de revisão.

## Etapa 3 — Pagamento simulado e confirmação

Exibir após a revisão ser gerada, conforme a progressão real existente. Título “3. Pagamento simulado”. Apoio:

> Escolha o resultado para testar. Nenhum valor será cobrado.

Duas opções visíveis, com a mesma qualidade visual dos seletores de modalidade:

- “Simular pagamento aprovado”.
- “Simular pagamento recusado”.

Não esconder recusa em texto, parâmetro técnico, documentação ou ação secundária difícil de descobrir. Preservar estado inicial e obrigatoriedade da seleção existentes; não pré-selecionar um resultado diferente apenas para a imagem.

Usar rótulos explícitos, radio compacto e tratamento selecionado em Raspberry/Pale Pink. Não depender só de verde/vermelho para distinguir resultados.

Manter o rótulo funcional do botão final existente, salvo ajuste textual necessário para explicar sua ação real. Usar Raspberry preenchido e largura confortável. Próximo da ação, identificar claramente que a confirmação usa pagamento simulado.

Enquanto confirma, bloquear duplicidade no controle e preservar a idempotência real do servidor. Não substituir isso por um bloqueio apenas visual.

Pagamento aprovado e recusado devem manter seus efeitos e mensagens reais. Em recusa, não mostrar sucesso, pedido autorizado ou limpar o carrinho se a regra atual o preserva. Permitir continuidade conforme o fluxo implementado; não criar tentativa automática ou cobrança real.

## Estados e acessibilidade

- Preservar login obrigatório antes da conclusão e a continuidade existente após autenticação.
- Tratar carregamento, falha de rede, CEP fora da cobertura, endereço indisponível, revisão inválida/expirada e produto indisponível sem mensagens técnicas expostas.
- Mensagens associadas à etapa e anunciadas de modo acessível; sem toast temporário como única confirmação.
- Foco visível, ordem de leitura coerente, rótulos associados e grupos de escolha nomeados.
- Quantidades e endereços extensos não podem provocar overflow.
- Testar 360, 390, 768 e 1440 px, além de zoom de 200% e teclado.
- Texto comum com contraste adequado; branco somente sobre superfície suficientemente escura, como Raspberry.
- Evitar seletores globais de campo que atinjam radios, checkboxes ou botões indevidamente. Corrigir a causa do radio gigante sem quebrar outros formulários.

## Critérios de aceite

| ID | Verificação | Resultado esperado |
| --- | --- | --- |
| CHECK-01 | Consistência visual | Mesmas cores, fontes, bordas e ações da loja |
| CHECK-02 | Modalidade | Radios proporcionais, opção inteira clicável e seleção por teclado |
| CHECK-03 | Entrega | Endereço real do cliente selecionável, com validação de cobertura preservada |
| CHECK-04 | Retirada | Sem exigência indevida de endereço; gratuidade conforme a regra real |
| CHECK-05 | Sem endereços ou fora da área | Orientação útil e ausência de revisão fictícia |
| CHECK-06 | Geração e resumo | Valores do servidor; frete e total final somente quando confirmados |
| CHECK-07 | Alterar dados após gerar | Revisão invalidada e nova geração necessária conforme a regra existente |
| CHECK-08 | Pagamento | Aprovação e recusa simuladas facilmente identificáveis e utilizáveis |
| CHECK-09 | Confirmar e repetir ação | Comportamento idempotente preservado, sem pedido duplicado |
| CHECK-10 | Recusa | Mensagem e estado reais, sem falso sucesso ou perda indevida do carrinho |
| CHECK-11 | Tela estreita e zoom | Sem recortes, radios gigantes ou resumo cobrindo controles |
| CHECK-12 | Regressão de estilos | Outros campos, radios e checkboxes afetados pelo CSS compartilhado continuam corretos |

## Entrega da implementação

Respeitar estrutura, vanilla, rotas, contratos e proteções de teste existentes. Nomes em inglês conforme convenções; comentários necessários em português, curtos e sem banners.

Coletar capturas reais de entrega, retirada, revisão gerada e seleção dos dois resultados simulados. Executar os roteiros pertinentes e registrar o que foi efetivamente verificado. Não usar dados pessoais nas imagens de referência ou credenciais nas evidências.

Mudanças em marcação e controles exigem regressão funcional do checkout, não apenas inspeção estética. Não alterar banco ou regras para fazer um teste passar. Não fazer commit, push, publicação ou limpeza de skills e SPECs nesta tarefa.
