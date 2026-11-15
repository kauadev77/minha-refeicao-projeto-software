/* Kaua de Jesus Soares - 10254229; Luiz Augusto de Sa Alves Ribeiro - 10322522. */
import java.util.function.*;

/** Porta para envio de SMS. A aplicacao de demonstracao injeta um envio local simulado. */
public class ProcessadorSMS {
    private final Supplier<String> gerador;
    private final Consumer<String> envio;
    private String codigo;
    private int tentativas;
    public ProcessadorSMS(Supplier<String> gerador, Consumer<String> envio) {
        this.gerador = gerador; this.envio = envio;
    }
    public void enviarCodigo(String celular) {
        new Assinante(celular);
        codigo = gerador.get(); tentativas = 3;
        envio.accept("[SMS SIMULADO para " + celular + "] Codigo: " + codigo);
    }
    public boolean validarCodigo(String informado) {
        if (codigo == null || tentativas == 0) return false;
        if (codigo.equals(informado)) { codigo = null; tentativas = 0; return true; }
        tentativas--;
        if (tentativas == 0) codigo = null;
        return false;
    }
    public int tentativasRestantes() { return tentativas; }
}
