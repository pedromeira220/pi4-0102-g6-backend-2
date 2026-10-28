package br.edu.pi4.backend2;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ServidorTCP implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(ServidorTCP.class);
    private final ObjectMapper json;
    private final Validator validator;
    private final NotificacaoService notificacoes;
    private final int porta;
    private final ExecutorService clientes = Executors.newFixedThreadPool(8);
    private final Set<Socket> conexoes = ConcurrentHashMap.newKeySet();
    private volatile boolean ativo;
    private ServerSocket servidor;

    public ServidorTCP(ObjectMapper json, Validator validator, NotificacaoService notificacoes,
                       @Value("${tcp.port}") int porta) {
        this.json = json;
        this.validator = validator;
        this.notificacoes = notificacoes;
        this.porta = porta;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        servidor = new ServerSocket(porta);
        ativo = true;
        Thread escuta = new Thread(this::aceitarConexoes, "servidor-tcp");
        escuta.setDaemon(true);
        escuta.start();
        log.info("Servidor TCP escutando na porta {}", servidor.getLocalPort());
    }

    private void aceitarConexoes() {
        while (ativo) {
            try {
                Socket cliente = servidor.accept();
                cliente.setSoTimeout(60000);
                // Limita conexões abertas; o Backend 1 pode reconectar após inatividade.
                if (conexoes.size() >= 8) {
                    cliente.close();
                    continue;
                }
                conexoes.add(cliente);
                clientes.submit(() -> receber(cliente));
            } catch (IOException e) {
                if (ativo) log.error("Falha ao aceitar conexão TCP", e);
            }
        }
    }

    private void receber(Socket cliente) {
        try (cliente;
             BufferedReader entrada = new BufferedReader(new InputStreamReader(
                     cliente.getInputStream(), StandardCharsets.UTF_8))) {
            String linha;
            while ((linha = lerLinha(entrada)) != null) {
                try {
                    AtualizacaoPedido pedido = json.readValue(linha, AtualizacaoPedido.class);
                    if (pedido == null || !validator.validate(pedido).isEmpty()) {
                        log.warn("Atualização TCP descartada: campos inválidos");
                        continue;
                    }
                    notificacoes.notificar(pedido);
                } catch (IOException | IllegalArgumentException e) {
                    log.warn("Atualização TCP descartada: JSON inválido");
                }
            }
        } catch (IOException e) {
            if (ativo) log.info("Conexão TCP encerrada: {}", e.getMessage());
        } finally {
            conexoes.remove(cliente);
        }
    }

    private String lerLinha(Reader entrada) throws IOException {
        StringBuilder linha = new StringBuilder();
        int caractere;
        while ((caractere = entrada.read()) != -1) {
            if (caractere == '\n') return linha.toString();
            if (linha.length() >= 8192) throw new IOException("Mensagem excede 8192 caracteres");
            linha.append((char) caractere);
        }
        return linha.isEmpty() ? null : linha.toString();
    }

    @PreDestroy
    public void encerrar() throws IOException {
        ativo = false;
        if (servidor != null) servidor.close();
        for (Socket cliente : conexoes) {
            try { cliente.close(); } catch (IOException ignored) { }
        }
        clientes.shutdownNow();
    }
}
