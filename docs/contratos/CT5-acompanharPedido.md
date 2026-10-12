# CT5: acompanharPedido

| Linha | Valor |
|---|---|
| Fronteira | WebSocket |
| Quem chama | Frontend, depois que consultarPedido (CT1) encontrou o pedido |
| Quem responde | Backend 2 |
| Atende | E3 |
| Como chamar | Conexão WebSocket em `/ws` e inscrição STOMP (`SUBSCRIBE`) no canal `/topic/pedidos/{senha}` |

## Entrada

| Campo | Tipo | Obrigatório | Exemplo |
|---|---|---|---|
| senha | inteiro | sim | 42 |

## Saída

Não tem. As mudanças do pedido chegam depois, pela mensagem etapaAlterada (CT6).

## Erros

| Quando | O que volta |
|---|---|
| A conexão cai, por exemplo quando o cliente bloqueia o celular | O Frontend reconecta, refaz a inscrição e busca a etapa atual com consultarPedido (CT1). |
| O Backend 2 está fora do ar | A conexão é recusada. A página mostra a etapa obtida por consultarPedido (CT1) e não se atualiza sozinha. |
