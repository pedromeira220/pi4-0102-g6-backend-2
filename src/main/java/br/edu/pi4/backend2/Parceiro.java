package br.edu.pi4.backend2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

final class Parceiro implements AutoCloseable {
    private static final int LIMITE_MENSAGEM = 8192;
    private final Socket socket;
    private final BufferedReader entrada;

    Parceiro(Socket socket) throws IOException {
        this.socket = socket;
        this.entrada = new BufferedReader(new InputStreamReader(
                socket.getInputStream(), StandardCharsets.UTF_8));
    }

    String receberMensagem() throws IOException {
        StringBuilder linha = new StringBuilder();
        int caractere;
        while ((caractere = entrada.read()) != -1) {
            if (caractere == '\n') return linha.toString();
            if (linha.length() >= LIMITE_MENSAGEM) {
                throw new IOException("Mensagem excede " + LIMITE_MENSAGEM + " caracteres");
            }
            linha.append((char) caractere);
        }
        return linha.isEmpty() ? null : linha.toString();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
