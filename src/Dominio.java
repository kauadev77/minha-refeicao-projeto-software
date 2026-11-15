/* Kaua de Jesus Soares - 10254229; Luiz Augusto de Sa Alves Ribeiro - 10322522. */
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

enum Categoria { PRINCIPAL, ACOMPANHAMENTO, SOBREMESA }
enum StatusAssinatura { AGUARDANDO_PAGAMENTO, ATIVA }
enum StatusPedido { PENDENTE, APROVADO }
enum Preferencia {
    TRADICIONAL(false, false), VEGETARIANA(true, false),
    SEM_LACTOSE(false, true), VEGETARIANA_SEM_LACTOSE(true, true);
    private final boolean vegetariana, semLactose;
    Preferencia(boolean v, boolean l) { vegetariana = v; semLactose = l; }
    boolean aceita(Produto p) {
        return (!vegetariana || p.vegetariano()) && (!semLactose || p.semLactose());
    }
}
record Assinante(String celular) {
    Assinante {
        if (celular == null || !celular.matches("[0-9]{10,11}"))
            throw new IllegalArgumentException("Celular deve ter DDD e 10 ou 11 digitos.");
    }
}
record Endereco(String rua, String numero, String bairro, String cidade, String uf, String cep) {
    Endereco {
        for (String campo : new String[]{rua, numero, bairro, cidade, uf, cep})
            if (campo == null || campo.isBlank()) throw new IllegalArgumentException("Preencha todo o endereco.");
        if (!uf.matches("[A-Za-z]{2}") || !cep.matches("[0-9]{8}"))
            throw new IllegalArgumentException("UF deve ter duas letras e CEP oito digitos.");
        uf = uf.toUpperCase(Locale.ROOT);
    }
    @Override public String toString() {
        return rua + ", " + numero + " - " + bairro + ", " + cidade + "/" + uf + " CEP " + cep;
    }
}
record PlanoRefeicoes(String id, String nome, int quantidadeRefeicoes,
                       String periodicidadeEntrega, BigDecimal valor) {
    boolean validarQuantidade(int quantidade) { return quantidade > 0 && quantidade <= quantidadeRefeicoes; }
}
record Produto(String id, String nome, Categoria categoria, boolean vegetariano, boolean semLactose) {}
record ItemPedido(Produto produto, int quantidade) {
    ItemPedido {
        Objects.requireNonNull(produto);
        if (quantidade < 1) throw new IllegalArgumentException("Quantidade deve ser positiva.");
    }
}
record Pedido(List<ItemPedido> itens, StatusPedido status) {
    Pedido { itens = List.copyOf(itens); }
    int contarRefeicoes() {
        return itens.stream().filter(i -> i.produto().categoria() == Categoria.PRINCIPAL)
            .mapToInt(ItemPedido::quantidade).sum();
    }
    int contar(Categoria categoria) {
        return itens.stream().filter(i -> i.produto().categoria() == categoria)
            .mapToInt(ItemPedido::quantidade).sum();
    }
    Pedido aprovar() { return new Pedido(itens, StatusPedido.APROVADO); }
}
record Pagamento(BigDecimal valor, boolean aprovado, String autorizacao) {
    Pagamento registrarAprovacao(String codigo) { return new Pagamento(valor, true, codigo); }
}
record Entrega(LocalDate previsao) {}
record Assinatura(UUID id, Assinante assinante, PlanoRefeicoes plano, Preferencia preferencia,
                  Pedido pedido, Endereco endereco, Entrega entrega, Pagamento pagamento,
                  StatusAssinatura status, String protocolo) {
    Assinatura ativar(String autorizacao) {
        return new Assinatura(id, assinante, plano, preferencia, pedido.aprovar(), endereco,
            entrega, pagamento.registrarAprovacao(autorizacao), StatusAssinatura.ATIVA,
            "MR-" + id.toString().toUpperCase(Locale.ROOT));
    }
    String confirmacao() {
        StringBuilder b = new StringBuilder("Plano: ").append(plano.nome())
            .append("\nPeriodicidade: ").append(plano.periodicidadeEntrega())
            .append("\nPreferencia: ").append(preferencia).append("\nRefeicoes: ").append(pedido.contarRefeicoes());
        for (ItemPedido i : pedido.itens()) b.append("\n  ").append(i.quantidade()).append(" x ").append(i.produto().nome());
        return b.append("\nEndereco: ").append(endereco).append("\nPrimeira entrega: ").append(entrega.previsao())
            .append("\nTotal: R$ ").append(pagamento.valor()).append("\nAssinatura: ").append(status)
            .append("\nPedido: ").append(pedido.status()).append("\nProtocolo: ").append(protocolo).toString();
    }
}
