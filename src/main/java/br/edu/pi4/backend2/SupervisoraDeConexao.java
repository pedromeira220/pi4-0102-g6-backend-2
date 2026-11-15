package br.edu.pi4.backend2;

import br.com.pi4g6.comunicado.Comunicado;
import br.com.pi4g6.comunicado.EtapaGravada;
import java.io.IOException;
import java.net.Socket;

import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SupervisoraDeConexao extends Thread {
    private static final Logger log = LoggerFactory.getLogger(SupervisoraDeConexao.class);

    private final Socket socket;
    private final Validator validator;
    private final NotificacaoService notificacoes;
    private final Runnable aoEncerrar;

    SupervisoraDeConexao(Socket socket, Validator validator,
                         NotificacaoService notificacoes, Runnable aoEncerrar) {
        super("conexao-tcp-" + socket.getPort());
        this.socket = socket;
        this.validator = validator;
        this.notificacoes = notificacoes;
        this.aoEncerrar = aoEncerrar;
    }

    @Override
    public void run() {
        try (socket; Parceiro parceiro = new Parceiro(socket)) {
            Comunicado comunicado;
            while ((comunicado = parceiro.receberComunicado()) != null) {
                processar(comunicado);
            }
        } catch (IOException e) {
            log.debug("Conexão TCP encerrada", e);
        } finally {
            aoEncerrar.run();
        }
    }

    private void processar(Comunicado comunicado) {
        if (!(comunicado instanceof EtapaGravada aviso)) {
            log.warn("Comunicado descartado: tipo {} não previsto no CT4",
                    comunicado.getClass().getSimpleName());
            return;
        }

        AtualizacaoPedido pedido = new AtualizacaoPedido(
                aviso.getSenha(),
                aviso.getEtapa(),
                aviso.getEstimativaMinutos(),
                aviso.getPosicaoNaFila(),
                aviso.getAtualizadoEm());

        if (!validator.validate(pedido).isEmpty()) {
            log.warn("Atualização TCP descartada: campos inválidos");
            return;
        }
        notificacoes.notificar(pedido);
    }
}
