# SPEC UI 008 — Minha conta

## Objetivo

Organizar a página Minha conta como um ponto de acesso simples às funções existentes: endereços e consulta de frete, pedidos e carrinho, com os dados cadastrais apresentados de forma legível.

Referência: captura atual enviada pelo responsável. Entrega desta colaboração: SPEC e imagem, sem código. Não adicionar edição de perfil, troca de senha, avatar, estatísticas ou recursos novos.

## Prévia visual

![Proposta para Minha conta](design-system/my-account-preview.png)

Imagem gerada para referência estética, com cliente e e-mail fictícios. Não é captura da aplicação ou evidência de teste. Não fixar dados nem contadores da imagem. Tamanhos, cores sólidas e fontes oficiais definidos na SPEC prevalecem sobre aproximações da imagem, incluindo pesos, texturas e proporções de títulos.

## Identidade e composição

Seguir as SPECs 15 a 21: Raspberry #BC164D nos títulos e ações, Pale Pink #FFD0DF em detalhes suaves, Butter Yellow #FFE164 somente em pequenos acentos. Fundo #FFF9FB; painéis brancos; texto principal #301523 e secundário #725763. Lilita One regular 400 nos títulos e Varela Round regular 400 nos textos e controles.

Preservar cabeçalho e rodapé compartilhados, rotas e permissões. O cabeçalho autenticado mantém Meus pedidos, Minha conta, Sair e carrinho, com este último à direita. Aplicar estilo consistente aos links e indicar a página atual sem depender só da cor. Não duplicar uma segunda barra de navegação dentro do conteúdo.

Conteúdo centralizado, largura máxima de aproximadamente 1120–1200 px, margem lateral mínima de 16 px em celular. No topo, ação discreta “Voltar ao catálogo”, com seta aprovada quando disponível, seguida de título “Minha conta” em Lilita One Raspberry, cerca de 36 px no desktop e 30 px em celular.

Texto de apoio: “Acesse seus pedidos, gerencie endereços e continue suas compras.” Não inserir saudação com dado fixo nem informações de conta fictícias na aplicação.

## Atalhos

Substituir a lista de links sem tratamento por três cards de navegação, com altura coerente, fundo branco, borda rosa suave, cantos de aproximadamente 20 px e preenchimento de 24 px. Em desktop amplo, uma linha de três cards; em telas menores, empilhar quando necessário, sem apertar textos.

| Título | Apoio | Ação |
| --- | --- | --- |
| Meus endereços | Cadastre endereços e consulte as opções de entrega. | Gerenciar endereços |
| Meus pedidos | Consulte seu histórico e os detalhes de cada pedido. | Ver pedidos |
| Meu carrinho | Confira os produtos antes de revisar o pedido. | Ver carrinho |

Títulos de 24–26 px, Lilita One Raspberry. Apoio de 14–16 px, Varela Round em cor secundária. Botões secundários com texto e borda Raspberry, fundo branco, cantos de 12 px e altura aproximada de 48 px; hover Pale Pink e foco perceptível.

As ações são navegação: usar links com destinos reais, mesmo com aparência de botão. Não aninhar links em links. Um alvo principal por card é suficiente; não tornar o título e o botão focáveis separadamente com a mesma intenção sem necessidade.

Não exibir números de pedidos, valores gastos ou contagem de endereços sem dado real já disponível e necessidade funcional. Não criar consultas de API para preencher decoração.

Ícones são opcionais e devem usar assets existentes coerentes. Não usar emoji, desenhar novos assets por código nesta entrega ou acrescentar ícones que exijam biblioteca. A hierarquia deve funcionar sem eles.

## Seus dados

Painel branco abaixo dos atalhos, separado por aproximadamente 24–32 px. Título “Seus dados”, Lilita One Raspberry, 26 px. Borda rosa suave, cantos de 20 px e preenchimento de 24 px.

Exibir os dois dados existentes, com rótulos visíveis e valores reais:

- Nome.
- E-mail.

Em desktop, apresentar em duas colunas se houver espaço; em celular, empilhar. Usar Varela Round 16 px nos valores e texto secundário menor nos rótulos. Remover o recuo irregular dos valores observado na captura, alinhando rótulos e conteúdo de cada grupo.

São informações somente de leitura, não campos de formulário desabilitados. Não acrescentar botões de editar, alterar e-mail ou trocar senha. Não expor papel interno, identificador de banco, dados de sessão ou informações de outros usuários.

Nomes longos e e-mails devem quebrar quando necessário, sem truncamento ou rolagem horizontal. Não mostrar valores inventados enquanto carrega; diferenciar carregamento, ausência de informação e erro conforme o contrato atual.

## Estados e comportamento

- Preservar autenticação e redirecionamento reais para visitante ou sessão expirada.
- Manter logout real, proteção CSRF e tratamento de sessão existentes. Não transformar saída em link que contorne o fluxo seguro atual.
- Dados vêm da conta autenticada pelo mecanismo já implementado.
- Carrinho e histórico mantêm seu estado real; não fixar contadores da imagem.
- Preservar acessos administrativos existentes condicionados às permissões, sem exibi-los para CLIENTE. A prévia representa cliente comum e não determina a navegação de ADMIN.
- Falhas de carregamento precisam de mensagem útil, sem substituir a tela por dados de exemplo.

## Responsividade e acessibilidade

Verificar 360, 390, 768 e 1440 px, zoom de 200% e navegação por teclado. Sem recortes, cards estreitos demais ou rolagem horizontal.

Ordem de leitura: retorno, título/apoio, atalhos, dados. Foco visível em ações, área de toque mínima de 44 × 44 px e texto comum com contraste previsto na SPEC 15. Preservar títulos semânticos e informação de página atual no cabeçalho.

Não adicionar animações de entrada, painéis flutuantes ou informações como única decoração. Espaço em branco é permitido; não preencher a página com funções não implementadas.

## Aceite

| ID | Cenário | Resultado esperado |
| --- | --- | --- |
| ACCOUNT-01 | Comparação visual | Mesmo padrão de fontes, cores, bordas e ações da loja |
| ACCOUNT-02 | Atalhos | Três destinos reais: endereços, pedidos e carrinho |
| ACCOUNT-03 | Dados | Nome/e-mail reais, somente leitura e bem alinhados |
| ACCOUNT-04 | Nomes e e-mails longos | Conteúdo completo sem corte ou overflow |
| ACCOUNT-05 | Sessão e saída | Login obrigatório e logout seguro preservados |
| ACCOUNT-06 | Cliente e administrador | Navegação respeita permissões existentes |
| ACCOUNT-07 | Celular e teclado | Cards empilhados quando necessário, foco e leitura coerentes |

## Entrega da implementação

Entregar capturas reais em desktop e celular, usando conta de teste com dados fictícios. Não usar o nome e e-mail pessoais da referência como conteúdo fixo ou dados da imagem de design.

Conferir navegação dos atalhos, dados da sessão e saída. Executar regressão pertinente aos componentes compartilhados afetados; não alterar backend ou banco por mudança visual.

Respeitar pastas, vanilla, rotas, seletores e consumidores existentes. Nomes em inglês conforme convenções; comentários necessários em português, curtos e sem banners. Não fazer commit, push, publicação ou limpeza de skills/SPECs nesta tarefa.
