# DT01 — ViaCEP como auxílio de preenchimento

Decisão adotada para a terceira etapa: consultar ViaCEP no servidor e permitir revisão e preenchimento manual. Referência: [documentação oficial](https://viacep.com.br/).

O cliente JDK HTTP usa exclusivamente `https://viacep.com.br/ws/{cep}/json/`, após validação local de oito dígitos. Não segue redirecionamentos, não repete requisições e não compartilha cookies, nome, login, número ou complemento. Conexão limitada a dois segundos; espera total pela resposta completa limitada a quatro segundos, cancelando o futuro ao exceder esse prazo.

Somente CEP, logradouro, bairro, cidade (localidade) e UF são mapeados. `erro=true` produz 404 CEP_NAO_ENCONTRADO. Resposta inválida, erro HTTP e falha de conexão produzem 502 SERVICO_CEP_INDISPONIVEL; demora produz 504 CONSULTA_CEP_EXPIRADA. Mensagens públicas não incluem corpo externo nem detalhes internos.

A interface consulta ao sair do campo válido ou pelo botão Buscar CEP, cancela consultas antigas e verifica CEP, sequência e revisões individuais dos campos antes de preencher. Campo editado durante a consulta é preservado, mesmo que volte ao valor anterior. Salvar e cancelar encerram consultas pendentes. Falhas não bloqueiam cadastro manual válido.

ViaCEP não comprova existência do imóvel, cobertura comercial nem preço. O serviço de frete usa somente endereço persistido da conta, UF DF e faixa ativa local. Brasília–DF é a referência; nomes de regiões administrativas são aceitos. Retirada é gratuita, sem seleção automática. Faixas e valores comerciais finais continuam pendentes.

Os testes do cliente HTTP usam respostas e futuros simulados, inclusive espera incompleta do corpo. Os testes de navegador interceptam o endpoint interno de CEP para tornar falha, demora e resposta atrasada reproduzíveis. Essas simulações não representam consulta real ao ViaCEP; eventual teste real de fumaça deve ser registrado separadamente no [registro da etapa](../testes/resultados/verificacao-terceira-etapa.md).

A verificação concluída no servidor real localhost:8092 manteve autenticação, endereços e frete reais em cupcakes_test. Somente `GET /api/conta/cep/*` foi interceptado no Playwright, incluindo atraso de 700 ms, troca de CEP e respostas 404/502/504. Essa passagem verifica a reação da interface ao auxílio externo controlado; não é uma consulta real ao ViaCEP nem uma nova comprovação do timeout HTTP do servidor. O [relatório do navegador real](../testes/resultados/enderecos-navegador.json) fica separado do [relatório anterior com APIs inteiramente simuladas](../testes/resultados/enderecos-interface-simulada.json).
