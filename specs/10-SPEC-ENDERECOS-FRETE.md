# SPEC 005 Endereços e consulta de frete

Versão 1. Escopo de implementação: HU17, HU18 e consulta necessária à HU08. Brasília–DF foi escolhida pelo responsável. Valores e faixas comerciais de demonstração ainda não foram definidos. Esta etapa não implementa compra, carrinho ou pedido.

## Objetivo e limites

Cliente autenticado mantém endereços na conta, recebe auxílio do ViaCEP e consulta se um endereço está na área atendida e qual seu frete. Cotação nesta etapa não cria pedido nem calcula total de carrinho. Checkout usará o mesmo serviço em etapa posterior.

Decisões operacionais desta SPEC: permitir vários endereços, sem endereço padrão; campos de endereço editáveis; retirada com frete zero; ausência de faixas cadastradas significa entrega não configurada, nunca taxa zero automática. Essas escolhas podem ser revisadas pelo responsável antes da implementação.

## Preservação e organização

- Inspecionar repositório, instruções, alterações e migrações antes de editar. Preservar V1/V2; se outra versão já existir, usar a próxima disponível.
- Manter stack e autenticação existentes. Recursos em endereco e frete com controller, service, repository, entity e dto conforme necessário. Integração externa pode ficar em endereco/integration.
- Novas classes e identificadores em inglês nas convenções Java/JavaScript; pacotes e contratos públicos definidos aqui em português. Comentários em português, breves, sem molduras ou banners.
- Interface vanilla simples, sem redesign e sem alterar favicon nesta etapa. Segredos fora do código e das evidências.

## Dados e validação

Tabela endereco, em nova migração: id BIGINT gerado, usuario_id FK para usuario, destinatario VARCHAR(120), cep VARCHAR(8), logradouro VARCHAR(160), numero VARCHAR(20), complemento VARCHAR(120) opcional, bairro VARCHAR(100), cidade VARCHAR(100), uf VARCHAR(2) e versao BIGINT para bloqueio otimista.

- Todos os campos, exceto complemento, são obrigatórios. Textos são aparados nas extremidades e não podem ficar vazios; não normalizar destrutivamente acentos ou nomes.
- CEP aceita na entrada oito dígitos ou formato XXXXX-XXX; persistir apenas oito dígitos, incluindo zeros iniciais. Rejeitar letras, formatos diferentes e excesso de caracteres. UF é convertida para maiúsculas e validada contra UFs brasileiras.
- Número aceita texto como S/N; nenhum campo deve ser interpolado em HTML não confiável ou em SQL.
- Banco reforça campos obrigatórios, limites, integridade referencial e CEP de oito dígitos. Limites acima valem também no servidor.
- Proprietário vem da sessão pelo usuário persistido. Não aceitar usuarioId, perfil ou outro proprietário no corpo.
- Consultas de alteração/remoção/detalhe filtram id e proprietário. Endereço inexistente e de outro usuário retornam o mesmo 404, sem revelar sua existência.
- Edição exige versao recebida; se desatualizada, 409 ENDERECO_DESATUALIZADO e nenhuma sobrescrita silenciosa. DELETE exige query versao, também validada, com a mesma proteção. Confirmar remoção na interface.
- Exclusão remove apenas endereço do cadastro. Futuro pedido terá cópia própria; não criar pedido agora.

## ViaCEP e decisão DT01

Necessidade: reduzir digitação ao cadastrar endereço. Decisão adotada pelo responsável: ViaCEP como auxílio; frete permanece local.

- Endpoint autenticado interno consulta exclusivamente https://viacep.com.br/ws/{cep}/json/ com CEP previamente validado. Host e caminho base fixos; não aceitar URL fornecida pelo cliente nem seguir redirecionamento para host arbitrário.
- Usar cliente HTTP compatível com a stack, com limites de conexão e resposta que encerrem a espera em até cinco segundos. Não adicionar retentativas automáticas ou consultas a cada tecla.
- Transmitir somente CEP. Não enviar nome, login, número, complemento ou cookie de sessão ao serviço externo.
- Mapear somente cep, logradouro, bairro, localidade para cidade e uf. Resposta com erro=true significa CEP não encontrado; resposta inesperada, falha e demora não significam inexistência.
- Front consulta ao sair do campo com CEP válido ou ao acionar Buscar CEP. Cancela/ignora consultas antigas quando CEP muda. Resposta atrasada não sobrescreve dados de outro CEP nem campos editados depois do início da consulta.
- Permitir revisão dos campos retornados e preenchimento manual, inclusive quando logradouro/bairro vierem vazios. Informar falha de consulta sem bloquear salvamento de endereço válido pelos critérios locais.
- A consulta não comprova existência do imóvel. A entrega depende exclusivamente da regra local de UF/faixa, validada no servidor. Documentar essa limitação sem afirmar validação postal completa.

## Faixas e cotação

Tabela faixa_frete: id BIGINT gerado, cep_inicio VARCHAR(8), cep_fim VARCHAR(8), valor NUMERIC(12,2), ativa BOOLEAN. Limites inclusivos com oito dígitos; inicio <= fim; valor >= 0; faixas ativas não podem se sobrepor.

- Escolher mecanismo que impeça ambiguidades também em alterações concorrentes, preferencialmente restrição de exclusão PostgreSQL de intervalos ou estratégia equivalente justificada. Não depender apenas de consulta prévia sem proteção transacional.
- Nesta etapa não criar CRUD administrativo de faixas. Inserções/configurações são controladas por script documentado ou mecanismo de configuração explícito e validado. Não alterar migração aplicada para ajustar preços.
- Banco de desenvolvimento começa sem faixas enquanto os dados finais não forem definidos. Testes usam faixas fictícias explicitamente identificadas em cupcakes_test. Não apresentar esses valores como cobertura real de Brasília.
- Endereço pode ser salvo fora da área, mas não permite cotação de entrega. Local de referência é Brasília–DF: entrega exige uf=DF e correspondência em exatamente uma faixa ativa. Não bloquear por igualdade literal do nome da cidade, pois ele pode representar região administrativa.
- Sem faixa correspondente: 422 FORA_DA_AREA_DE_ENTREGA. Sem configuração de faixas ativas para DF: 422 ENTREGA_NAO_CONFIGURADA. Informar retirada como alternativa, sem selecioná-la automaticamente.
- RETIRADA tem valor 0.00 e não exige endereço. ENTREGA exige enderecoId da conta; somente o servidor determina a taxa. Não aceitar valor informado pelo cliente.
- Cotação não garante um preço futuro. Futuro checkout recalculará e exigirá revisão em alteração de endereço, faixa ou taxa.

## Contratos da API

Todos os endpoints abaixo exigem autenticação. Operações POST, PUT e DELETE exigem CSRF conforme integração já existente. Respostas privadas usam Cache-Control: no-store.

| Método e caminho | Entrada | Sucesso |
| --- | --- | --- |
| GET /api/conta/enderecos | Nenhuma | 200 array ordenado por id; vazio [] |
| GET /api/conta/enderecos/{id} | ID positivo | 200 endereço da conta |
| POST /api/conta/enderecos | JSON campos de endereço, sem id/proprietário/versao | 201 endereço salvo |
| PUT /api/conta/enderecos/{id} | JSON campos e versao atual | 200 endereço atualizado |
| DELETE /api/conta/enderecos/{id}?versao=N | ID e versão não negativa | 204 |
| GET /api/conta/cep/{cep} | CEP válido | 200 campos de endereço disponíveis |
| POST /api/conta/frete/cotacao | JSON modalidade ENTREGA/RETIRADA; enderecoId só para entrega | 200 modalidade, enderecoId e versaoEndereco quando entrega, valor como string decimal de duas casas |

Objeto de endereço: id, destinatario, cep, logradouro, numero, complemento, bairro, cidade, uf e versao. Não expor entidade JPA ou usuário completo.

Erros seguem codigo e mensagem; validação pode incluir erros de campo. Status: 400 entrada inválida, 401 sem sessão, 403 CSRF inválido, 404 ENDERECO_NAO_ENCONTRADO, 409 versão divergente, 415 formato incompatível, 422 entrega não atendida/configurada, 500 falha interna. Consulta CEP: 404 CEP_NAO_ENCONTRADO, 502 SERVICO_CEP_INDISPONIVEL para falha upstream/resposta inválida e 504 CONSULTA_CEP_EXPIRADA para tempo excedido. Erros de integração não expõem corpo bruto, URL sensível ou stack trace.

## Telas

- static/paginas/conta/enderecos.html, ligada à Minha conta. Uma tela pode conter lista e formulário de inclusão/edição, com estados claros.
- Listar endereços, informar lista vazia, criar, editar, cancelar edição e remover com confirmação. Tratar edição concorrente sem perder silenciosamente o texto digitado.
- Após entrada, usuário pode voltar à tela de endereços. Validar sessão no carregamento; ausência de sessão direciona ao login existente. Não usar returnUrl arbitrário externo.
- Exibir consulta de frete associada ao endereço salvo, com taxa, fora da área ou entrega não configurada. Toda edição invalida resultado visual anterior; nova consulta usa dados persistidos. Informar retirada gratuita sem exigir checkout nesta etapa.
- Área de consulta de CEP com carregamento, resultado, campos manuais e falha recuperável. Mostrar endereço final para revisão antes de salvar.
- Funcionamento por teclado, labels explícitos, mensagens em português, sem depender só de cor; 360/1280 pixels sem rolagem horizontal.

## Testes previstos

| ID | Cenário | Aceite |
| --- | --- | --- |
| ADR01 | Criar/listar/detalhar endereço próprio | Persistência e campos corretos; proprietário obtido da sessão |
| ADR02 | Dois usuários e ID de endereço alheio | Nenhum dado revelado/alterado/removido; 404 para recurso alheio |
| ADR03 | Validação de limites/CEP/UF e propriedades indevidas | 400; sem gravação parcial |
| ADR04 | Editar e excluir com versão atual e desatualizada | Atualiza/remove apenas quando versão confere; conflito 409 |
| ADR05 | Visitante e CSRF inválido | Regras de segurança aplicadas; nenhum endereço criado indevidamente |
| ADR06 | ViaCEP válido, vazio, inexistente, falha e demora | Campos mapeados; erros distintos; preenchimento manual viável |
| ADR07 | Resposta atrasada e alteração de CEP | Nenhum campo novo sobrescrito por resposta antiga |
| FRT01 | Limites exatos de faixa ativa | Ambos limites inclusivos e valor decimal exato |
| FRT02 | CEP fora, faixa inativa e UF divergente | Entrega recusada mesmo com endereço salvo |
| FRT03 | Faixas inválidas/sobrepostas | Configuração rejeitada com integridade concorrente |
| FRT04 | Sem faixas, retirada e endereço alheio | Mensagens/zero de retirada/404 conforme contrato |
| ADR08 | Reinício e consulta de endereço | Cadastro persiste; dados demo não o substituem |
| ADR09 | Navegador em 360/1280 e teclado | Fluxos próprios, mensagens e cotação executados com capturas |
| REG01 | Regressão do catálogo e autenticação | Testes afetados aprovados e sessão/CSRF preservados |

Executar PostgreSQL/Flyway somente em cupcakes_test com proteção efetiva já existente; não copiar ou limpar cupcakes. Testes do ViaCEP usam servidor HTTP controlado/respostas simuladas, cobrindo falhas de forma determinística. Identificar que são simulações de integração; uma consulta real de fumaça pode ser executada separadamente e registrada, sem torná-la dependência de todos os testes.

Para navegador, adaptar servidor isolado existente ou criar classe equivalente de teste. Documentar comando funcional com spring-boot:test-run e main-class explícita, já validado na etapa anterior. Não repetir o comando spring-boot:run que falhou ao localizar a classe de testes. Se faltarem credenciais no processo, pedir configuração local mínima e continuar partes independentes.

## Evidências e entrega

Atualizar dicionário, contratos, DT01 e registro da terceira etapa com fatos executados, resultados e pendências. Capturas sem data nos nomes, com IDs ADR/FRT. Faixas finais/valores permanecem pendentes; ausência deles não impede concluir CRUD de endereços e validar motor de cotação com fixtures isoladas, mas impede declarar entrega de Brasília configurada.

Não fazer commit, push, publicação ou ampliar escopo sem instrução. Não marcar teste aprovado por mera compilação. Documentação acadêmica descreve implementação, decisões e evidências, sem incorporar prompts operacionais.

Referência da integração: https://viacep.com.br/ (documentação oficial já consultada na decisão DT01).
