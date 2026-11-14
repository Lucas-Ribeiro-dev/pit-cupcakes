# SPEC UI 002 — Página inicial e catálogo

Status: direção visual aprovada pelo responsável; implementação pendente.

## Objetivo e forma de entrega

Aplicar à página inicial existente do PIT Cupcakes a aparência da prévia aprovada, preservando catálogo, navegação, autenticação e carrinho reais. O resultado deve ser simples, legível e coerente com o design system.

Esta especificação orienta a IA responsável pela implementação. Nesta entrega foram produzidos apenas este documento e as referências visuais já existentes. A prévia é um protótipo visual independente, não uma implementação integrada ou evidência de teste da aplicação.

Orientação permanente do responsável para próximas entregas de design nesta colaboração: produzir somente SPECs e imagens. Não gerar código, páginas executáveis ou arquivos de implementação como parte dessas entregas.

## Referências visuais

- Desktop aprovado: [homepage-desktop.png](design-system/homepage-desktop.png), 1440 × 1200 px.
- Referência de composição em tela estreita: [homepage-mobile.png](design-system/homepage-mobile.png), 500 × 1500 px. É uma captura parcial da página, com continuação vertical dos produtos. Não comprova validação em celular real ou em 360 px.
- Base: [15-SPEC-DESIGN-SYSTEM.md](15-SPEC-DESIGN-SYSTEM.md).

![Página inicial aprovada em desktop](design-system/homepage-desktop.png)

Prioridade: pedidos explícitos do responsável, esta SPEC para a composição da página e SPEC UI 001 para fundamentos visuais. Caso regras genéricas de uma skill conflitem com essas escolhas, preservar a direção aprovada.

## Escopo

Redesenhar cabeçalho compartilhado conforme aplicável à página inicial, faixa de apresentação, título do catálogo, cards, mensagens e rodapé. Se o cabeçalho for compartilhado com outras telas, conferir essas telas para evitar regressão de navegação e disposição.

Manter HTML, CSS e JavaScript vanilla e a estrutura atual de pastas, exceto necessidade técnica concreta explicada na entrega. Preservar seletores, identificadores, atributos e contratos usados pelos scripts e testes; se uma mudança de marcação for indispensável, adaptar os consumidores e verificar o comportamento afetado.

Esta etapa não inclui novos filtros, busca, promoções, estoque quantitativo, logo definitiva ou mudanças nos formulários, checkout, administração e regras de negócio. Não alterar banco, migrações, autenticação, frete, preços, pagamento ou pedidos para adequá-los à imagem. Não remover skills ou SPECs, nem realizar commit, push ou publicação nesta etapa.

## Identidade visual obrigatória

| Elemento | Decisão |
| --- | --- |
| Cor principal | Raspberry Pink, #BC164D |
| Superfície de destaque | Pale Pink, #FFD0DF |
| Detalhe pequeno | Butter Yellow, #FFE164 |
| Fundo geral | Branco rosado, #FFF9FB |
| Superfícies dos cards e cabeçalho | Branco, #FFFFFF |
| Texto principal | Ameixa escuro, #301523 |
| Texto secundário | Ameixa suave, #725763 |
| Nome da loja e títulos curtos | Lilita One, regular 400 |
| Textos, navegação, botões e nomes dos produtos | Varela Round, regular 400 |
| Preços em destaque | Lilita One, regular 400, Raspberry |

Não manter marrom nos botões ou na marca, nem verde nos preços. As cores próprias das imagens dos produtos podem permanecer. Não usar amarelo como fundo predominante ou em textos sobre rosa claro.

Preferir fontes locais com licença preservada e alternativa legível durante o carregamento. Não simular pesos de fonte inexistentes. Aplicar as cores aos componentes efetivos; apenas declarar variáveis sem utilizá-las não atende ao aceite.

## Estrutura e proporções

Ordem visual: cabeçalho, faixa de apresentação, catálogo, mensagem contextual quando necessária e rodapé.

Em desktop, limitar o conteúdo a aproximadamente 1200 px e centralizá-lo. Na referência de 1440 px, as margens laterais são aproximadamente 120 px. Em telas menores, manter pelo menos 16 px de respiro lateral.

Usar escala de espaçamento da SPEC UI 001. Referências principais: 32 px entre cabeçalho e apresentação; 32 px entre apresentação e catálogo; 24 px entre texto introdutório do catálogo e cards; 20 px entre cards. Ajustar conforme a largura, sem sobreposição ou recortes.

Evitar sombras fortes, gradientes, animações de entrada e efeitos decorativos adicionais. A personalidade vem da tipografia, da faixa rosa e das cores das ações.

## Cabeçalho

Fundo branco, borda inferior rosa discreta e altura aproximada de 96 px em desktop. Marca à esquerda; ações à direita.

### Marca

Exibir “PIT Cupcakes” em Lilita One e Raspberry Pink, aproximadamente 36 px no desktop e 30 px em tela estreita. Manter a grafia e um pequeno ponto amarelo decorativo ao final, sem fazê-lo parte do nome acessível.

A marca deve levar à página inicial usando a rota real existente. Esta composição tipográfica é provisória e não substitui a futura logo.

### Ações de visitante

Ordem obrigatória da esquerda para a direita:

1. Entrar: fundo branco, borda e texto Raspberry.
2. Criar conta: fundo Raspberry e texto branco.
3. Carrinho: botão com ícone à extrema direita, fundo Pale Pink e ícone Raspberry.

Botões com altura aproximada de 48 px, cantos de 12 px e espaçamento de 12 px. O carrinho pode medir aproximadamente 52 × 48 px e deve permanecer depois das ações de conta.

Usar ícone de carrinho consistente, em traço simples, sem emoji e sem rótulo textual visível obrigatório. Fornecer nome acessível, como “Abrir carrinho”, e informar sua quantidade de forma acessível. O ícone decorativo não deve ser anunciado separadamente.

Exibir contador pequeno no canto superior direito, Raspberry com texto branco. Usar a quantidade e a convenção reais já adotadas pelo carrinho; não inventar um contador independente. Não confundir unidades com tipos de produto. Contador e nome acessível devem refletir o mesmo estado.

### Sessão autenticada

Substituir as ações de visitante pelos controles correspondentes já existentes, como acesso à conta e saída, preservando permissões e eventuais acessos administrativos. Manter a hierarquia visual e o carrinho por último. Não apresentar “Entrar” e “Criar conta” como estado ativo para quem já está autenticado.

## Faixa de apresentação

Bloco Pale Pink com cantos aproximados de 24 px, margens alinhadas ao catálogo e preenchimento de 32–48 px no desktop. Altura aproximada de 270 px, ajustável ao conteúdo.

À esquerda:

- Título em Lilita One e Raspberry: “Seu dia merece um cupcake.” Pode ocupar duas linhas, como na imagem.
- Texto em Varela Round e ameixa: “Escolha seu sabor favorito. O carinho vai em cada cobertura.”

Título de aproximadamente 48–52 px em desktop e 36 px em tela estreita, com entrelinha compacta e sem cortes em letras acentuadas. Texto de apoio em torno de 16 px, reduzido a 14–16 px em tela estreita sem prejudicar leitura.

À direita, manter uma ilustração simples de cupcake nas cores da marca, com poucos detalhes amarelos, conforme a referência. Ela é decorativa e não representa um produto selecionável. Não gerar nova identidade de marca nem substituir fotos de produtos por essa arte. A imagem aprovada orienta a composição; a implementação pode reutilizar um recurso adequado existente.

Em tela estreita, ocultar a ilustração quando não houver espaço suficiente. Preservar o título e o texto; não usar a arte atrás do texto.

## Cabeçalho do catálogo

Título: “Escolha seu favorito”, em Lilita One, Raspberry e aproximadamente 32 px no desktop ou 28 px em tela estreita.

Texto: “Cupcakes artesanais, feitos para adoçar sua pausa.”, em Varela Round e cor secundária, aproximadamente 14–15 px.

Manter hierarquia semântica de títulos: um título principal da página, título de seção para o catálogo e títulos dos produtos abaixo dele.

## Cards de produto

Desktop amplo: quatro colunas com largura equivalente e alinhamento entre preços e ações. Cards brancos, borda rosa discreta e cantos de aproximadamente 20 px. Usar a quantidade real de produtos; os quatro da imagem são conteúdo ilustrativo da base atual.

Ordem do conteúdo:

1. Imagem do produto ou substituta existente.
2. Nome completo do produto.
3. Preço real em reais, no formato brasileiro.
4. Disponibilidade textual.
5. Botão “Adicionar ao carrinho”.
6. Ação “Ver detalhes”.

### Imagens

Área de altura uniforme, aproximadamente 190 px em desktop e 220 px em uma coluna. Centralizar a imagem e preservar suas proporções; impedir que ela invada o nome, o preço ou os botões. Usar fundo suave e coerente com o rosa, mantendo legibilidade e os recursos existentes.

Preservar tratamento de imagem ausente e falha de carregamento. Não fabricar foto para produto sem foto. Texto alternativo deve descrever o produto ou identificar a substituta, conforme o recurso.

### Conteúdo

Preenchimento aproximado de 20 px. Nome em Varela Round, 17–18 px, permitindo quebra de linha. Não cortar nomes para obter alinhamento; acomodar títulos maiores com altura flexível.

Preço em Lilita One, aproximadamente 28 px, Raspberry. Não substituir valores recebidos da aplicação por valores fixos da referência.

Para produto disponível, exibir “Disponível” em Raspberry, acompanhado opcionalmente por pequeno ponto da mesma cor. Para indisponível, exibir “Indisponível no momento” em cor secundária. A informação deve existir em texto, independentemente da cor.

### Ações

“Adicionar ao carrinho” é o botão principal preenchido Raspberry, texto branco, largura total e altura de pelo menos 48 px. No desktop, garantir texto legível de pelo menos 14 px, ajustando espaço em vez de comprimir a fonte para caber.

“Ver detalhes” aparece abaixo como link visual Raspberry, sublinhado e com área de interação confortável. Navegar para a rota real de detalhes. A aparência pode ser de link mesmo quando a integração existente exigir outro elemento; preservar semântica apropriada à ação.

Para indisponível, desabilitar a inclusão com rótulo “Indisponível” e fundo neutro. “Ver detalhes” continua disponível. Manter também a validação real no servidor; a aparência desabilitada não é a única proteção.

## Comportamento real e estados

Integrar o visual aos dados e às ações atuais. Não transportar para a aplicação os comportamentos simulados da prévia.

- Ao adicionar, executar a inclusão real. Atualizar o contador somente após confirmação do resultado.
- Mostrar confirmação persistente e perceptível em região apropriada, anunciada de modo acessível, sem deslocar o foco sem necessidade. Incluir acesso ao carrinho quando adequado.
- Durante uma requisição, impedir envio acidental duplicado conforme o fluxo existente, indicar processamento e restaurar a ação após falha.
- Se a inclusão falhar, manter o estado coerente e mostrar mensagem útil. Não simular sucesso apenas com alteração de texto ou contador.
- Preservar estados de carregamento, catálogo vazio, erro de rede, recuperação e produto inexistente já tratados na aplicação.
- Não apresentar um catálogo vazio como falha nem deixar uma falha como tela sem explicação.
- Se o comportamento atual permitir carrinho de visitante, preservá-lo. Não exigir login antecipado por causa da mudança visual; cadastro obrigatório antes de concluir a compra permanece conforme o escopo funcional.

A frase da prévia “os botões demonstram a interação, sem realizar compras” é exclusiva do protótipo e não deve entrar na página real. Preservar os avisos acadêmicos e de pagamento simulado nos locais funcionais apropriados.

## Rodapé

Fundo branco, borda superior rosa suave, marca tipográfica menor à esquerda e identificação acadêmica à direita em desktop. Em tela estreita, empilhar e alinhar à esquerda.

Preservar a identificação do Projeto Integrador Transdisciplinar em Engenharia de Software e os dados acadêmicos existentes que forem necessários. Não criar dados institucionais ou texto de direitos sem base.

## Responsividade e acessibilidade

- Desktop amplo: marca e ações na mesma linha; quatro cards por linha.
- Larguras intermediárias: reduzir para duas ou três colunas conforme o espaço real disponível, sem apertar ações e nomes.
- Celular: uma coluna de produtos. Marca na primeira linha e ações na segunda quando necessário, mantendo Entrar, Criar conta e carrinho nessa ordem. O carrinho deve permanecer totalmente visível.
- Verificar pelo menos 360, 390, 768 e 1440 px. A imagem de 500 px não substitui essas verificações.
- Não permitir rolagem horizontal, imagens sobrepostas ou botões fora da tela. O conteúdo deve continuar utilizável com zoom de 200%.
- Teclado deve alcançar marca, conta, carrinho e ações dos produtos em ordem coerente; foco visível e sem armadilhas.
- Não depender de hover, cor, posição ou animação como única fonte de informação.
- Controles devem oferecer área de toque de pelo menos 44 × 44 px; botões principais mantêm aproximadamente 48 px de altura.
- Texto comum deve atender ao contraste definido na SPEC UI 001. Não usar branco em Pale Pink ou Butter Yellow.
- Respeitar preferência por redução de movimento. Não adicionar animações de entrada.

## Critérios de aceite

| ID | Verificação | Resultado esperado |
| --- | --- | --- |
| HOME-01 | Comparar página com referência desktop | Mesma direção de cor, tipografia, hierarquia e composição; diferenças responsivas justificadas |
| HOME-02 | Conferir cores aplicadas | #BC164D, #FFD0DF e #FFE164 corretos; marca e ações sem o antigo marrom |
| HOME-03 | Conferir fontes | Lilita One e Varela Round efetivamente carregadas, inclusive acentos |
| HOME-04 | Abrir como visitante | Entrar, Criar conta e ícone do carrinho, nessa ordem, com destinos reais |
| HOME-05 | Abrir autenticado e sair | Controles refletem a sessão e preservam o fluxo existente |
| HOME-06 | Adicionar produto disponível | Inclusão real, confirmação visível e contador coerente |
| HOME-07 | Produto indisponível | Inclusão bloqueada e detalhes acessíveis |
| HOME-08 | Conferir catálogo e detalhes | Nomes, preços e imagens vêm da aplicação; rotas funcionam |
| HOME-09 | Forçar falha e recuperar | Mensagem correta, sem falso sucesso, com recuperação funcional |
| HOME-10 | Catálogo vazio e imagem ausente | Estados existentes preservados e visualmente coerentes |
| HOME-11 | Conferir larguras e zoom | Sem recortes ou overflow; carrinho acessível à direita das ações |
| HOME-12 | Usar teclado | Ordem lógica, nomes acessíveis e foco perceptível |
| HOME-13 | Examinar efeitos compartilhados | Cabeçalho e estilos não quebram conta, carrinho ou demais páginas afetadas |

## Validação e entrega da implementação

Comparar o resultado real à referência, corrigir divergências materiais e coletar capturas da aplicação em desktop e celular, incluindo cabeçalho completo, cards e confirmação de inclusão. Registrar versão e ambiente verificados no relato técnico; não apresentar a imagem do protótipo como evidência de teste integrado.

Executar os testes de navegador e as verificações funcionais pertinentes às mudanças. Reaproveitar os roteiros existentes; não repetir toda a suíte PostgreSQL apenas por alterações de cor, fonte e espaçamento. Se houver mudança em comportamento, integração ou segurança, executar também a regressão correspondente e registrar o resultado real.

Não usar o banco de produção para cenários que criam ou alteram dados de teste. Preservar a proteção existente do banco de testes e nunca incluir credenciais em documentos ou capturas.

Na entrega, informar arquivos alterados, comparação com as imagens, resultados obtidos, evidências e pendências. Não declarar aceite completo de cenários não executados.

## Convenções para a IA implementadora

Usar nomes em inglês conforme as convenções da linguagem: classes em UpperCamelCase, funções e variáveis em lowerCamelCase e classes CSS com padrão consistente com o projeto. Comentários, quando necessários, curtos e em português. Não criar banners, linhas decorativas ou blocos de comentário ornamentados.

Respeitar a organização existente e as regras do repositório. Esta SPEC não solicita código nesta conversa: sua implementação será feita pela IA escolhida pelo responsável no ambiente do projeto.
