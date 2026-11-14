# SPEC UI 009 — Imagens finais de produtos, hero e marca

## Entrega

Pacote de imagens para a loja PIT Cupcakes. Sem código ou alteração na aplicação. Nome confirmado pelo responsável: PIT Cupcakes.

## Arquivos

| Arquivo relativo a assets-finais | Uso | Dimensões |
| --- | --- | --- |
| produtos/cupcake-baunilha.webp | Baunilha Tradicional | 1254 × 1254 |
| produtos/cupcake-chocolate.webp | Chocolate Belga | 1254 × 1254 |
| produtos/cupcake-frutas-vermelhas.webp | Frutas Vermelhas | 1254 × 1254 |
| produtos/cupcake-caramelo.webp | Caramelo Salgado | 1254 × 1254 |
| hero/hero-cupcakes.webp | Hero panorâmico, texto fora da imagem | 2172 × 724 |
| marca/pit-cupcakes-logo.webp | Logo principal com transparência | 1334 × 261 |
| marca/pit-cupcakes-logo.png | Alternativa PNG transparente | 1334 × 261 |

## Produtos

Imagens sintéticas fotorrealistas criadas para o projeto acadêmico, com fundo rosa suave, forminha rosa e iluminação consistente. Não são fotografias de produtos efetivamente fabricados ou evidências de testes.

Associar os arquivos aos sabores reais do catálogo pelos mecanismos existentes. Não fixar identificadores, criar produtos duplicados ou alterar migrações anteriores apenas para trocar uma imagem. Preservar preço, descrição, disponibilidade e fallback. Em particular, a imagem de caramelo não torna o produto disponível.

Usar a mesma proporção quadrada no catálogo e preservar o produto inteiro nos detalhes. Não esticar imagens; evitar cortes na cobertura e na base. Manter texto alternativo adequado ao sabor. Usar esses arquivos como fonte também para miniaturas somente onde os dados atuais suportarem esse recurso.

A substituição da imagem de frutas vermelhas elimina a imagem ausente desse produto, mas não autoriza remover o tratamento genérico de imagem ausente e erro de carregamento.

## Hero

Imagem sem texto, com quatro cupcakes à direita e espaço livre à esquerda. Manter título e demais conteúdos como texto real da página, nunca incorporados ao arquivo.

O fundo fotográfico tem variações naturais: as cores sólidas exatas da marca continuam nos componentes da interface. Não alterar o Raspberry ou a tipografia para tentar reproduzir cores amostradas da foto.

Conferir contraste do título sobre a área livre. Evitar texto sobre os cupcakes e corte excessivo do grupo em desktop. Em celular, priorizar leitura e espaço: posicionar imagem separada do texto ou ocultá-la, conforme o layout aprovado, em vez de comprimir ambos na mesma linha.

## Logo

Marca tipográfica horizontal: PIT em Lilita One regular; Cupcakes em Varela Round regular. Ambos em #BC164D. Pequeno ponto final decorativo em #FFE164. Arquivos renderizados com as fontes originais e fundo transparente; WEBP sem perdas e PNG alternativo. Licenças OFL incluídas na pasta marca.

Usar a logo completa preservando proporção; não esticar, aplicar sombra ou recolorir parcialmente. Não repetir outro ponto amarelo externo se ele já estiver na imagem. Ao substituir o nome textual no cabeçalho, manter nome acessível PIT Cupcakes e destino real da página inicial. Não tornar a mesma marca anunciada duas vezes por texto e imagem redundantes.

Usar sobre branco ou fundo claro com contraste adequado. Esta versão não foi preparada para fundo Raspberry. Não criar automaticamente ícone de favicon cortando letras do wordmark.

## Verificação da implementação

- Imagens carregam no catálogo e detalhes sem deformação e com fallback preservado.
- Preços, disponibilidade, carrinho, rotas e dados não mudam pela troca de assets.
- Logo tem transparência real, tamanho legível e nenhum fundo sólido acidental.
- Hero mantém texto independente, contraste e responsividade.
- Conferir desktop e 360/390 px, além de zoom de 200%.
- Capturas de design e imagens sintéticas não são apresentadas como evidências de avaliações de participantes.

Não fazer publicação, alteração de banco ou limpeza do Git como parte desta entrega de design. A implementação fica com a IA escolhida pelo responsável.
