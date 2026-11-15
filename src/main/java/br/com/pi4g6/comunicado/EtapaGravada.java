package br.com.pi4g6.comunicado;

import java.time.LocalDateTime;

public class EtapaGravada extends Comunicado {

    private static final long serialVersionUID = 1L;

    private final int senha;
    private final String etapa;
    private final Integer estimativaMinutos;
    private final Integer posicaoNaFila;
    private final LocalDateTime atualizadoEm;

    public EtapaGravada(int senha, String etapa, Integer estimativaMinutos,
            Integer posicaoNaFila, LocalDateTime atualizadoEm) {
        this.senha = senha;
        this.etapa = etapa;
        this.estimativaMinutos = estimativaMinutos;
        this.posicaoNaFila = posicaoNaFila;
        this.atualizadoEm = atualizadoEm;
    }

    public int getSenha() {
        return senha;
    }

    public String getEtapa() {
        return etapa;
    }

    public Integer getEstimativaMinutos() {
        return estimativaMinutos;
    }

    public Integer getPosicaoNaFila() {
        return posicaoNaFila;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }
}
