/* Kaua de Jesus Soares - 10254229; Luiz Augusto de Sa Alves Ribeiro - 10322522. */
import java.math.BigDecimal;
import java.util.*;

/** Colecoes de planos e produtos. Dados ficticios para a demonstracao academica. */
public class Catalogo {
    private final List<PlanoRefeicoes> planos = List.of(
        new PlanoRefeicoes("SEMANAL", "Plano semanal 5", 5, "Semanal", new BigDecimal("125.00")),
        new PlanoRefeicoes("QUINZENAL", "Plano quinzenal 10", 10, "Quinzenal", new BigDecimal("230.00")),
        new PlanoRefeicoes("MENSAL", "Plano mensal 20", 20, "Mensal", new BigDecimal("420.00")));
    private final List<Produto> produtos = List.of(
        new Produto("P1", "Frango grelhado", Categoria.PRINCIPAL, false, true),
        new Produto("P2", "Lentilhas com legumes", Categoria.PRINCIPAL, true, true),
        new Produto("P3", "Lasanha de queijo", Categoria.PRINCIPAL, true, false),
        new Produto("A1", "Arroz integral", Categoria.ACOMPANHAMENTO, true, true),
        new Produto("A2", "Legumes ao vapor", Categoria.ACOMPANHAMENTO, true, true),
        new Produto("S1", "Salada de frutas", Categoria.SOBREMESA, true, true),
        new Produto("S2", "Pudim de leite", Categoria.SOBREMESA, true, false));
    public List<PlanoRefeicoes> listarPlanos() { return planos; }
    public PlanoRefeicoes buscarPlano(String id) {
        return planos.stream().filter(p -> p.id().equals(id)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Plano inexistente."));
    }
    public Produto buscarProduto(String id) {
        return produtos.stream().filter(p -> p.id().equals(id)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Produto inexistente: " + id));
    }
    public List<Produto> listar(Categoria categoria, Preferencia preferencia) {
        return produtos.stream().filter(p -> p.categoria() == categoria && preferencia.aceita(p)).toList();
    }
}
