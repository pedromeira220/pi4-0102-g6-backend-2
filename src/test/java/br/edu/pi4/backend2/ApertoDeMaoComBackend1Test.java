package br.edu.pi4.backend2;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "tcp.port=34317")
class ApertoDeMaoComBackend1Test {

    private static final int PORTA_TCP = 34317;

    private static final String LINHA_DO_BACKEND1 =
            "{\"senha\":42,\"etapa\":\"em_preparo\",\"estimativaMinutos\":8,"
                    + "\"posicaoNaFila\":3,\"atualizadoEm\":\"2026-10-13T19:20:00\"}";

    @LocalServerPort
    private int portaHttp;

    @Autowired
    private ObjectMapper json;

    private StompSession conectar() throws Exception {
        WebSocketStompClient cliente = new WebSocketStompClient(new StandardWebSocketClient());
        MappingJackson2MessageConverter conversor = new MappingJackson2MessageConverter();
        conversor.setObjectMapper(json);
        cliente.setMessageConverter(conversor);
        return cliente.connectAsync("ws://localhost:" + portaHttp + "/ws",
                new StompSessionHandlerAdapter() {
                }).get(10, TimeUnit.SECONDS);
    }

    private void mandarPeloSocket(String linha) throws Exception {
        try (Socket socket = new Socket("localhost", PORTA_TCP);
                Writer saida = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)) {
            saida.write(linha + "\n");
            saida.flush();
        }
    }

    @Test
    void oJsonDoBackend1ChegaNoCanalDoPedido() throws Exception {
        StompSession sessao = conectar();
        CompletableFuture<AtualizacaoPedido> recebido = new CompletableFuture<>();

        sessao.subscribe("/topic/pedidos/42", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders cabecalhos) {
                return AtualizacaoPedido.class;
            }

            @Override
            public void handleFrame(StompHeaders cabecalhos, Object conteudo) {
                recebido.complete((AtualizacaoPedido) conteudo);
            }
        });

        Thread.sleep(500);
        mandarPeloSocket(LINHA_DO_BACKEND1);

        AtualizacaoPedido aviso = recebido.get(10, TimeUnit.SECONDS);
        assertThat(aviso.senha()).isEqualTo(42);
        assertThat(aviso.etapa()).isEqualTo("em_preparo");
        assertThat(aviso.estimativaMinutos()).isEqualTo(8);
        assertThat(aviso.posicaoNaFila()).isEqualTo(3);
        assertThat(aviso.atualizadoEm()).isEqualTo(LocalDateTime.of(2026, 10, 13, 19, 20, 0));

        sessao.disconnect();
    }

    @Test
    void etapaEmMaiusculaEhDescartadaENaoChegaNaTela() throws Exception {
        StompSession sessao = conectar();
        CompletableFuture<AtualizacaoPedido> recebido = new CompletableFuture<>();

        sessao.subscribe("/topic/pedidos/99", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders cabecalhos) {
                return AtualizacaoPedido.class;
            }

            @Override
            public void handleFrame(StompHeaders cabecalhos, Object conteudo) {
                recebido.complete((AtualizacaoPedido) conteudo);
            }
        });

        Thread.sleep(500);
        mandarPeloSocket("{\"senha\":99,\"etapa\":\"EM_PREPARO\","
                + "\"atualizadoEm\":\"2026-10-13T19:20:00\"}");

        Thread.sleep(1500);
        assertThat(recebido).isNotCompleted();

        sessao.disconnect();
    }

    @Test
    void jsonQuebradoNaoDerrubaOServidor() throws Exception {
        mandarPeloSocket("isso nao e json");

        StompSession sessao = conectar();
        CompletableFuture<AtualizacaoPedido> recebido = new CompletableFuture<>();
        sessao.subscribe("/topic/pedidos/55", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders cabecalhos) {
                return AtualizacaoPedido.class;
            }

            @Override
            public void handleFrame(StompHeaders cabecalhos, Object conteudo) {
                recebido.complete((AtualizacaoPedido) conteudo);
            }
        });

        Thread.sleep(500);
        mandarPeloSocket("{\"senha\":55,\"etapa\":\"recebido\","
                + "\"atualizadoEm\":\"2026-10-13T19:30:00\"}");

        assertThat(recebido.get(10, TimeUnit.SECONDS).senha()).isEqualTo(55);
        sessao.disconnect();
    }

    @Test
    void avisoSemEstimativaEPosicaoTambemChega() throws Exception {
        StompSession sessao = conectar();
        CompletableFuture<AtualizacaoPedido> recebido = new CompletableFuture<>();

        sessao.subscribe("/topic/pedidos/7", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders cabecalhos) {
                return AtualizacaoPedido.class;
            }

            @Override
            public void handleFrame(StompHeaders cabecalhos, Object conteudo) {
                recebido.complete((AtualizacaoPedido) conteudo);
            }
        });

        Thread.sleep(500);
        mandarPeloSocket("{\"senha\":7,\"etapa\":\"pronto\",\"atualizadoEm\":\"2026-10-13T19:25:00\"}");

        AtualizacaoPedido aviso = recebido.get(10, TimeUnit.SECONDS);
        assertThat(aviso.senha()).isEqualTo(7);
        assertThat(aviso.etapa()).isEqualTo("pronto");
        assertThat(aviso.estimativaMinutos()).isNull();
        assertThat(aviso.posicaoNaFila()).isNull();

        sessao.disconnect();
    }
}
