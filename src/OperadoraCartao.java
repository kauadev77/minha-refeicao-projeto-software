/* Kaua de Jesus Soares - 10254229; Luiz Augusto de Sa Alves Ribeiro - 10322522. */
import java.math.BigDecimal;
import java.util.UUID;

public interface OperadoraCartao {
    record Resultado(boolean aprovado, String autorizacao) {}
    Resultado autorizar(BigDecimal valor, String token);
}
/** Simulador local, sem captura de cartao real e sem cobranca. */
class OperadoraCartaoSimulada implements OperadoraCartao {
    public Resultado autorizar(BigDecimal valor, String token) {
        if (valor.signum() <= 0) throw new IllegalArgumentException("Valor deve ser positivo.");
        return "APROVAR".equals(token)
            ? new Resultado(true, "SIM-" + UUID.randomUUID()) : new Resultado(false, "");
    }
}
