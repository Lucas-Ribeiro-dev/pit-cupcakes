# SPEC UI 004 — Ajustes pontuais de login e criação de conta

## Objetivo

Manter o layout atual de login e cadastro, já considerado adequado pelo responsável, e executar somente dois ajustes: simplificar a orientação da senha e estilizar discretamente a navegação secundária.

Entrega desta colaboração: especificação, sem código. Referência: captura da tela de login enviada pelo responsável. A tela de cadastro deve receber o mesmo padrão, preservando seus campos e sua organização existentes.

## Imagem de referência

![Login e criação de conta lado a lado](design-system/login-cadastro-preview.png)

Imagem gerada para comparação estética: duas páginas separadas, não dois formulários na mesma tela. A composição do cadastro é ilustrativa; preservar os campos reais existentes. O fundo rosa do link “Voltar ao catálogo” representa hover, conforme as regras abaixo. Fontes oficiais, cores sólidas e medidas da SPEC prevalecem sobre aproximações da imagem, incluindo eventuais texturas, gradientes ou pesos artificiais. A prévia não constitui evidência de teste da aplicação.

## Escopo preservado

Não redesenhar o formulário, criar card adicional, incluir ilustrações, alterar larguras, reorganizar campos ou mudar o cabeçalho compartilhado. Manter Lilita One nos títulos, Varela Round na interface e a paleta das SPECs 15 e 16.

Não alterar rotas, validação de senha, autenticação, sessão, CSRF, permissões ou regras de cadastro. Não adicionar recursos de recuperação de senha, login social ou confirmação de senha nesta tarefa.

## Orientação de senha

Substituir a orientação atual:

> De 8 a 64 caracteres Unicode e no máximo 72 bytes UTF-8. Espaços fazem parte da senha.

Pelo texto exato:

> De 8 a 64 caracteres. Espaços fazem parte da senha.

Aplicar nos locais correspondentes das telas de login e cadastro onde essa orientação já aparece. Não adicionar um segundo texto com o mesmo conteúdo.

Usar Varela Round, aproximadamente 14 px, entrelinha de 1,5 e texto secundário #725763. Manter proximidade com o campo de senha e associação acessível com o campo. Permitir quebra natural em celular.

Trata-se de simplificação da mensagem de orientação, não de mudança das regras: preservar a validação existente de comprimento, o limite técnico de bytes e o tratamento de espaços. Não cortar, normalizar ou remover espaços da senha para adequá-la à orientação. Se uma senha violar o limite técnico, manter a rejeição com explicação útil junto ao campo; não aceitar, truncar ou esconder um erro para aparentar conformidade. Não replicar jargão técnico permanentemente no texto de ajuda.

## Navegação secundária

Os elementos abaixo navegam entre páginas e devem manter semântica de link e seus destinos reais, mesmo recebendo aparência de botão discreto.

### Voltar ao catálogo

Manter acima do título, alinhado ao início do formulário. Rótulo “Voltar ao catálogo”, com seta simples à esquerda opcional e decorativa.

Tratamento de ação terciária: texto Raspberry #BC164D, fundo transparente, sem borda em repouso, cantos de aproximadamente 10–12 px. Área de interação mínima de 44 px de altura, com preenchimento horizontal discreto. O texto deve continuar visualmente alinhado ao formulário, sem deslocamento horizontal excessivo causado pelo preenchimento.

Ao passar o ponteiro, usar fundo Pale Pink #FFD0DF. Não usar o roxo padrão de link visitado. Preservar foco de teclado claramente visível e indicação de interação por sublinhado no hover ou outro sinal além da cor.

### Alternância entre entrar e criar conta

Manter abaixo do botão principal, com intervalo aproximado de 16 px e alinhamento ao formulário. Não competir com a ação principal e não preencher com Raspberry sólido.

| Tela | Ação principal existente | Link secundário |
| --- | --- | --- |
| Login | Entrar | Criar conta |
| Criação de conta | Preservar o rótulo principal existente | Entrar na conta |

Texto do link secundário em Varela Round, aproximadamente 14–16 px e Raspberry. Fundo transparente ou branco conforme a superfície atual, borda Raspberry fina, cantos de aproximadamente 12 px e altura mínima de 44 px. Largura ajustada ao conteúdo, com preenchimento lateral de aproximadamente 16 px.

Hover com fundo Pale Pink e texto Raspberry. Foco visível com contorno e afastamento, sem depender apenas da alteração de fundo. Em telas estreitas, permitir largura maior quando necessário, preservando margem lateral e texto sem cortes.

Não criar novas frases de apoio obrigatórias como “Ainda não tem conta?”; a alteração solicitada se limita ao tratamento dos links já existentes.

## Ação principal

Preservar o botão principal preenchido Raspberry #BC164D, texto branco, altura aproximada de 48 px, largura atual do formulário e cantos de 12 px. Manter o fluxo real de envio, processamento e mensagens de erro e sucesso.

## Verificação e aceite

- Login e cadastro exibem o texto curto de senha exatamente como definido, onde a orientação já existia.
- Não aparece orientação técnica de bytes no texto permanente de ajuda; a validação real permanece intacta.
- “Voltar ao catálogo”, “Criar conta” e “Entrar na conta” seguem o tratamento discreto e levam aos destinos existentes.
- Links não ficam roxos após visitados; todos oferecem foco perceptível e funcionam por teclado.
- Botão principal mantém a maior hierarquia visual.
- Conferir em desktop e em 360/390 px, sem rolagem horizontal, sobreposição ou corte de textos.
- Verificar navegação entre as três páginas, submissão válida e erro de validação existente. Preservar o retorno ao fluxo anterior quando esse comportamento já existir.
- Registrar capturas reais das duas telas após implementação. Não declarar testes funcionais ou de banco que não tenham sido executados.

## Orientações à IA implementadora

Respeitar pastas, tecnologias e seletores existentes. Evitar alterações em outras telas ao reutilizar estilos compartilhados. Se houver impacto compartilhado, verificar os componentes afetados.

Nomes em inglês conforme as convenções da linguagem. Comentários necessários em português, curtos e sem banners ou linhas ornamentadas.

Entregar relação de arquivos alterados, resultado das verificações e capturas. Não fazer commit, push, publicação ou remoção de skills e SPECs nesta tarefa.
