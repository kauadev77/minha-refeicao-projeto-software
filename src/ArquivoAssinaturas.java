/* Kaua de Jesus Soares - 10254229; Luiz Augusto de Sa Alves Ribeiro - 10322522. */
import java.io.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

/** Persistencia textual de um agregado por arquivo; substituicao atomica quando suportada. */
public class ArquivoAssinaturas {
    private final Path pasta;
    public ArquivoAssinaturas(Path pasta) { this.pasta = pasta; }
    public void salvar(Assinatura a) {
        Properties p = new Properties();
        p.setProperty("id", a.id().toString());
        p.setProperty("celular", a.assinante().celular());
        p.setProperty("plano.id", a.plano().id());
        p.setProperty("plano.nome", a.plano().nome());
        p.setProperty("plano.quantidade", "" + a.plano().quantidadeRefeicoes());
        p.setProperty("plano.periodicidade", a.plano().periodicidadeEntrega());
        p.setProperty("plano.valor", a.plano().valor().toPlainString());
        p.setProperty("preferencia", a.preferencia().name());
        p.setProperty("status", a.status().name());
        p.setProperty("protocolo", a.protocolo());
        p.setProperty("pedido.status", a.pedido().status().name());
        p.setProperty("pagamento.aprovado", "" + a.pagamento().aprovado());
        p.setProperty("pagamento.autorizacao", a.pagamento().autorizacao());
        p.setProperty("pagamento.valor", a.pagamento().valor().toPlainString());
        p.setProperty("entrega.previsao", a.entrega().previsao().toString());
        Endereco e = a.endereco();
        p.setProperty("endereco.rua", e.rua()); p.setProperty("endereco.numero", e.numero());
        p.setProperty("endereco.bairro", e.bairro()); p.setProperty("endereco.cidade", e.cidade());
        p.setProperty("endereco.uf", e.uf()); p.setProperty("endereco.cep", e.cep());
        p.setProperty("itens.total", "" + a.pedido().itens().size());
        for (int i = 0; i < a.pedido().itens().size(); i++) {
            ItemPedido item = a.pedido().itens().get(i); Produto pr = item.produto();
            String k = "item." + i + ".";
            p.setProperty(k + "id", pr.id()); p.setProperty(k + "nome", pr.nome());
            p.setProperty(k + "categoria", pr.categoria().name());
            p.setProperty(k + "vegetariano", "" + pr.vegetariano());
            p.setProperty(k + "semLactose", "" + pr.semLactose());
            p.setProperty(k + "quantidade", "" + item.quantidade());
        }
        Path temporario = null;
        try {
            Files.createDirectories(pasta);
            temporario = Files.createTempFile(pasta, "adesao-", ".tmp");
            try (var out = Files.newOutputStream(temporario)) {
                p.store(out, "Minha Refeicao - dados ficticios da demonstracao academica");
            }
            Path destino = pasta.resolve(a.id() + ".txt");
            try { Files.move(temporario, destino, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) { throw new UncheckedIOException("Nao foi possivel persistir a assinatura.", ex); }
        finally {
            if (temporario != null) try { Files.deleteIfExists(temporario); } catch (IOException ignored) {}
        }
    }
    public Assinatura carregar(UUID id) {
        Properties p = new Properties();
        try (var in = Files.newInputStream(pasta.resolve(id + ".txt"))) { p.load(in); }
        catch (IOException ex) { throw new UncheckedIOException("Nao foi possivel ler a assinatura.", ex); }
        PlanoRefeicoes plano = new PlanoRefeicoes(p.getProperty("plano.id"), p.getProperty("plano.nome"),
            Integer.parseInt(p.getProperty("plano.quantidade")), p.getProperty("plano.periodicidade"), new BigDecimal(p.getProperty("plano.valor")));
        Endereco e = new Endereco(p.getProperty("endereco.rua"), p.getProperty("endereco.numero"),
            p.getProperty("endereco.bairro"), p.getProperty("endereco.cidade"), p.getProperty("endereco.uf"), p.getProperty("endereco.cep"));
        List<ItemPedido> itens = new ArrayList<>();
        for (int i = 0; i < Integer.parseInt(p.getProperty("itens.total")); i++) {
            String k = "item." + i + ".";
            Produto pr = new Produto(p.getProperty(k + "id"), p.getProperty(k + "nome"),
                Categoria.valueOf(p.getProperty(k + "categoria")), Boolean.parseBoolean(p.getProperty(k + "vegetariano")),
                Boolean.parseBoolean(p.getProperty(k + "semLactose")));
            itens.add(new ItemPedido(pr, Integer.parseInt(p.getProperty(k + "quantidade"))));
        }
        return new Assinatura(UUID.fromString(p.getProperty("id")), new Assinante(p.getProperty("celular")), plano,
            Preferencia.valueOf(p.getProperty("preferencia")), new Pedido(itens, StatusPedido.valueOf(p.getProperty("pedido.status"))),
            e, new Entrega(LocalDate.parse(p.getProperty("entrega.previsao"))),
            new Pagamento(new BigDecimal(p.getProperty("pagamento.valor")), Boolean.parseBoolean(p.getProperty("pagamento.aprovado")),
                p.getProperty("pagamento.autorizacao")), StatusAssinatura.valueOf(p.getProperty("status")), p.getProperty("protocolo"));
    }
}
