package br.edu.pi4.backend2;

import br.com.pi4g6.comunicado.Comunicado;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

final class Parceiro implements AutoCloseable {
    private final Socket socket;
    private final ObjectInputStream entrada;

    Parceiro(Socket socket) throws IOException {
        this.socket = socket;
        this.entrada = new ObjectInputStream(socket.getInputStream());
    }

    Comunicado receberComunicado() throws IOException {
        try {
            return (Comunicado) entrada.readObject();
        } catch (EOFException fim) {
            return null;
        } catch (ClassNotFoundException | ClassCastException erro) {
            throw new IOException("Objeto fora do contrato CT4", erro);
        }
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
