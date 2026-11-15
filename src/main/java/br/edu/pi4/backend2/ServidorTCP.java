package br.edu.pi4.backend2;

import java.io.IOException;

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

    private final Validator validator;
    private final NotificacaoService notificacoes;
    private final int porta;
    private AceitadoraDeConexao aceitadora;

    public ServidorTCP(Validator validator, NotificacaoService notificacoes,
                       @Value("${tcp.port}") int porta) {
        this.validator = validator;
        this.notificacoes = notificacoes;
        this.porta = porta;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        aceitadora = new AceitadoraDeConexao(porta, validator, notificacoes);
        aceitadora.start();
        log.info("Servidor TCP escutando na porta {}", aceitadora.porta());
    }

    @PreDestroy
    public void encerrar() {
        if (aceitadora != null) aceitadora.close();
    }
}
