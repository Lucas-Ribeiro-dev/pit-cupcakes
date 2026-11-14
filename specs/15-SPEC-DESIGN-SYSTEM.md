# SPEC UI 001 Design system PIT Cupcakes

Versão 1 de 11/11/2026. Entrega desta etapa: fundamento visual e catálogo de componentes reutilizáveis para a loja existente. Não redesenhar todos os fluxos sem validar a direção visual. Este documento não autoriza troca de tecnologia, alteração de regras de negócio, publicação ou remoção de skills/SPECs do Git.

## Direção visual

Doceria contemporânea, acolhedora e simples. Referência: contraste entre Raspberry Pink intenso, Pale Pink claro e pequenos detalhes Butter Yellow; tipografia de destaque espessa/arredondada e interface legível. Usar a referência como direção de cor e personalidade, sem copiar nome, símbolo ou composição da marca Crumbz.

O usuário deseja aparência bonita e funcional, não uma campanha complexa. Sem gradientes, glassmorphism, parallax, animações de entrada, cursores especiais, formas decorativas espalhadas, carrosséis ou biblioteca de componentes. Formulários e administração devem parecer parte da mesma marca, com menor carga decorativa.

Manter HTML, CSS e JavaScript vanilla. Não adicionar framework, Tailwind, biblioteca de animação ou etapa de build do front. Os componentes são convenções de HTML/CSS e pequenos comportamentos existentes.

## Paleta confirmada

| Papel | Nome | Hex | Aplicação |
| --- | --- | --- | --- |
| Marca/ação | Raspberry Pink | #BC164D | Marca, botões principais, links e destaque de títulos |
| Superfície de marca | Pale Pink | #FFD0DF | Faixas de destaque, áreas suaves e estados secundários |
| Acento pequeno | Butter Yellow | #FFE164 | Pequeno detalhe do wordmark, detalhe de selo ou ícone; nunca fundo dominante |
| Fundo base | Branco rosado | #FFF9FB | Fundo das páginas, leve e próximo do branco |
| Superfície | Branco | #FFFFFF | Cards, formulários, menus e tabelas |
| Texto principal | Ameixa escuro | #301523 | Textos extensos e valores que precisam de legibilidade |
| Texto secundário | Ameixa suave | #725763 | Instruções, descrição e metadados |
| Ação hover | Raspberry escuro | #98103D | Hover/active de ação principal |
| Borda de campo em branco | Rosa médio | #B0788D | Campos e controles sobre branco; outras superfícies precisam de nova verificação |

A cor amarela ocupa no máximo aproximadamente 5% da interface visível. Pale Pink aparece em blocos delimitados; grandes áreas de leitura permanecem brancas ou quase brancas. Não cobrir todos os cards de Raspberry; a ação deve se destacar pela raridade.

## Contraste verificado

Razões calculadas a partir dos hex originais, para cores sólidas opacas. O mínimo adotado é 4,5:1 para texto comum; não depender da exceção para texto grande como padrão da interface.

| Texto / fundo | Razão aproximada | Regra |
| --- | --- | --- |
| Branco / Raspberry | 6,25:1 | Botão principal e faixa Raspberry |
| Raspberry / Pale Pink | 4,57:1 | Permitido, mas margem pequena; não reduzir opacidade ou clarear o Raspberry |
| Raspberry / Butter Yellow | 4,82:1 | Permitido em pequenos selos, sem tratá-lo como estado de alerta automaticamente |
| Ameixa escuro / Pale Pink | 12,20:1 | Preferido para leitura em superfície rosa |
| Ameixa escuro / Butter Yellow | 12,89:1 | Preferido para pequenos detalhes com texto |
| Ameixa escuro / fundo base | 16,06:1 | Texto principal |
| Ameixa suave / fundo base | 6,20:1 | Texto secundário |
| Branco / Pale Pink | 1,37:1 | Proibido para informação textual |
| Branco / Butter Yellow | 1,30:1 | Proibido para informação textual |

Bordas e indicadores de controle precisam ser distinguíveis da superfície. #B0788D em branco tem aproximadamente 3,55:1, mas em Pale Pink somente 2,59:1: nesse fundo usar borda Raspberry ou controle em superfície branca. Não assumir que todo token serve em qualquer combinação. Validar novamente cor com transparência, sobre imagem ou estados de hover.

## Tipografia

### Destaque e nome da loja

Escolha proposta pelo designer: Lilita One, regular 400. Usar no wordmark textual “PIT Cupcakes” e em títulos principais curtos. A aparência já é encorpada; não solicitar peso 700/900 inexistente, não aplicar itálico artificial ou text-shadow para engrossar letras.

Lilita One aproxima a sensação arredondada, compacta e expressiva da referência, sem afirmar que é a fonte original da imagem. O nome deve manter a grafia PIT Cupcakes. Não criar logotipo ilustrado nesta fase; a marca gráfica será feita posteriormente.

Wordmark Raspberry em fundo branco ou Pale Pink. Em fundo Raspberry, usar branco. Amarelo pode aparecer como pequeno ponto ou detalhe decorativo separado; não destacar palavra inteira em amarelo sobre Pale Pink, que não tem contraste suficiente. Não rotacionar letras nem transformar o nome em imagem nesta etapa.

### Texto da interface

Fonte confirmada: Varela Round, regular 400. Usar no corpo, links, navegação, botões, campos, cards, tabelas e instruções. “Valera Round” foi interpretado como Varela Round, nome oficial da família.

As duas famílias consultadas têm arquivo regular 400. Não depender de pesos artificiais 600/700 para hierarquia: usar tamanho, espaçamento, cor e organização. Usar fonte de destaque somente em títulos curtos, não em tabelas, números extensos ou parágrafos. Ênfase semântica não deve perder significado para leitores de tela.

| Uso | Família | Desktop | Mobile | Entrelinha |
| --- | --- | --- | --- | --- |
| Nome na navegação | Lilita One | 36 px | 30 px | 1,05 |
| Destaque de entrada, quando existir | Lilita One | 48 px | 36 px | 1,10 |
| Título de página | Lilita One | 36 px | 30 px | 1,15 |
| Título de seção curto | Lilita One | 26 px | 24 px | 1,20 |
| Nome de produto | Varela Round | 18 px | 18 px | 1,40 |
| Corpo, botão, campo | Varela Round | 16 px | 16 px | 1,50–1,60 |
| Rótulo/metadado | Varela Round | 14 px | 14 px | 1,45 |
| Preço em card | Lilita One | 26 px | 24 px | 1,20 |

Fallback do texto: system-ui, sans-serif. Fallback de destaque: ui-rounded, system-ui, sans-serif. Página permanece funcional enquanto carrega fonte.

Preferir fontes auto-hospedadas em assets/fonts, com font-display: swap e licença OFL junto dos arquivos. Obter arquivos de fonte de distribuição oficial, cobrindo português e números; evitar versões duplicadas, vários formatos desnecessários e bibliotecas npm apenas para fontes. Caso Google Fonts remoto seja usado no primeiro preview, documentar e evitar que o funcionamento dependa da rede externa.

## Tokens e organização CSS

Declarar tokens com nomes em inglês e valores centralizados. Exemplo de contrato de tokens, não código de implementação já executado:

```css
:root {
  --color-brand: #BC164D;
  --color-brand-hover: #98103D;
  --color-brand-soft: #FFD0DF;
  --color-accent: #FFE164;
  --color-page: #FFF9FB;
  --color-surface: #FFFFFF;
  --color-text: #301523;
  --color-text-muted: #725763;
  --color-input-border: #B0788D;
  --font-display: "Lilita One", ui-rounded, system-ui, sans-serif;
  --font-body: "Varela Round", system-ui, sans-serif;
  --radius-control: 12px;
  --radius-card: 20px;
  --radius-section: 24px;
  --content-max: 1200px;
}
```

Escala de espaço em px: 4, 8, 12, 16, 24, 32, 48 e 64. Usar rem na implementação e evitar ajustes arbitrários por página. Inputs e botões têm altura mínima 48 px; alvos de ícone isolado pelo menos 44 × 44 px. Não arredondar todos os elementos como pílulas: pílula fica para selo curto; controls usam 12 px, cards 20 px.

Sombra discreta rosada em cards elevados, sem substituir borda ou foco. Não aplicar sombra forte em todos os componentes. Bordas de cards podem ser suaves e decorativas; controles mantêm contraste suficiente.

Preservar estrutura existente. Proposta: assets/css/tokens.css para valores; assets/css/components.css para componentes; style.css para base/layout e compatibilidade. Não dividir por dividir: se o código atual permitir solução menor, manter poucos arquivos com responsabilidades claras. Evitar seletores globais que alterem todos os buttons, inputs, spans ou h2 sem escopo e quebrem administração. Comentários em português, curtos e sem banners.

## Componentes obrigatórios

### Cabeçalho e navegação

Fundo branco ou Pale Pink, wordmark à esquerda e links legíveis à direita. Altura aproximada de 80 px no desktop, adaptável no mobile sem altura fixa que corte conteúdo. Navegação móvel pode quebrar em linhas organizadas nesta primeira fase; se menu for necessário, implementar botão com nome acessível, aria-expanded, teclado e fechamento. Não esconder Minha conta, Carrinho, Meus pedidos, Administração ou Sair de forma que torne o fluxo inacessível.

### Botões e links

- Primário: Raspberry, texto branco, raio 12 px; hover Raspberry escuro. Uma ação principal dominante por grupo.
- Secundário: branco ou transparente, borda Raspberry e texto Raspberry.
- Terciário: link Raspberry sublinhado onde contexto não deixa clique evidente; não depender só de diferença de cor.
- Perigoso: rótulo explícito como Remover ou Cancelar pedido e confirmação quando já prevista; nunca amarelo chamativo como incentivo à ação destrutiva.
- Disabled claramente identificado, mantendo legibilidade e sem usar pointer-events para esconder completamente a razão. Botão em envio mostra Aguarde… e previne cliques duplicados, preservando a lógica existente.
- Foco: anel duplo claro/ameixa visível em branco, rosa e Raspberry; conferir os contextos reais. Nunca remover outline sem substituto equivalente.

### Card de produto

Superfície branca, raio 20 px, imagem com proporção 1:1 em área clara, nome, preço Raspberry, selo de disponibilidade e ações coerentes. Não deformar imagens atuais nem trocar SVGs do catálogo por imagens de outra marca. Substituição das imagens será etapa separada se solicitada.

Nome pode ocupar duas ou três linhas sem truncar informação essencial. Ações no rodapé com alinhamento consistente entre cards; evitar height fixa que corte texto. Ver detalhes secundário e Adicionar ao carrinho primário. Produto indisponível mantém detalhes acessíveis e não permite adicionar.

### Formulários

Campo branco com borda distinguível, rótulo sempre visível acima, instrução abaixo, foco claro e mensagem de erro ligada por aria-describedby quando aplicável. Placeholder não substitui label. Tamanho de texto mínimo 16 px nos campos móveis. Preservar autocomplete, inputmode, nomes, IDs e validações existentes.

Checkbox e radio têm estado selecionado Raspberry e tamanho confortável; manter input nativo utilizável por teclado. No checkout, apresentar aprovação/recusa em escolhas legíveis, sem seleção guiada apenas por cor. Não ocultar aviso de pagamento simulado ou modo de teste em nome de uma aparência mais comercial.

### Feedback de carrinho e mensagens

Preservar faixa de sucesso e link Conferir itens já implementados. Usar superfície suave com texto escuro, região aria-live e posicionamento ligado à ação. Não substituir exclusivamente por toast que desaparece ou por contador sutil. Melhorias visuais não removem evidência funcional previamente validada.

Estados técnicos podem usar cores auxiliares de sucesso, erro e aviso, exclusivamente semânticas, discretas e sempre acompanhadas de texto/ícone: não reutilizar Raspberry para todo status nem amarelo para toda mensagem. Conferir contraste das cores auxiliares escolhidas na implementação. Pagamento RECUSADO e produto indisponível precisam continuar inequívocos.

### Carrinho, checkout, pedidos e administração

Carrinho e resumo em superfícies brancas; subtotal/total visualmente distintos. Pale Pink no agrupamento do resumo; texto ameixa, valores destacados Raspberry. Não usar amarelo para grandes áreas ou tabela inteira.

Pedidos mantêm legenda textual por situação, alinhamento de valores e espaço entre ações. Administração usa mesma paleta/fonte de texto, cabeçalho claro e densidade confortável; tabelas não usam display font nas células. Em mobile, transformar apresentação ou permitir rolagem contida com contexto; não causar rolagem horizontal da página.

Estado vazio, erro de rede, carregamento, foco, disabled e sucesso entram no design system. Nenhum estado precisa de ilustração nova para funcionar.

## Layout e responsividade

- Conteúdo máximo 1200 px, margens laterais de 24–32 px em telas amplas e 16 px em celular.
- Grid do catálogo: 1 coluna em 360 px, 2 a partir de 600 px, 3 a partir de 900 px e 4 quando cards couberem confortavelmente em 1200 px. Ajustar pelo conteúdo real; não sacrificar tamanho de alvo para manter quatro colunas.
- Formulários de conta com largura de leitura entre 440 e 560 px, centralizados; checkout pode usar duas colunas no desktop e uma em mobile.
- Área de apresentação do catálogo, se houver, é curta, com Pale Pink, título Raspberry e chamada objetiva; produtos continuam próximos do início. Não adicionar grande landing page antes de comprar.
- Respeitar zoom de 200%, textos maiores e mensagens longas. Não trocar ordem lógica do DOM para simular layout.
- Interações até 150–180 ms em cor/sombra, sem movimentos grandes; respeitar prefers-reduced-motion.

## Execução em duas passagens

1. Fundamentos: criar tokens, carregar fontes, aplicar base comum e preparar prévia de componentes. Pode criar página de guia em operacional/design-system/ fora de static, servida somente por preview local; não expor uma página técnica ao cliente. Não criar build ou servidor novo obrigatório na aplicação.
2. Após revisão visual: aplicar o sistema às páginas existentes, começando por catálogo/detalhes, depois conta/endereços, carrinho/checkout/pedidos e administração. A mudança é visual; não renomear IDs usados pelo JS, campos, rotas, handlers ou respostas API por estética. Se markup precisar mudar, preservar semântica e atualizar testes afetados.

O responsável pediu iniciar pelo design system. Apresentar a prévia da primeira passagem e aguardar sua revisão antes de redesenhar todas as telas. Essa revisão visual é parte do fluxo solicitado, não autorização para encerrar sem produzir a prévia.

## Aceite desta primeira passagem

- Tokens e componentes coerentes com as cores exatas escolhidas; amarelo restrito a detalhes pequenos.
- Nome PIT Cupcakes em Lilita One, textos em Varela Round, acentos portugueses corretos e fallback funcional.
- Prévia mostra wordmark, título, parágrafo, links, card disponível/indisponível, botão primário/secundário/desabilitado/focado, campo normal/erro, opções de pagamento e mensagem do carrinho.
- Capturas reais da prévia em 360 e 1280 px; sem overflow indevido, controles cortados ou texto branco sobre fundos claros.
- Verificar contraste após a implementação, carregamento das fontes, teclado e manutenção das ações existentes afetadas. Não repetir todos os testes PostgreSQL por uma alteração apenas de cor e fonte; repetir verificação funcional/browser pertinente quando markup ou comportamento mudarem.
- Nenhuma mudança em banco, regras de autenticação, total, frete, transições de pedido, Docker, Git ou publicação nesta tarefa.
- Entregar arquivos alterados, URL/comando de preview, capturas e pendências. Preservar skills e SPECs; limpeza do repositório está adiada.

## Referências

- Referência visual anexada pelo responsável: paleta de três cores e lettering arredondado da marca mostrada no painel inferior.
- Lilita One: https://fonts.google.com/specimen/Lilita+One
- Varela Round: https://fonts.google.com/specimen/Varela+Round
- Metadados oficiais Lilita One: https://raw.githubusercontent.com/google/fonts/main/ofl/lilitaone/METADATA.pb
- Metadados oficiais Varela Round: https://raw.githubusercontent.com/google/fonts/main/ofl/varelaround/METADATA.pb
- Critério de contraste de texto: https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html

Fontes e contraste foram pesquisados/calculados na preparação desta SPEC. A interface ainda não foi implementada ou visualmente validada nesta tarefa.
