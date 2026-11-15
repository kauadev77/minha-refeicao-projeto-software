/* Kaua de Jesus Soares - 10254229; Luiz Augusto de Sa Alves Ribeiro - 10322522. */
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        Path pasta = Path.of("dados");
        SecureRandom random = new SecureRandom();
        var controller = new AssinaturaController(new Catalogo(),
            new ProcessadorSMS(() -> String.format("%06d", random.nextInt(1_000_000)), System.out::println),
            new OperadoraCartaoSimulada(), new ArquivoAssinaturas(pasta));
        if (args.length == 1 && args[0].equals("--demo")) { demonstrar(); return; }
        if (args.length == 2 && args[0].equals("--consultar")) {
            System.out.println(new ArquivoAssinaturas(pasta).carregar(UUID.fromString(args[1])).confirmacao()); return;
        }
        new TelaAssinatura(controller, new Scanner(System.in)).executar();
    }
    private static void demonstrar() {
        Path pasta = Path.of("dados-demo");
        var c = new AssinaturaController(new Catalogo(), new ProcessadorSMS(() -> "123456", System.out::println),
            new OperadoraCartaoSimulada(), new ArquivoAssinaturas(pasta));
        System.out.println("DEMONSTRACAO AUTOMATICA - dados ficticios, SMS e cartao simulados");
        c.iniciarAssinatura("11999990000");
        for (int i = 1; i <= 3; i++) System.out.println("FA1 tentativa " + i + ": " + c.confirmarCodigo("000000"));
        System.out.println("Tentativas restantes: " + c.tentativasRestantes() + ". Solicitando novo codigo.");
        c.iniciarAssinatura("11999990000");
        System.out.println("Codigo correto: " + c.confirmarCodigo("123456"));
        c.confirmarPlano("SEMANAL"); c.registrarPreferencias(Preferencia.VEGETARIANA_SEM_LACTOSE);
        c.confirmarItens(Categoria.PRINCIPAL, Map.of("P2", 6));
        try { c.confirmarSelecao(); } catch (IllegalArgumentException e) { System.out.println("FA2: " + e.getMessage()); }
        c.confirmarItens(Categoria.PRINCIPAL, Map.of("P2", 5));
        c.confirmarItens(Categoria.ACOMPANHAMENTO, Map.of("A1", 5));
        c.confirmarItens(Categoria.SOBREMESA, Map.of("S1", 5)); c.confirmarSelecao();
        c.confirmarEndereco(new Endereco("Rua Exemplo", "10", "Centro", "Sao Paulo", "SP", "01001000"));
        System.out.println("Endereco confirmado -> " + c.assinatura().status());
        System.out.println("FA3 pagamento recusado: " + c.autorizarPagamento("RECUSAR"));
        System.out.println("Status mantido: " + c.assinatura().status());
        System.out.println("Nova tentativa aprovada: " + c.autorizarPagamento("APROVAR"));
        System.out.println(c.assinatura().confirmacao());
        System.out.println("RECARGA DO ARQUIVO TXT -> " + new ArquivoAssinaturas(pasta).carregar(c.assinatura().id()).status());
    }
}
