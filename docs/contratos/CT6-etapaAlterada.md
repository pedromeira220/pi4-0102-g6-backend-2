# CT6: etapaAlterada

| Linha | Valor |
|---|---|
| Fronteira | WebSocket |
| Quem chama | Backend 2, sempre que recebe etapaGravada (CT4) do Backend 1 |
| Quem responde | Frontend, em todas as páginas inscritas no canal do pedido. Não há resposta. |
| Atende | E3 e E5 |
| Como chamar | Canal `/topic/pedidos/{senha}`, mensagem do tipo etapaAlterada |

## Entrada

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| senha | inteiro | sim | 42 |
| etapa | um de: recebido, em_preparo, pronto, encerrado | sim | em_preparo |
| estimativaMinutos | inteiro | não (vem só nas etapas recebido e em_preparo) | 8 |
| atualizadoEm | data e hora | sim | 2026-10-13T19:20:00 |

## Saída

Não tem.

## Erros

| Quando | O que volta |
|---|---|
| A página estava fechada na hora do envio | Nada. Ao abrir, a página busca a etapa atual com consultarPedido (CT1). |
