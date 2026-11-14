# Verificação final para entrega acadêmica — 14/11/2026

## Ambiente e escopo

- Checkout: `master`, com remoto `origin` em `https://github.com/Lucas-Ribeiro-dev/pit-cupcakes.git`. Antes desta rodada, `master` e `origin/master` apontavam para `06d65bd`; o acabamento visual e os arquivos de publicação estavam somente no checkout local.
- Java 21.0.7, Spring Boot 3.4.2, Maven Wrapper 3.9.9; testes rápidos com H2/MockMvc e integrações com PostgreSQL 18.6 exclusivamente em `cupcakes_test`.
- O guard de integração validou URL, perfil, credenciais, `current_database()` e `ddl-auto=validate` antes do acesso. Flyway confirmou quatro migrações válidas, com schema na versão 4. Nenhum teste usou o banco `cupcakes`.
- Navegador Microsoft Edge headless, com verificações em 1280 px e 360 px e capturas finais em [final-20261114-1500](../evidencias/final-20261114-1500/). Capturas e resultados das rodadas anteriores foram preservados.

## Resultados efetivos

| Rodada | Resultado | Cobertura |
| --- | --- | --- |
| H2/MockMvc, 12 suítes | 64 testes; 0 falhas, 0 erros, 0 ignorados | Catálogo, cadastro e sessão, endereços, carrinho, entrega e retirada, revisão, pagamentos simulados, histórico, permissões e administração. |
| PostgreSQL `cupcakes_test`, 7 suítes | 15 testes; 0 falhas, 0 erros, 0 ignorados | Persistência, migrações, reinício, frete, carrinho, pedidos e administração. |
| [Fluxo completo no navegador](fluxo-completo-navegador-final-20261114-1500.json) | 28 cenários aprovados; 0 erros JavaScript | Catálogo, cadastro/login/logout, endereços, carrinho, entrega, revisão, recusa e aprovação simuladas, histórico e administração. Capturas em desktop e celular. |

Os 79 testes Java são a soma das duas execuções independentes. O navegador exerceu o caminho de entrega; a retirada e suas transições de atendimento foram verificadas nas suítes de API e PostgreSQL. A consulta de CEP no navegador usou resposta controlada para manter o teste independente do serviço externo.

## Falhas encontradas e correções

1. A primeira suíte H2 falhou em `DemoDataLoaderTest`: a expectativa antiga exigia um cupcake sem imagem. O teste foi ajustado para exigir quatro imagens WebP existentes; a reexecução de 64 testes passou.
2. A primeira tentativa do navegador encontrou dois erros JavaScript `FALLBACK_IMAGE_PATH is not defined` no catálogo e carrinho. As duas páginas agora importam a constante compartilhada. A rodada final registrou zero erros JavaScript.
3. O roteiro de navegador usava controles antigos do carrinho e uma opção SVG removida do cadastro administrativo. Os seletores foram atualizados para a interface final. O roteiro passou a aceitar `--run-id`, gravando resultados e capturas em caminhos exclusivos e recusando sobrescrever um resultado existente.
4. A inspeção da revisão em 360 px revelou quebra excessiva no texto das opções de pagamento. A descrição foi agrupada em cada opção; uma nova rodada completa passou e a captura final mostra o texto legível.

As tentativas parciais estão em [primeira tentativa](fluxo-completo-navegador-final-20261114-1437.json) e [segunda tentativa](fluxo-completo-navegador-final-20261114-1440.json). A [terceira rodada](fluxo-completo-navegador-final-20261114-1444.json) passou antes do ajuste visual; a rodada final supersede as três, sem apagá-las.

## Revisão de entrega

- As capturas de catálogo, revisão de entrega e atendimento administrativo em 360 px e catálogo em 1280 px foram inspecionadas. Os controles permaneceram acessíveis e não houve rolagem horizontal nas verificações automatizadas.
- O catálogo do banco isolado contém produtos de fixture, incluindo um produto sem imagem para exercitar o substituto visual. Esse conteúdo não representa o catálogo de produção.
- A revisão dos arquivos destinados ao GitHub encontrou placeholders em `.env.example` e referências a variáveis de ambiente, sem valores de credenciais locais. Arquivos `.env*` reais, material local nominal, `target/`, `.venv/`, chaves e contexto operacional privado permanecem ignorados.
- Os arquivos locais de SPECs e ferramentas operacionais foram preservados no computador, mas retirados do índice do Git. Não houve publicação em hospedagem.

## Pendências

- Publicação em hospedagem e validação de HTTPS/cookies no domínio final pertencem à próxima etapa.
- A disponibilidade e os valores comerciais finais de frete em produção dependem da configuração do ambiente publicado.

## Rastreabilidade

Os contratos funcionais detalhados estão consolidados em [`histórias e casos de uso`](../requisitos/historias-e-casos-de-uso.md), [`visão arquitetural`](../arquitetura/visao-geral.md), [`dicionário de dados`](../banco-de-dados/dicionario-dados.md), [`manual de uso e publicação`](../manual/manual-usuario-e-publicacao.md), [`design e requisitos`](../design/identidade-visual-e-especificacoes.md) e nos registros de teste. As decisões técnicas individuais permanecem em [`docs/decisoes`](../decisoes/).

O `README.md` foi preservado exatamente como solicitado. Por isso, a seção inicial ainda contém os links históricos para `specs/07-SPEC-CATALOGO.md`, `specs/09-SPEC-AUTENTICACAO.md`, `specs/10-SPEC-ENDERECOS-FRETE.md`, `specs/11-SPEC-CARRINHO.md` e `specs/12-SPEC-CONCLUSAO-FUNCIONAL.md`; esses cinco links ficam pendentes para a reformulação posterior do README pelo responsável. Nenhum outro documento final depende de caminho local em `specs/` ou `operacional/`.
