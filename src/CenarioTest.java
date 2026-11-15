/* Kaua de Jesus Soares - 10254229; Luiz Augusto de Sa Alves Ribeiro - 10322522. */
import java.nio.file.*;
import java.util.*;

/** Testes de comportamento sem bibliotecas externas. Execute com java -cp out CenarioTest. */
public class CenarioTest {
    static int verificacoes;
    static void exigir(boolean condicao, String mensagem) {
        verificacoes++;
        if (!condicao) throw new AssertionError(mensagem);
    }
    static void rejeitar(Runnable acao, String mensagem) {
        try { acao.run(); } catch (IllegalArgumentException | IllegalStateException e) {
            verificacoes++; return;
        }
        throw new AssertionError(mensagem);
    }
    static AssinaturaController novo(Path pasta) {
        return new AssinaturaController(new Catalogo(),
            new ProcessadorSMS(() -> "123456", s -> {}),
            new OperadoraCartaoSimulada(), new ArquivoAssinaturas(pasta));
    }
    static void selecionar(AssinaturaController c) {
        c.iniciarAssinatura("11999990000");
        exigir(c.confirmarCodigo("123456"), "codigo valido deve liberar planos");
        c.confirmarPlano("SEMANAL");
        c.registrarPreferencias(Preferencia.VEGETARIANA_SEM_LACTOSE);
        c.confirmarItens(Categoria.PRINCIPAL, Map.of("P2", 5));
        c.confirmarItens(Categoria.ACOMPANHAMENTO, Map.of("A1", 5));
        c.confirmarItens(Categoria.SOBREMESA, Map.of("S1", 5));
        c.confirmarSelecao();
    }
    public static void main(String[] args) throws Exception {
        Path pasta = Files.createTempDirectory("refeicao-teste-");
        try {
            AssinaturaController c = novo(pasta);
            rejeitar(() -> c.confirmarPlano("SEMANAL"), "nao pode escolher plano sem validar SMS");
            c.iniciarAssinatura("11999990000");
            exigir(!c.confirmarCodigo("000000"), "FA1: codigo errado deve falhar");
            exigir(!c.confirmarCodigo("000000"), "FA1: segunda tentativa deve falhar");
            exigir(!c.confirmarCodigo("000000"), "FA1: terceira tentativa deve falhar");
            exigir(c.tentativasRestantes() == 0, "FA1: tres erros bloqueiam codigo");
            exigir(!c.confirmarCodigo("123456"), "FA1: codigo bloqueado nao deve ser reutilizado");
            c.iniciarAssinatura("11999990000");
            exigir(c.confirmarCodigo("123456"), "novo codigo deve liberar fluxo");
            c.confirmarPlano("SEMANAL");
            c.registrarPreferencias(Preferencia.VEGETARIANA_SEM_LACTOSE);
            rejeitar(() -> c.confirmarItens(Categoria.PRINCIPAL, Map.of("P1", 1)), "carne viola preferencia vegetariana");
            rejeitar(() -> c.confirmarItens(Categoria.SOBREMESA, Map.of("S2", 1)), "lactose viola preferencia");
            rejeitar(() -> c.confirmarItens(Categoria.PRINCIPAL, Map.of("A1", 1)), "categoria incorreta deve falhar");
            rejeitar(() -> c.confirmarItens(Categoria.PRINCIPAL, Map.of("P2", -1)), "quantidade negativa deve falhar");
            c.confirmarItens(Categoria.PRINCIPAL, Map.of("P2", 6));
            rejeitar(c::confirmarSelecao, "FA2: seis refeicoes excedem plano de cinco");
            c.confirmarItens(Categoria.PRINCIPAL, Map.of("P2", 5));
            c.confirmarSelecao();
            rejeitar(() -> c.confirmarEndereco(new Endereco("", "1", "Centro", "Sao Paulo", "SP", "01001000")), "endereco incompleto deve falhar");
            c.confirmarEndereco(new Endereco("Rua Exemplo", "10", "Centro", "Sao Paulo", "SP", "01001000"));
            exigir(c.assinatura().status() == StatusAssinatura.AGUARDANDO_PAGAMENTO, "endereco gera adesao pendente");
            UUID id = c.assinatura().id();
            exigir(new ArquivoAssinaturas(pasta).carregar(id).status() == StatusAssinatura.AGUARDANDO_PAGAMENTO, "adesao pendente deve persistir");
            exigir(!c.autorizarPagamento("RECUSAR"), "FA3: operadora deve recusar token de teste");
            exigir(c.assinatura().status() == StatusAssinatura.AGUARDANDO_PAGAMENTO, "FA3: recusa nao ativa assinatura");
            exigir(c.assinatura().pedido().status() == StatusPedido.PENDENTE, "FA3: recusa nao aprova pedido");
            exigir(c.autorizarPagamento("APROVAR"), "nova tentativa aprovada deve concluir");
            Assinatura lida = new ArquivoAssinaturas(pasta).carregar(id);
            exigir(lida.status() == StatusAssinatura.ATIVA, "assinatura ativa deve sobreviver a recarga");
            exigir(lida.pedido().status() == StatusPedido.APROVADO, "pedido aprovado deve persistir");
            exigir(lida.pedido().contarRefeicoes() == 5, "quantidade deve persistir");
            exigir(lida.pagamento().valor().toPlainString().equals("125.00"), "valor do plano deve persistir exatamente");
            exigir(!lida.protocolo().isBlank(), "confirmacao deve ter protocolo");
            exigir(lida.endereco().numero().equals("10"), "endereco deve persistir");
            exigir(lida.entrega().previsao().isAfter(java.time.LocalDate.now()), "primeira entrega deve estar no futuro");
            rejeitar(() -> c.autorizarPagamento("APROVAR"), "pagamento duplicado deve ser bloqueado");
            for (Path p : Files.list(pasta).toList()) {
                String texto = Files.readString(p);
                exigir(!texto.contains("123456") && !texto.contains("APROVAR"), "codigo SMS e token nao devem ser persistidos");
            }
            AssinaturaController completa = novo(pasta);
            selecionar(completa);
            completa.confirmarEndereco(new Endereco("Rua Teste", "20", "Centro", "Sao Paulo", "SP", "01001000"));
            exigir(completa.autorizarPagamento("APROVAR"), "fluxo com todas categorias deve funcionar");
            exigir(completa.assinatura().pedido().itens().size() == 3, "principal, acompanhamento e sobremesa devem persistir");
            System.out.println("OK - " + verificacoes + " verificacoes: fluxo principal, FA1, FA2, FA3 e persistencia.");
        } finally {
            try (var arquivos = Files.list(pasta)) {
                for (Path p : arquivos.toList()) Files.delete(p);
            }
            Files.delete(pasta);
        }
    }
}
