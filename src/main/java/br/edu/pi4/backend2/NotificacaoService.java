package br.edu.pi4.backend2;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificacaoService {
    private final SimpMessagingTemplate mensagens;

    public NotificacaoService(SimpMessagingTemplate mensagens) {
        this.mensagens = mensagens;
    }

    public void notificar(AtualizacaoPedido pedido) {
        mensagens.convertAndSend("/topic/pedidos/" + pedido.senha(), pedido);
    }
}
