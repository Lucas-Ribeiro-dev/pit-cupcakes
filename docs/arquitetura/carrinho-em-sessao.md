# Carrinho em sessão

O carrinho da quarta etapa guarda somente `produtoId → quantidade` em `HttpSession`. Não há tabela, entidade JPA de carrinho, preço congelado ou vínculo persistido com a conta. O servidor consulta o catálogo a cada resposta, calcula os subtotais com `BigDecimal` e marca linhas removidas ou indisponíveis como bloqueadas. Elas permanecem visíveis e removíveis, mas não entram no subtotal.

Mutações da mesma sessão são serializadas no objeto da sessão. O login do Spring Security renova o identificador sem descartar os atributos; o logout invalida a sessão. O prazo configurado é 30 minutos e um reinício pode perder o carrinho. A estratégia de sincronização atende uma instância; implantação distribuída exige rever o armazenamento de sessão e a atomicidade entre nós.

Rotas públicas de carrinho aceitam visitante e cliente. POST, PUT e DELETE exigem CSRF. Rotas de conta e administração mantêm as permissões anteriores. O cliente consulta o estado após erro de rede e não repete automaticamente inclusões, pois uma inclusão anterior pode ter sido processada.

Esta etapa apresenta subtotal de revisão, sem frete ou total final. Preços podem mudar até a futura revisão do pedido.
