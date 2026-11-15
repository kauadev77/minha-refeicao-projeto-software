/* Kaua de Jesus Soares - 10254229; Luiz Augusto de Sa Alves Ribeiro - 10322522. */
import java.util.*;
import java.util.function.Supplier;

/** Objeto fronteira: entrada e exibicao no terminal; regras ficam no controlador/dominio. */
public class TelaAssinatura {
    private final AssinaturaController controller;
    private final Scanner teclado;
    public TelaAssinatura(AssinaturaController controller, Scanner teclado) {
        this.controller = controller; this.teclado = teclado;
    }
    private String ler(String pergunta) {
        System.out.print(pergunta + " ");
        if (!teclado.hasNextLine()) throw new NoSuchElementException("Entrada encerrada.");
        return teclado.nextLine().trim();
    }
    private <T> T repetir(Supplier<T> operacao) {
        for (;;) try { return operacao.get(); }
        catch (IllegalArgumentException | IllegalStateException e) { System.out.println("Ajuste: " + e.getMessage()); }
    }
    public void executar() {
        System.out.println("MINHA REFEICAO - demonstracao academica");
        System.out.println("SMS e pagamento sao simulados. Use dados ficticios; nao informe cartoes reais.");
        try {
            String celular = repetir(() -> {
                String valor = ler("Celular ficticio com DDD (10/11 digitos):");
                controller.iniciarAssinatura(valor); return valor;
            });
            while (!controller.confirmarCodigo(ler("Codigo mostrado no SMS simulado:"))) {
                System.out.println("Codigo invalido. Tentativas restantes: " + controller.tentativasRestantes());
                if (controller.tentativasRestantes() == 0) {
                    if (!ler("Receber novo codigo? S/N:").equalsIgnoreCase("S")) return;
                    controller.iniciarAssinatura(celular);
                }
            }
            for (PlanoRefeicoes p : controller.listarPlanos())
                System.out.println(p.id() + ": " + p.quantidadeRefeicoes() + " refeicoes, entrega " + p.periodicidadeEntrega() + ", R$ " + p.valor());
            repetir(() -> { controller.confirmarPlano(ler("Escolha o identificador do plano:").toUpperCase(Locale.ROOT)); return true; });
            System.out.println("Preferencias: " + Arrays.toString(Preferencia.values()));
            repetir(() -> { controller.registrarPreferencias(Preferencia.valueOf(ler("Preferencia:").toUpperCase(Locale.ROOT))); return true; });
            boolean selecaoConfirmada = false;
            while (!selecaoConfirmada) {
                for (Categoria categoria : Categoria.values()) {
                    System.out.println("\n" + categoria);
                    for (Produto p : controller.listarProdutos(categoria)) System.out.println(p.id() + ": " + p.nome());
                    repetir(() -> {
                        String linha = ler("Itens ID:quantidade separados por virgula (vazio para nenhum):");
                        Map<String,Integer> itens = new LinkedHashMap<>();
                        if (!linha.isBlank()) for (String parte : linha.split(",")) {
                            String[] par = parte.trim().split(":", -1);
                            if (par.length != 2) throw new IllegalArgumentException("Use ID:quantidade.");
                            String id = par[0].trim().toUpperCase(Locale.ROOT);
                            if (itens.putIfAbsent(id, Integer.parseInt(par[1].trim())) != null)
                                throw new IllegalArgumentException("Nao repita um ID; informe sua quantidade total.");
                        }
                        controller.confirmarItens(categoria, itens); return true;
                    });
                }
                System.out.println("\nComposicao do pedido:");
                for (ItemPedido i : controller.resumoPedido().itens()) System.out.println(i.quantidade() + " x " + i.produto().nome());
                if (!ler("Confirmar composicao? S/N:").equalsIgnoreCase("S")) continue;
                try { controller.confirmarSelecao(); selecaoConfirmada = true; }
                catch (IllegalArgumentException e) { System.out.println("FA2: " + e.getMessage() + " Ajuste a selecao."); }
            }
            repetir(() -> {
                Endereco e = new Endereco(ler("Rua:"), ler("Numero:"), ler("Bairro:"), ler("Cidade:"), ler("UF:"), ler("CEP (8 digitos):"));
                System.out.println(e);
                if (!ler("Confirmar endereco? S/N:").equalsIgnoreCase("S")) throw new IllegalArgumentException("Informe novamente.");
                controller.confirmarEndereco(e); return true;
            });
            System.out.println("Total: R$ " + controller.assinatura().pagamento().valor());
            for (;;) {
                String token = ler("Cartao SIMULADO: APROVAR, RECUSAR ou SAIR:").toUpperCase(Locale.ROOT);
                if (token.equals("SAIR")) { System.out.println("Assinatura mantida em AGUARDANDO_PAGAMENTO."); return; }
                if (controller.autorizarPagamento(token)) break;
                System.out.println("FA3: pagamento nao autorizado; informe outro token ou desista.");
            }
            System.out.println("\nAssinatura confirmada!\n" + controller.assinatura().confirmacao());
        } catch (NoSuchElementException e) { System.out.println("Operacao encerrada pelo usuario."); }
    }
}
