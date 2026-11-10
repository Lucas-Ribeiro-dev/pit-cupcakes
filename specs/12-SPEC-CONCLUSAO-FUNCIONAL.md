# SPEC 007 Conclusão funcional da loja

Plano de execução concentrada a partir de 10/11/2026. Prazo acadêmico: 17/11/2026 às 23h59 BRT. Esta SPEC integra funcionalidades pendentes; não autoriza declarar testes ou avaliações não realizados.

## Estratégia

Implementar checkout/pedidos e administração em blocos sequenciais na mesma tarefa, preservando módulos anteriores. Rodar testes por bloco afetado e uma rodada integrada ao final. Não bloquear toda implementação por credenciais indisponíveis no processo: preparar testes e seguir nas partes independentes, solicitando uma rodada local consolidada ao responsável.

Ler primeiro os contratos e código atuais de usuário, endereços, frete e carrinho. Esta SPEC complementa as anteriores. Se contrato existente e esta SPEC conflitam, explicar o conflito e adotar a menor adaptação coerente sem perder funcionalidade. Não redesenhar arquitetura nem atualizar dependências indiscriminadamente.

## Entrega mínima completa

1. Visitante navega e monta carrinho; autenticação continua obrigatória para comprar.
2. Cliente escolhe retirada ou um endereço próprio de entrega, revisa valores e confirma pagamento simulado.
3. Sistema registra pedido consistente, incluindo recusa simulada identificada.
4. Cliente consulta seus pedidos e detalhes.
5. Administrador mantém produtos e atualiza atendimento de pedidos.
6. Interface, documentação e dados de demonstração permitem apresentar o fluxo completo.

Sem cobrança real, cartão, integrações financeiras/fiscais, upload de imagem, e-mail transacional, recuperação de senha, descontos, relatórios gerenciais ou estoque quantitativo. Polimento visual fica para depois de concluir o fluxo.

## Decisões de demonstração propostas para destravar entrega

- Loja acadêmica de Brasília–DF; retirada gratuita. Endereço do ponto de retirada será dado fictício explicitamente identificado na demonstração/manual, nunca um estabelecimento real inventado.
- Configuração opcional de demonstração: CEP 70000000–70999999 com frete R$ 8,00; CEP 71000000–71999999 com frete R$ 12,00. São valores e intervalos arbitrários para demonstrar regras, não validação de cobertura postal/comercial de Brasília.
- Entrega exige UF DF e correspondência em uma faixa ativa; fora das duas faixas é recusada com alternativa de retirada. Não assumir atendimento de todo o DF.
- Responsável pode substituir esses parâmetros. Não iniciar ou sobrescrever faixas automaticamente em todos os ambientes; carregar somente por rotina demo explicitamente ativada e idempotente. Não alterar V3 aplicada. Faixas fictícias de testes permanecem separadas.
- Produtos de demonstração e faixas demo não substituem registros existentes. Endereços e dados de participantes não entram em seeds.

## Bloco 1 Revisão e confirmação da compra

### Revisão

Criar POST /api/conta/checkout/revisao com JSON modalidade (ENTREGA/RETIRADA), enderecoId para entrega e versaoEndereco. Exigir sessão autenticada e CSRF.

- Ler carrinho do servidor; rejeitar vazio ou qualquer linha bloqueada. Não comprar subconjunto silenciosamente.
- Reconsultar preços, disponibilidade, quantidades, endereço/proprietário/versão e frete no servidor.
- Retirada não exige endereço; não enviar dados antigos de entrega na resposta de retirada.
- Criar revisão imutável temporária, ligada ao usuário e sessão, com token aleatório, prazo de dez minutos, itens/versões/preços, modalidade, frete, subtotal, total e cópia do endereço quando entrega.
- Resposta 200: revisaoId, expiraEm, itens, modalidade, endereco quando aplicável, subtotal, frete e total. Dinheiro em strings decimais de duas casas. Totais em BigDecimal no servidor.
- Não aceitar valores monetários ou usuário enviados pelo navegador. Qualquer mudança de carrinho exige nova revisão; futura confirmação compara o estado efetivo, não apenas o formulário.

### Confirmação e repetição segura

Criar POST /api/conta/pedidos com JSON revisaoId e resultadoSimulado (APROVADO/RECUSADO), sem preço ou dados financeiros reais.

- Mostrar aviso de simulação na tela e permitir escolher aprovação/recusa antes de confirmar.
- Revisão inexistente, de outro usuário/sessão ou expirada é rejeitada sem criar pedido. Retornar erro compreensível, sem revelar dados alheios.
- Antes de registrar, conferir que carrinho, preços, versões relevantes, disponibilidade, endereço e taxa continuam iguais. Em mudança, 409 REVISAO_DESATUALIZADA, sem pedido; usuário faz nova revisão.
- Endereço alterado/removido, produto indisponível ou faixa de frete alterada exigem nova revisão. Se a ausência de faixa torna entrega inviável, informar a causa e não trocar para retirada automaticamente.
- Criar pedido, itens, cópia de endereço e pagamento simulado em uma transação. Erro provoca rollback completo.
- revisaoId é chave idempotente única por usuário no banco. Reenvio da mesma confirmação retorna o mesmo pedido, sem duplicação. Se tentar reenviar a mesma chave com outro resultado simulado, 409 CONFIRMACAO_DIVERGENTE.
- Verificar pedido já persistido dessa chave antes de exigir que revisão ainda esteja na sessão ou que carrinho ainda esteja cheio. Assim, resposta perdida pode ser recuperada na mesma sessão autenticada. Conservar vínculo de usuário e chave mesmo após consumir a revisão.
- Requisições simultâneas da mesma confirmação resultam em um pedido. Usar restrição única e tratamento transacional adequado; não tentar continuar usando transação marcada para rollback após violação de unicidade.
- Concorrência de produto/endereço/faixa: realizar leituras e validação com bloqueios/transação apropriados para impedir alteração entre validação e persistência. Ordenar bloqueios de produto por ID. Serializar confirmação e mutações do carrinho da mesma sessão. Não usar só um check fora da transação como garantia.
- Primeira confirmação retorna 201; repetição idempotente retorna 200 com mesmo pedido. Nenhuma resposta contém senha ou dados de outro usuário.

### Pagamento e carrinho

- APROVADO: pagamento simulado aprovado, atendimento RECEBIDO, limpar carrinho após commit bem-sucedido.
- RECUSADO: persistir pedido com pagamento simulado recusado e atendimento NAO_AUTORIZADO; manter carrinho para uma nova revisão. Não permitir preparar esse pedido.
- Cada revisão gera no máximo um pedido e um resultado de pagamento. Nova tentativa após recusa usa nova revisão e outro pedido; não implementar pagamento posterior no pedido recusado.
- Campos técnicos do pagamento nunca sugerem transação real. Guardar resultado e instante, sem cartão, CPF ou token financeiro.

## Banco e histórico

Criar próxima migração disponível, sem editar V1/V2/V3. Modelar pedido, item_pedido, endereco_pedido e pagamento_simulado, adaptando nomes internos conforme as convenções do projeto.

- pedido: id, usuario_id, revisao_id, modalidade, status_atendimento, subtotal, frete, total, criado_em e versao. Unicidade usuario_id/revisao_id; valores não negativos e total = subtotal + frete. Retirada tem frete zero. Valores NUMERIC(12,2).
- item_pedido: id, pedido_id, produto_id, nome_produto, preco_unitario, quantidade. Quantidade 1–99, preço positivo e produto único por pedido. Cópia histórica do nome/preço não muda se catálogo for editado.
- endereco_pedido: pedido_id PK/FK e campos de endereço da compra. Obrigatório para entrega e ausente para retirada, validado transacionalmente.
- pagamento_simulado: id, pedido_id único, resultado, criado_em. Sem campos de cartão.
- FKs preservam histórico: não apagar usuários/produtos em cascata que removam pedidos. Nesta versão não há exclusão física de produto na administração.
- Serviços garantem pedido com pelo menos um item e subtotal igual à soma das linhas; CHECK não agrega linhas de outra tabela.
- Datas em UTC com exibição local e versões para concorrência. Atualizar modelo/dicionário conforme o esquema efetivamente implementado.

## Bloco 2 Meus pedidos

- GET /api/conta/pedidos: pedidos somente do usuário autenticado, ordem mais recente primeiro. Resumo com id, criadoEm, modalidade, statusAtendimento, resultadoSimulado e total. Vazio é [].
- GET /api/conta/pedidos/{id}: detalhes com itens históricos, endereço histórico quando entrega, subtotal, frete, total e estados. Pedido alheio e inexistente retornam mesmo 404.
- Páginas vanilla checkout.html, pedidos.html e pedido.html sob estrutura existente. Adicionar navegação na conta e link para revisão no carrinho. Visitante faz login e retorna ao checkout por retorno fixo permitido, preservando carrinho.
- Mensagens específicas para vazio, indisponibilidade, área não atendida, revisão expirada, preço alterado e recusa simulada. Não repetir confirmação automaticamente com nova chave após erro de rede; preservar a chave e consultar/repetir com a mesma conforme ação explícita.
- Desabilitar envio enquanto pendente, sem confiar nisso como idempotência. Dados privados com no-store e apresentação segura.

## Bloco 3 Administração

### Conta administrativa

- Provisionamento explícito e restrito por perfil/rotina de inicialização e variáveis ADMIN_EMAIL, ADMIN_NAME e ADMIN_PASSWORD, sem senha padrão ou impressa. Nunca cadastro público de ADMIN.
- Se e-mail já existir, não promover CLIENTE nem sobrescrever senha automaticamente: falhar claramente no provisionamento. Rotina idempotente não altera administrador existente sem instrução explícita.
- Produzir instruções locais sem segredos; testar com administrador fictício isolado. Não criar conta real em migration.

### Produtos

- GET /api/admin/produtos e GET /api/admin/produtos/{id}.
- POST /api/admin/produtos: nome, descricao, preco, imagemCaminho opcional e disponivel. Nome 1–150, descrição 1–2000, preço entre 0.01 e 99999999.99 e no máximo duas casas; limites compatíveis com V1. Sem preço negativo ou arredondamento silencioso de entrada inválida.
- PUT /api/admin/produtos/{id}: mesmos dados e versao; conflito 409, preservando texto no formulário.
- Imagens apenas de recursos estáticos locais permitidos, sem upload, URL arbitrária ou caminho de disco. Opção sem imagem mantém substituta. Disponibilidade editável; sem exclusão física.
- Não aceitar id/versao no cadastro ou campos internos adicionais. Restringir ADMIN no servidor; cliente 403 e visitante 401 para APIs.

### Atendimento

- GET /api/admin/pedidos e GET /api/admin/pedidos/{id}; mostrar dados necessários ao atendimento, nunca hash de usuário.
- PUT /api/admin/pedidos/{id}/status com statusAtendimento e versao atual. Aplicar validação de perfil, versão e transição no servidor, sob transação.
- APROVADO + entrega: RECEBIDO → EM_PREPARACAO → SAIU_PARA_ENTREGA → CONCLUIDO.
- APROVADO + retirada: RECEBIDO → EM_PREPARACAO → PRONTO_PARA_RETIRADA → CONCLUIDO.
- CANCELADO permitido a partir de RECEBIDO, EM_PREPARACAO ou PRONTO_PARA_RETIRADA. Sem cancelamento após saída para entrega ou conclusão. Não simular estorno real; apenas informar cancelamento do pedido acadêmico.
- Recusado/NAO_AUTORIZADO, CANCELADO e CONCLUIDO não recebem novas transições. Não permitir saltos, regressões ou ramo errado da modalidade. Repetir estado não incrementa versão desnecessariamente; documentar resposta idempotente.
- Erros de versão/transição retornam 409. Cliente acompanha as alterações nos próprios pedidos.
- Páginas de administração em static/admin para catálogo e pedidos. Proteção da API é obrigatória mesmo se links estiverem ocultos. Tela sem sessão orienta login; sem perfil informa acesso negado.

## Testes concentrados e critérios de conclusão

Criar testes significativos para:

1. Compra de retirada e entrega com valores da faixa, dados próprios e registro completo.
2. Recusa simulada persistida, carrinho mantido e atendimento impedido.
3. Vazio, itens bloqueados, endereço alheio, revisão expirada e campos adulterados rejeitados.
4. Mudança de preço, disponibilidade, endereço e frete entre revisão/confirmação rejeitada sem pedido parcial.
5. Repetição, duas confirmações concorrentes e resposta perdida gerando apenas um pedido.
6. Erro de persistência com rollback e carrinho não limpo antes do commit.
7. Histórico preservado após edição do produto/endereço e acesso a pedido alheio bloqueado.
8. Cadastro/edição de produto por ADMIN; cliente e visitante sem privilégios; e-mail existente não promovido no provisionamento.
9. Atendimento por modalidade, versão, cancelamento e estados finais respeitados.
10. Navegador real: catálogo → carrinho → login → checkout → pedido → administração → acompanhamento; aprovação/recusa e 360/1280 px.

Usar cupcakes_test com guard de conexão efetiva. Preservar dados de cupcakes. Uma rodada consolidada de PostgreSQL e navegador pode fechar o carrinho pendente junto do fluxo completo; não duplicar execução sem falha ou alteração nova. Não usar APIs internas simuladas como evidência do fluxo integrado real. ViaCEP pode ser controlado nos testes de falha, identificado no registro.

Preparar um roteiro PowerShell local para o responsável fornecer credenciais uma vez e executar a rodada de testes e iniciar servidor isolado. Senha por entrada protegida, somente ambiente do processo; sem alterar variáveis globais ou salvar credencial em arquivo. Documentar comando validado, incluindo JAVA_HOME e DB_TEST_USERNAME. O servidor de teste usa main-class explícita e spring-boot:test-run, sem confundir com servidor de desenvolvimento. A automação de navegador é executada pelo agente enquanto o servidor local permanece aberto.

## Documentação em paralelo

- Atualizar histórias relacionadas, diagramas UML de classes/casos de uso/sequência dos fluxos essenciais e atividade de compra, arquitetura, modelos de dados/dicionário, manual e resultados por etapa.
- Manter documentos enxutos, coerentes com o código; não criar um relatório por endpoint.
- Não desenhar dados fictícios como se fossem histórico anterior do Integrador I. Não inventar participantes ou avaliações.
- Preparar formulário de avaliação e roteiro curto para cinco participantes, com os campos exigidos; preparar laudo que será preenchido com resultados reais.
- Documentar limitações: pagamento simulado, faixas acadêmicas, sessão em instância única e recursos fora do escopo.
- Capturas sem data no nome, com IDs de caso. Não esconder falhas nem marcar testes não executados como aprovados.

## Ambiente de publicação

Preparar artefato executável, configuração por ambiente, roteiro de banco/migrações e instruções de HTTPS/cookies/provisionamento admin. Revisar proteção de abuso de login antes da exposição pública, usando solução pequena e documentada. Não contratar, publicar ou acessar contas sem instrução do responsável. Provedor final segue pendente; identificar requisitos concretos sem paralisar implementação local.

## Encerramento da tarefa

Informar blocos concluídos, arquivos, migrações, testes executados, falhas e pendências. Pedir intervenção somente em bloqueio real que não possa ser resolvido dentro do projeto. Não encerrar a tarefa após mero plano, depois de um único bloco ou por faltar credencial nas partes independentes. Não criar commit/push automaticamente nesta implementação.
