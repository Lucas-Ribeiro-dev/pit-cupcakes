# Design e requisitos consolidados

Este documento reúne os requisitos de produto e as decisões visuais que orientaram a implementação final do PIT Cupcakes. Os identificadores históricos (`HU`, `ADR`, `FRT`, `CRT`, `CHK`, `PED` e `ADM`) permanecem nos testes, evidências e registros de verificação.

## Escopo funcional consolidado

- **Catálogo e detalhes:** catálogo público ordenado por ID, preços calculados pelo servidor, disponibilidade textual, imagem local ou substituta, estados de carregamento, vazio, falha recuperável e produto inexistente.
- **Identidade e sessão:** cadastro de cliente, e-mail normalizado, senha BCrypt, sessão renovada no login, CSRF, logout, limite de tentativas e permissões de cliente e administrador.
- **Endereços e frete:** endereços vinculados ao proprietário, ViaCEP opcional com fallback manual, controle otimista de versão, entrega por faixa ativa e retirada gratuita.
- **Carrinho:** estado em `HttpSession`, quantidade entre 1 e 99 por item, remoção idempotente, subtotal recalculado no servidor, preservação no login e limpeza somente após pedido aprovado.
- **Checkout e pedidos:** revisão temporária, revalidação transacional, idempotência, pagamentos acadêmicos simulados `APROVADO` e `RECUSADO`, histórico congelado e acompanhamento do status.
- **Administração:** catálogo e atendimento protegidos por `ROLE_ADMIN`, concorrência otimista, transições de status por modalidade e bloqueio de estados finais.

## Sistema visual

| Função | Valor |
| --- | --- |
| Cor principal, títulos e ações | Raspberry Pink `#BC164D` |
| Superfície suave e seleções | Pale Pink `#FFD0DF` |
| Acento pequeno de marca | Butter Yellow `#FFE164` |
| Fundo da página | `#FFF9FB` |
| Painéis e campos | `#FFFFFF` |
| Texto principal | `#301523` |
| Texto secundário | `#725763` |
| Títulos e preços | Lilita One, peso regular |
| Texto, rótulos e controles | Varela Round, peso regular |

Os tokens e componentes reais estão em [`tokens.css`](../../src/main/resources/static/assets/css/tokens.css), [`components.css`](../../src/main/resources/static/assets/css/components.css) e [`style.css`](../../src/main/resources/static/assets/css/style.css). A interface mantém margens mínimas de 16 px em celular, largura de leitura aproximada de 1120–1200 px em desktop, foco visível, controles nomeados e ausência de rolagem horizontal nos tamanhos verificados.

## Composição por tela

- **Página inicial:** hero com texto real separado da imagem, catálogo em uma coluna no celular e múltiplas colunas em desktop, ações de conta e carrinho sempre acessíveis.
- **Detalhes:** imagem e informações em duas colunas no desktop e empilhadas no celular; quantidade com diminuir, campo numérico e aumentar; inclusão real no carrinho e fallback preservado.
- **Login e cadastro:** formulários existentes preservados, orientação curta de senha, navegação secundária estilizada e associação acessível entre rótulos e campos.
- **Carrinho:** itens e resumo em painéis distintos, controle compacto de quantidade, atualização explícita, remoção e ação de revisão dependente da sessão.
- **Checkout:** etapas de modalidade, endereço, revisão e pagamento simulado visualmente separadas; texto de `APROVADO` e `RECUSADO` agrupado para leitura em telas estreitas.
- **Endereços:** endereços salvos e formulário de edição em duas colunas quando houver espaço, empilhados no celular, sem conceito de endereço padrão.
- **Minha conta, pedidos e administração:** cartões e seções com hierarquia consistente, estados de permissão explícitos, atendimento administrativo legível em 360 px e 1280 px.

## Assets e referências visuais

Os assets efetivos estão em [`src/main/resources/static/assets`](../../src/main/resources/static/assets), incluindo as fontes licenciadas, logo, hero, seta e imagens WebP dos produtos. As imagens abaixo são prévias visuais de design, preservadas para consulta acadêmica; não são capturas da aplicação nem evidências de teste integrado:

- [Prévia do design system em 1280 px](previas-visuais/preview-1280.png)
- [Prévia do design system em 360 px](previas-visuais/preview-360.png)

As prévias não substituem as evidências reais em [`docs/testes/evidencias`](../testes/evidencias/) nem os resultados estruturados em [`docs/testes/resultados`](../testes/resultados/). O acabamento final foi validado pelas capturas da rodada de navegador registrada no relatório de entrega.

## Limitações e decisões preservadas

- O pagamento permanece simulado; não há adquirente, PIX ou cartão real.
- As faixas de frete de demonstração são acadêmicas e não representam cobertura comercial de produção.
- O carrinho e o limitador de login usam memória de instância única; escala distribuída exigiria armazenamento compartilhado.
- Não foram adicionados filtros de catálogo, prazo de entrega, edição de perfil ou recursos de pagamento real.

## Rastreabilidade

Os contratos funcionais detalhados estão consolidados em [`histórias e casos de uso`](../requisitos/historias-e-casos-de-uso.md), [`visão arquitetural`](../arquitetura/visao-geral.md), [`dicionário de dados`](../banco-de-dados/dicionario-dados.md), [`manual de uso e publicação`](../manual/manual-usuario-e-publicacao.md) e nos registros de teste. As decisões técnicas individuais permanecem em [`docs/decisoes`](../decisoes/).
