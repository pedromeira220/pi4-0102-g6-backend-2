package br.edu.pi4.backend2;

import java.io.IOException;
import java.net.Socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SupervisoraDeConexao extends Thread {
    private static final Logger log = LoggerFactory.getLogger(SupervisoraDeConexao.class);

    private final Socket socket;
    private final ObjectMapper json;
    private final Validator validator;
    private final NotificacaoService notificacoes;
    private final Runnable aoEncerrar;

    SupervisoraDeConexao(Socket socket, ObjectMapper json, Validator validator,
                         NotificacaoService notificacoes, Runnable aoEncerrar) {
        super("conexao-tcp-" + socket.getPort());
        this.socket = socket;
        this.json = json;
        this.validator = validator;
        this.notificacoes = notificacoes;
        this.aoEncerrar = aoEncerrar;
    }

    @Override
    public void run() {
        try (socket; Parceiro parceiro = new Parceiro(socket)) {
            String mensagem;
            while ((mensagem = parceiro.receberMensagem()) != null) {
                processar(mensagem);
            }
        } catch (IOException e) {
            log.debug("Conexão TCP encerrada", e);
        } finally {
            aoEncerrar.run();
        }
    }

    private void processar(String mensagem) {
        try {
            AtualizacaoPedido pedido = json.readValue(mensagem, AtualizacaoPedido.class);
            if (pedido == null || !validator.validate(pedido).isEmpty()) {
                log.warn("Atualização TCP descartada: campos inválidos");
                return;
            }
            notificacoes.notificar(pedido);
        } catch (IOException | IllegalArgumentException e) {
            log.warn("Atualização TCP descartada: JSON inválido");
        }
    }
}
