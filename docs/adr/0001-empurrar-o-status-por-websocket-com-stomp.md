# ADR 0001: empurrar o status por WebSocket com STOMP

**Status:** aceito

**Contexto:** O cliente deixa a página do seu pedido aberta no celular, e a fila de demonstração avança os pedidos de etapa sem ninguém tocar na tela. O critério de sucesso exige que cada mudança de etapa apareça na tela em até 10 s. O serviço HTTP (backend-1) já guarda os pedidos e é quem muda a etapa. O grupo tem cerca de 5 horas por semana, somadas, para construir o sistema inteiro.

**Decisão:** Criar um serviço de tempo real (backend-2) com uma conexão WebSocket com STOMP aberta para a página do cliente. O backend-1 avisa o backend-2 a cada mudança de etapa, e o backend-2 envia a nova etapa para a página.

**Alternativas consideradas:**
- Polling a cada 5 s no backend-1: cumpre o limite de 10 s sem um segundo serviço; descartada porque a mudança chega com até 5 s de atraso e cada página aberta faz uma requisição a cada 5 s, mesmo quando nada mudou.
- WebSocket dentro do próprio backend-1: descartada porque o grupo quis separar o tempo real do cadastro e da consulta de pedidos, para que cada serviço fique simples e escale de forma independente.

**Consequências:**
- Positivas: a mudança de etapa aparece na tela em menos de 1 s, contra o limite de 10 s; a página não faz requisição nenhuma enquanto o pedido não muda; o Spring e o navegador têm suporte nativo a WebSocket com STOMP.
- Negativas: um segundo serviço para construir, testar e subir no Docker Compose, com pouca hora disponível; o backend-1 passa a depender do backend-2, e se o backend-2 cair a tela para de atualizar sem aviso; quando o cliente bloqueia o celular a conexão cai, e a página precisa reconectar e buscar a etapa atual ao voltar.
