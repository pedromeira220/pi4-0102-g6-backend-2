package br.edu.pi4.backend2;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AtualizacaoPedido(
        @NotNull @PositiveOrZero Integer senha,
        @NotNull @Pattern(regexp = "recebido|em_preparo|pronto|encerrado") String etapa,
        @PositiveOrZero Integer estimativaMinutos,
        @PositiveOrZero Integer posicaoNaFila,
        @NotNull LocalDateTime atualizadoEm) {
}
