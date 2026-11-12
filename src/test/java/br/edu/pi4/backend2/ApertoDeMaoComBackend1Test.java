package br.edu.pi4.backend2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import br.com.pi4g6.comunicado.Comunicado;
import br.com.pi4g6.comunicado.EtapaGravada;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ObjectOutputStream;
import java.lang.reflect.Type;
import java.net.Socket;
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
    private static final LocalDateTime INSTANTE = LocalDateTime.of(2026, 10, 13, 19, 20, 0);

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

    private CompletableFuture<AtualizacaoPedido> assinar(StompSession sessao, int senha) {
        CompletableFuture<AtualizacaoPedido> recebido = new CompletableFuture<>();
        sessao.subscribe("/topic/pedidos/" + senha, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders cabecalhos) {
                return AtualizacaoPedido.class;
            }

            @Override
            public void handleFrame(StompHeaders cabecalhos, Object conteudo) {
                recebido.complete((AtualizacaoPedido) conteudo);
            }
        });
        return recebido;
    }

    private void mandarPeloSocket(Object... objetos) throws Exception {
        try (Socket socket = new Socket("localhost", PORTA_TCP);
                ObjectOutputStream saida = new ObjectOutputStream(socket.getOutputStream())) {
            for (Object objeto : objetos) {
                saida.writeObject(objeto);
                saida.flush();
                saida.reset();
            }
        }
    }

    @Test
    void oObjetoSerializadoDoBackend1ChegaNoCanalDoPedido() throws Exception {
        StompSession sessao = conectar();
        CompletableFuture<AtualizacaoPedido> recebido = assinar(sessao, 42);
        Thread.sleep(500);

        mandarPeloSocket(new EtapaGravada(42, "em_preparo", 8, 3, INSTANTE));

        AtualizacaoPedido aviso = recebido.get(10, TimeUnit.SECONDS);
        assertThat(aviso.senha()).isEqualTo(42);
        assertThat(aviso.etapa()).isEqualTo("em_preparo");
        assertThat(aviso.estimativaMinutos()).isEqualTo(8);
        assertThat(aviso.posicaoNaFila()).isEqualTo(3);
        assertThat(aviso.atualizadoEm()).isEqualTo(INSTANTE);

        sessao.disconnect();
    }

    @Test
    void varioAvisosNaMesmaConexaoChegamTodos() throws Exception {
        StompSession sessao = conectar();
        CompletableFuture<AtualizacaoPedido> primeiro = assinar(sessao, 1);
        CompletableFuture<AtualizacaoPedido> terceiro = assinar(sessao, 3);
        Thread.sleep(500);

        mandarPeloSocket(
                new EtapaGravada(1, "recebido", 5, 1, INSTANTE),
                new EtapaGravada(2, "recebido", 7, 2, INSTANTE),
                new EtapaGravada(3, "em_preparo", 3, 3, INSTANTE));

        assertThat(primeiro.get(10, TimeUnit.SECONDS).senha()).isEqualTo(1);
        assertThat(terceiro.get(10, TimeUnit.SECONDS).senha()).isEqualTo(3);

        sessao.disconnect();
    }

    @Test
    void avisoSemEstimativaEPosicaoTambemChega() throws Exception {
        StompSession sessao = conectar();
        CompletableFuture<AtualizacaoPedido> recebido = assinar(sessao, 7);
        Thread.sleep(500);

        mandarPeloSocket(new EtapaGravada(7, "pronto", null, null, INSTANTE));

        AtualizacaoPedido aviso = recebido.get(10, TimeUnit.SECONDS);
        assertThat(aviso.etapa()).isEqualTo("pronto");
        assertThat(aviso.estimativaMinutos()).isNull();
        assertThat(aviso.posicaoNaFila()).isNull();

        sessao.disconnect();
    }

    @Test
    void etapaEmMaiusculaEhDescartadaENaoChegaNaTela() throws Exception {
        StompSession sessao = conectar();
        CompletableFuture<AtualizacaoPedido> recebido = assinar(sessao, 99);
        Thread.sleep(500);

        mandarPeloSocket(new EtapaGravada(99, "EM_PREPARO", null, null, INSTANTE));

        Thread.sleep(1500);
        assertThat(recebido).isNotCompleted();

        sessao.disconnect();
    }

    @Test
    void objetoForaDoContratoNaoDerrubaOServidor() throws Exception {
        assertThatCode(() -> mandarPeloSocket("isso nao e um Comunicado")).doesNotThrowAnyException();

        StompSession sessao = conectar();
        CompletableFuture<AtualizacaoPedido> recebido = assinar(sessao, 55);
        Thread.sleep(500);

        mandarPeloSocket(new EtapaGravada(55, "recebido", 4, 1, INSTANTE));

        assertThat(recebido.get(10, TimeUnit.SECONDS).senha()).isEqualTo(55);
        sessao.disconnect();
    }

    @Test
    void asClassesCompartilhadasTemOMesmoContratoDeSerializacao() {
        assertThat(Comunicado.class.getPackageName()).isEqualTo("br.com.pi4g6.comunicado");
        assertThat(EtapaGravada.class.getPackageName()).isEqualTo("br.com.pi4g6.comunicado");
        assertThat(java.io.Serializable.class).isAssignableFrom(EtapaGravada.class);
        assertThat(java.io.ObjectStreamClass.lookup(EtapaGravada.class).getSerialVersionUID())
                .isEqualTo(1L);
    }
}
