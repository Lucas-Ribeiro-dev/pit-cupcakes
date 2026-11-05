# SPEC 004 Cadastro e autenticação

Versão 1 para implementação após revisão do responsável. Escopo: HU03, HU04 e HU05; base para permissões futuras. Não implementar compras, carrinho, endereços, ViaCEP, área administrativa ou polimento visual.

## Resultado esperado

Visitante cria conta de cliente, entra com e-mail e senha, consulta sua sessão e sai. Catálogo e detalhes continuam públicos e funcionais. Regras de acesso existem no servidor; não apenas nos links da interface.

## Preservar o projeto

- Manter Java 21, versão atual do Spring Boot e gerenciamento de dependências existente; não fazer atualização geral nesta etapa.
- Adicionar Spring Security e suporte de testes correspondente sob gerenciamento do Boot; consultar documentação compatível com a versão efetivamente instalada, não copiar APIs de versão principal diferente.
- Organizar classes em identidade/controller, service, repository, entity e dto; configuração de segurança em config. Usar nomes em inglês com UpperCamelCase para classes, lowerCamelCase para métodos/variáveis e UPPER_SNAKE_CASE para constantes.
- Preservar módulos e pacotes existentes em português. Contratos JSON e mensagens públicas em português.
- Comentários exclusivamente em português, breves e úteis; sem banners, molduras, emojis, separadores decorativos ou comentários que repetem o código.
- A ausência de favicon é pendência de polimento e não faz parte desta implementação.

## Regras de cadastro

- Campos JSON: nome, email e senha. Não aceitar perfil, id ou hash enviados pelo cliente; propriedades adicionais de cadastro são rejeitadas com 400, impedindo atribuição indevida.
- Nome: remover espaços nas extremidades, exigir de 2 a 120 caracteres.
- E-mail: remover espaços nas extremidades e converter para minúsculas com Locale.ROOT; exigir formato válido e máximo 254 caracteres. Essa política vale para cadastro e login, sem regras especiais de pontos ou sufixos de provedores.
- Senha: mínimo de 8 e máximo de 64 pontos de código Unicode, e no máximo 72 bytes em UTF-8 para compatibilidade com BCrypt. Não remover espaços nem normalizar senha. Exibir esses limites na interface, sem exigir composição arbitrária de maiúsculas/símbolos.
- Usar PasswordEncoder do Spring Security com BCrypt adaptativo, nunca texto puro ou SHA simples. Armazenar formato com identificador do algoritmo usando DelegatingPasswordEncoder. Verificar funcionamento com a versão instalada.
- Cadastro público sempre cria CLIENTE; não criar administrador por endpoint público nem credencial padrão.
- E-mail normalizado único no banco e validado no serviço. Tratar também violação de unicidade concorrente como 409, sem expor SQL.
- Cadastro concluído não autentica automaticamente: apresentar sucesso e link para login. Senha não reaparece na tela, resposta ou logs.
- Sem confirmação por e-mail, recuperação de senha, login social ou alteração de senha nesta fatia; registrar limitações, sem botões falsos.

## Banco

Criar nova migração V2__criar_tabela_usuario.sql, sem editar V1 aplicada. Tabela usuario: id BIGINT gerado, nome VARCHAR(120), email_normalizado VARCHAR(254) único, senha_hash VARCHAR(255), perfil VARCHAR(20) com CHECK CLIENTE/ADMIN e criado_em TIMESTAMPTZ. Campos obrigatórios e criação transacional. Não adicionar usuários reais a migrations ou dados demo.

Proposta: entidade User, repositório UserRepository e serviços de cadastro e carregamento para autenticação, separados conforme a responsabilidade. Nunca serializar a entidade inteira nem incluir senha_hash em DTO público. Atualizar o dicionário com V2 após implementação.

## Autenticação e sessão

- Usar autenticação de formulário do Spring Security com endpoint configurado para fetch, login por e-mail e handlers JSON; não criar mecanismo próprio de comparação de senha ou sessão.
- POST de login usa application/x-www-form-urlencoded, campos email e senha. Aplicar a mesma normalização de e-mail do cadastro antes da autenticação, preservando senha literalmente.
- Sessão em cookie HttpOnly com SameSite=Lax e timeout de inatividade de 30 minutos. Em produção HTTPS, Secure obrigatório; ambiente HTTP local permite cookie sem Secure. Não usar JWT nem guardar senha/token de sessão em localStorage ou sessionStorage.
- Renovar identificador da sessão após login com os mecanismos do Spring Security.
- Habilitar CSRF, inclusive em cadastro, login e logout. Disponibilizar endpoint público de token para a interface; devolver token efetivamente esperado pelo handler configurado.
- Front solicita token antes do primeiro POST e o envia no cabeçalho indicado. Após login ou logout, obter token novo, pois o anterior pode ter sido invalidado. Não desativar CSRF nem liberar CORS irrestrito para resolver integração.
- Logout somente via POST protegido, invalidando sessão e removendo cookie. GET não encerra sessão.
- APIs retornam JSON e códigos HTTP; não redirecionam fetch para HTML de login. Páginas podem direcionar visitante ao login com comportamento explícito.
- Resposta de login inválido é genérica, sem distinguir e-mail inexistente de senha errada. Não implementar bloqueio permanente ou rate limit improvisado; registrar proteção contra abuso como pendência antes de disponibilização pública.

## Contratos

| Endpoint | Entrada | Sucesso | Falhas esperadas |
| --- | --- | --- | --- |
| GET /api/autenticacao/csrf | Nenhuma | 200: token, headerName, parameterName | 500 tratado |
| POST /api/usuarios | JSON nome, email, senha; cabeçalho CSRF | 201: id, nome, email, perfil | 400 dados inválidos; 409 EMAIL_JA_CADASTRADO; 403 CSRF inválido |
| POST /api/autenticacao/login | Form email, senha; cabeçalho CSRF | 200: id, nome, email, perfil, com sessão persistida | 401 CREDENCIAIS_INVALIDAS; 403 CSRF inválido |
| GET /api/autenticacao/sessao | Cookie de sessão | 200: id, nome, email, perfil | 401 NAO_AUTENTICADO |
| POST /api/autenticacao/logout | Cookie e cabeçalho CSRF | 204 sem corpo | 403 CSRF inválido |

Erros preservam codigo e mensagem; validações podem incluir erros por campo, nunca valores de senha. Definir DADOS_INVALIDOS e CSRF_INVALIDO em português. Cadastro com JSON malformado retorna 400, não 500. Requests de formato incompatível retornam 415. Não permitir que tratamento genérico de exceções transforme erros esperados de segurança ou validação em 500.

## Política de acesso

- Públicos: catálogo, detalhes, recursos estáticos necessários, token CSRF, cadastro e processamento de login.
- Sessão exige autenticação; retorno somente do usuário atual.
- Reservar /api/admin/** a ADMIN e /api/conta/** a usuário autenticado. Esta fatia não cria operações administrativas fictícias.
- Rotas públicas explicitamente listadas; não usar permitAll em /api/**. Acesso autenticado sem perfil necessário retorna 403; ausência de autenticação em recurso protegido retorna 401.
- Testar regra de ADMIN com autoridade de teste e verificar a decisão de autorização. Caminho sem implementação não precisa retornar 200: um ADMIN pode ultrapassar a segurança e encontrar 404; cliente deve ser bloqueado antes. Não criar endpoint de negócio apenas para satisfazer teste.
- Preparação segura de administrador real fica para a fatia de administração.

## Interface vanilla

- Criar static/paginas/conta/cadastro.html, login.html e minha-conta.html; scripts em assets/js/cliente e utilitários de API/CSRF em assets/js/compartilhado.
- Manter visual simples e coerente com catálogo, sem redesign. Adicionar links Entrar/Criar conta ou Minha conta/Sair conforme sessão, sem quebrar detalhes.
- Cadastro: nome, e-mail, senha, confirmação de senha apenas no front e descrição dos limites. Confirmação divergente impede envio. Validação de domínio continua no servidor.
- Login: e-mail, senha, mensagens genéricas e desabilitação temporária de envio durante a requisição.
- Minha conta: exibir somente nome e e-mail; atualizar página preserva sessão; ausência de sessão direciona ao login. Sem edição de perfil nesta etapa.
- Saída informa encerramento e atualiza navegação; depois de sair, atualizar minha conta não permite recuperar dados da conta anterior. Conteúdo privado usa Cache-Control: no-store.
- Fetch usa mesma origem e sessão; cliente controla 401/403, falha de rede e reaquisição de CSRF sem repetir automaticamente operações de compra futuras.
- Rótulos acessíveis, foco visível, preenchimento apropriado de autocomplete e uso em 360/1280 px.
- Carrinho ainda não existe: preservar sua retomada no planejamento, sem implementar carrinho nesta etapa.

## Testes de aceite

| ID | Cenário | Resultado esperado |
| --- | --- | --- |
| AUTH01 | Cadastro válido e consulta no PostgreSQL de teste | 201, CLIENTE, campos corretos e hash distinto da senha; encoder verifica senha |
| AUTH02 | E-mail repetido, incluindo caixa/espaços nas extremidades | 409 e apenas um usuário; restrição única presente no banco |
| AUTH03 | Campos inválidos, limites de senha, JSON malformado ou perfil enviado | 400, nenhuma conta indevida e nenhum segredo exposto |
| AUTH04 | Login válido usando normalização de e-mail | 200; cookie permite consultar sessão em requisição posterior; id de sessão renovado |
| AUTH05 | Login com senha errada ou e-mail inexistente | Mesmo status e mensagem genérica; sem sessão autenticada |
| AUTH06 | Consulta de sessão como visitante | 401 JSON, sem redirecionamento para HTML |
| AUTH07 | POST sem token ou com token inválido | 403; cadastro/login/logout não executados; token válido libera fluxo |
| AUTH08 | Login seguido de novo token e logout | 204; sessão anterior não acessa recursos; novo login é possível |
| AUTH09 | Cliente tenta rota administrativa | 403; teste distingue visitante 401 e autoridade ADMIN permitida pela regra |
| AUTH10 | Reinício e novo login da conta cadastrada | Conta persiste; não exigir persistência de sessões entre reinícios |
| AUTH11 | Catálogo e detalhes após Spring Security | Continuam públicos; testes anteriores afetados passam |
| AUTH12 | Formulários reais e sessão no navegador | Cadastro, login, recarga, saída, mensagens, teclado e responsividade funcionam |

Usar dados fictícios, PostgreSQL cupcakes_test com proteção efetiva já existente e migrações V1/V2. Não limpar cupcakes. Atualizar os testes da proteção de migração apenas quando necessário, mantendo rejeição de banco errado. Testes de auth devem exercitar filtros reais relevantes, sem desabilitar a segurança para fazê-los passar. Senhas fictícias de testes não são credenciais reais.

## Conclusão e evidências

Registrar casos executados, resultados, ambiente e capturas sem data no nome, por exemplo HU03_AUTH01_cadastro-sucesso.png e HU05_AUTH08_logout.png. Não marcar verificação como aprovada sem executar. Nenhum resultado desta SPEC foi executado nesta etapa de preparação.

Antes de implementar, revisar handlers existentes, dados atuais e status Git. Ao final, atualizar documentação desta fatia, informar alterações e pendências, sem commit automático ou publicação. Persistência do catálogo já foi aprovada pelo responsável; incorporar as coletas existentes ao registro, sem repetir o teste desnecessariamente.

## Fontes para implementação

- https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/form.html
- https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html
- https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html

Consultar a versão compatível com o Spring Security gerenciado pelo Boot do projeto. As fontes explicam os mecanismos; os contratos e limites acima são decisões desta SPEC.
