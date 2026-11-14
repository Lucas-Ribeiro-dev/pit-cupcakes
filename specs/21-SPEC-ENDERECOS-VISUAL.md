# SPEC UI 007 — Cadastro e gerenciamento de endereços

## Objetivo

Refinar a página de endereços existente, mantendo a identidade das SPECs 15 a 20. A referência enviada mostra endereços postais, não gerenciamento de e-mails. Escopo: apresentação, organização e usabilidade dos controles existentes.

Entrega desta colaboração: especificação e imagem, sem código. A captura mostra somente o começo do formulário; conferir os campos reais antes de implementar. Não concluir que campos abaixo do recorte estão ausentes nem criar novos campos a partir da imagem ilustrativa.

## Prévia visual

![Proposta de gerenciamento de endereços](design-system/addresses-preview.png)

Imagem gerada para referência estética, com dados fictícios. Não é captura da aplicação ou evidência de teste. Campos representados não autorizam alterar o modelo existente. Usar Varela Round para destinatário e informações do endereço conforme a regra textual abaixo, mesmo que a imagem aproxime alguns destaques com fonte de título. Cores sólidas, fontes oficiais e contraste da SPEC prevalecem sobre variações ou texturas da geração. Aplicar a cor Raspberry à ação Remover, independentemente da aproximação da imagem.

## Identidade

Raspberry #BC164D nas ações e títulos; Pale Pink #FFD0DF nas superfícies suaves; Butter Yellow #FFE164 somente em detalhe pequeno da marca. Fundo #FFF9FB, painéis brancos, texto principal #301523 e secundário #725763. Lilita One regular 400 nos títulos; Varela Round regular 400 nos textos, campos e botões.

Painéis com borda rosa discreta, cantos de aproximadamente 20 px e preenchimento de 24 px. Campos e botões com cantos de 12 px e altura aproximada de 48 px. Não manter controles cinza padrão do navegador ou links azuis/roxos sem estilo. Evitar sombras fortes, gradientes e decoração adicional.

## Composição

Preservar cabeçalho e rodapé compartilhados. Conteúdo centralizado, largura máxima aproximada de 1120–1200 px e margens mínimas de 16 px em celular.

Ação terciária “Minha conta” com seta aprovada quando disponível, título “Endereços” e apoio:

> Cadastre e gerencie seus endereços. Você escolhe qual usar em cada pedido.

Preservar a regra de não haver endereço selecionado como padrão. Não criar selo “Principal”, favorita, escolha automática ou persistência de seleção nesta tarefa.

Desktop: duas colunas com intervalo de 24–32 px. À esquerda, aproximadamente 40–45% da largura, endereços salvos e consulta de retirada. À direita, formulário de cadastro/edição com aproximadamente 55–60%. Em telas sem espaço confortável, empilhar salvos e formulário, nessa ordem. A edição deve ser fácil de localizar também em listas longas.

## Endereços salvos

Título de seção “Endereços salvos”, Lilita One Raspberry, 24–26 px. Cada endereço em painel branco separado por 16 px.

Organizar em linhas legíveis: destinatário em destaque; logradouro, número e complemento quando informado; bairro; cidade/UF e CEP formatado. Evitar parágrafo único que mistura nome e endereço. Não cortar nomes ou endereços extensos.

Não acrescentar apelidos como “Casa” ou “Trabalho” se não existir esse dado. Nas imagens de referência, usar apenas destinatário e endereço fictícios, sem reproduzir dados pessoais da captura.

### Ações

- “Editar”: botão secundário com borda e texto Raspberry, fundo branco e hover Pale Pink.
- “Consultar entrega”: ação secundária no mesmo padrão, sem competir com salvar o formulário.
- “Remover”: ação terciária Raspberry, com texto explícito e foco visível.

Organizar em linha com quebra natural; em celular empilhar quando necessário. Áreas de toque de pelo menos 44 × 44 px. Não usar somente ícones sem nome acessível.

Preservar os fluxos atuais de edição, exclusão e eventual confirmação já existente. Não introduzir modal ou mecanismo de desfazer nesta etapa. Durante operações, impedir envios conflitantes e indicar processamento.

Após exclusão, anunciar resultado e preservar foco em destino lógico. Não remover visualmente antes do sucesso real. Se excluir o último endereço, apresentar estado vazio com orientação para o formulário.

## Consulta de entrega e retirada

Resultado de “Consultar entrega” fica próximo do endereço consultado, com taxa recebida do servidor ou mensagem de indisponibilidade. Não confundir sucesso de cadastro ou consulta de CEP com cobertura de entrega.

Texto de apoio:

> Esta consulta é uma estimativa. O frete será recalculado na revisão do pedido.

Preservar cobertura e valores configurados; não fixar frete da imagem ou tratar toda Brasília/DF como automaticamente atendida. Não adicionar distância, prazo ou integração de transportadora.

Apresentar retirada em bloco discreto Pale Pink abaixo dos salvos, com título “Prefere retirar?”, texto “Retirada gratuita, sem precisar cadastrar endereço.” e ação secundária “Consultar retirada”. Preservar sua operação real. A consulta não seleciona retirada automaticamente para pedidos futuros.

Sem resultado, não exibir cotação ou confirmação fictícia. Quando houver resultado, identificar a modalidade e permitir compreender a qual endereço se refere.

## Formulário de novo endereço

Painel branco com título “Novo endereço”. Rótulos visíveis acima dos campos, Varela Round 14–16 px. Campos com borda de contraste suficiente, fundo branco e texto de pelo menos 16 px. Não usar placeholder como substituto do rótulo.

Preservar todos os campos, obrigatoriedade e limites existentes. Composição sugerida, apenas para campos já disponíveis:

1. Destinatário em largura total.
2. CEP ao lado de “Buscar CEP” em desktop; empilhar se não couber.
3. Logradouro em largura maior e número em largura menor.
4. Complemento e bairro, em duas colunas quando houver espaço.
5. Cidade e UF, com UF mais estreita.

Não mudar ordem se houver dependência funcional que torne a composição inadequada. Em celular, usar uma coluna, permitindo cidade/UF na mesma linha somente se legível.

CEP com teclado numérico apropriado, formatação visual legível e compatibilidade com colar valores. Preservar validação e normalização existentes. Campos com preenchimento automático do navegador coerente quando aplicável.

### Auxílio de CEP

Botão “Buscar CEP” secundário, borda Raspberry, largura adequada ao texto; não manter uma faixa cinza estreita como na captura.

Texto curto de ajuda:

> Use o CEP para ajudar no preenchimento. Você também pode informar o endereço manualmente.

Preservar ViaCEP como auxílio opcional e edição manual. Mostrar carregamento, CEP inválido, não encontrado, demora ou falha com mensagens úteis, sem bloquear permanentemente o formulário. Não afirmar que uma consulta externa foi bem-sucedida quando controlada por mock.

Não permitir que uma resposta atrasada sobrescreva um CEP mais recente ou descarte edições manuais posteriores. Usar o tratamento existente de concorrência e verificar seu comportamento se os controles forem alterados.

Buscar CEP não salva o endereço, não confirma número, não valida cobertura e não escolhe modalidade para o checkout.

### Salvar

Ação principal “Salvar endereço”, Raspberry preenchido com texto branco, altura aproximada de 48 px e largura confortável. Após sucesso real, atualizar lista e exibir confirmação perceptível conforme o fluxo existente. Não inventar endereço salvo ou sucesso só visual.

Erros associados aos respectivos campos; preservar dados digitados quando apropriado, sem usar somente mensagens temporárias. Evitar múltiplas submissões enquanto processa.

## Edição

Ao editar, reutilizar o formulário e trocar título para “Editar endereço”, ação principal para “Salvar alterações” e oferecer “Cancelar edição” como secundária. Preservar identidade e versão do registro, propriedade e tratamento de conflito existentes.

Se o formulário estiver fora da área visível, facilitar acesso ao título de edição e gerenciar foco de forma apropriada, respeitando redução de movimento. Não transformar edição em criação duplicada.

Cancelar encerra a edição sem alterar o registro salvo. Respeitar o comportamento atual de limpeza/restauração do formulário, tornando-o previsível. Após conflito ou registro removido, apresentar orientação útil; não sobrescrever silenciosamente outra versão.

## Estados e integração

- Carregamento inicial, lista vazia e falha de carregamento devem ser distintos e compreensíveis.
- Sem autenticação, preservar o encaminhamento real para login e o retorno existente.
- Propriedade dos endereços, versão e validações continuam no servidor.
- Preservar o retorno ao checkout quando esse fluxo já existir; não adicionar mecanismo novo ou escolher endereço automaticamente por causa da reformulação visual.
- Restringir alterações à interface. Não alterar contratos, rotas, banco, migrações, regras de CEP, cotação ou limites.
- Preservar seletores e atributos consumidos pelos scripts/testes; se houver mudança necessária de marcação, adaptar consumidores e verificar regressão.

## Acessibilidade e responsividade

Verificar 360, 390, 768 e 1440 px e zoom de 200%, sem rolagem horizontal ou campos cortados. Teclado deve alcançar ações dos endereços e campos na ordem de leitura, com foco visível e nomes específicos nas ações repetidas.

Mensagens de busca, salvamento, erro e exclusão devem ser anunciadas apropriadamente. Cor não pode ser a única indicação. Texto comum atende ao contraste da SPEC 15. Não usar branco sobre Pale Pink ou Butter Yellow.

Corrigir estilos compartilhados com seletores específicos sem deformar radios, checkboxes ou controles de outras telas. Não adicionar animações de entrada.

## Aceite e entrega

| ID | Verificação | Resultado esperado |
| --- | --- | --- |
| ADR-UI-01 | Consistência | Mesma identidade visual e componentes da loja |
| ADR-UI-02 | Lista | Dados completos e ações legíveis, inclusive com endereços extensos |
| ADR-UI-03 | Cadastro | Validações preservadas, gravação real e confirmação |
| ADR-UI-04 | CEP | Busca, falha, demora e preenchimento manual funcionam |
| ADR-UI-05 | Edição e cancelamento | Estado claro, versão preservada e ausência de duplicação |
| ADR-UI-06 | Remoção | Sucesso/falha reais, foco lógico e estado vazio correto |
| ADR-UI-07 | Cotação | Resultado do servidor associado ao endereço; sem cobertura ou taxa inventada |
| ADR-UI-08 | Retirada | Consulta gratuita sem seleção automática de modalidade |
| ADR-UI-09 | Segurança e continuidade | Propriedade, login e retorno existentes preservados |
| ADR-UI-10 | Celular e teclado | Sem recortes, ações acessíveis e foco visível |

Entregar capturas reais de lista com formulário, edição, feedback de busca e estado vazio. Usar dados fictícios de teste nas evidências e nunca expor credenciais.

Executar regressão dos fluxos afetados, aproveitando testes existentes; registrar resultados efetivos e limitações. A comparação visual sozinha não comprova cadastro, edição ou consulta de frete.

Respeitar pastas e vanilla. Nomes em inglês segundo as convenções; comentários necessários em português, curtos e sem banners. Não fazer commit, push, publicação ou remoção de skills e SPECs nesta tarefa.
