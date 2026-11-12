package br.edu.pi4.backend2;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class AceitadoraDeConexao extends Thread implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(AceitadoraDeConexao.class);
    private static final int LIMITE_CONEXOES = 8;
    private static final int TEMPO_LIMITE_MS = 60_000;

    private final ServerSocket servidor;
    private final Set<Socket> conexoes = new HashSet<>();
    private final Validator validator;
    private final NotificacaoService notificacoes;
    private volatile boolean ativa = true;

    AceitadoraDeConexao(int porta, Validator validator,
                        NotificacaoService notificacoes) throws IOException {
        super("servidor-tcp");
        this.servidor = new ServerSocket(porta);
        this.validator = validator;
        this.notificacoes = notificacoes;
        setDaemon(true);
    }

    int porta() {
        return servidor.getLocalPort();
    }

    @Override
    public void run() {
        while (ativa) {
            try {
                encaminhar(servidor.accept());
            } catch (IOException e) {
                if (ativa) log.error("Falha ao aceitar conexão TCP", e);
            }
        }
    }

    private synchronized void encaminhar(Socket socket) throws IOException {
        if (!ativa || conexoes.size() >= LIMITE_CONEXOES) {
            socket.close();
            return;
        }

        try {
            socket.setSoTimeout(TEMPO_LIMITE_MS);
        } catch (IOException e) {
            fechar(socket);
            throw e;
        }

        conexoes.add(socket);
        new SupervisoraDeConexao(socket, validator, notificacoes,
                () -> remover(socket)).start();
    }

    private synchronized void remover(Socket socket) {
        conexoes.remove(socket);
    }

    @Override
    public synchronized void close() {
        ativa = false;
        try {
            servidor.close();
        } catch (IOException e) {
            log.warn("Falha ao fechar servidor TCP", e);
        }
        conexoes.forEach(this::fechar);
        conexoes.clear();
    }

    private void fechar(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            log.warn("Falha ao fechar conexão TCP", e);
        }
    }
}
