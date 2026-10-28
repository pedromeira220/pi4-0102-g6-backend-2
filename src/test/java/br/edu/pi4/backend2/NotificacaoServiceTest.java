package br.edu.pi4.backend2;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import static org.mockito.Mockito.*;

class NotificacaoServiceTest {
    @Test
    void enviaAtualizacaoSomenteAoCanalDoPedido() {
        SimpMessagingTemplate mensagens = mock(SimpMessagingTemplate.class);
        AtualizacaoPedido pedido = new AtualizacaoPedido(42, "em_preparo", 8, 3,
                LocalDateTime.of(2026, 10, 13, 19, 20));
        new NotificacaoService(mensagens).notificar(pedido);
        verify(mensagens).convertAndSend("/topic/pedidos/42", pedido);
        verifyNoMoreInteractions(mensagens);
    }
}
