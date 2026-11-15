/* Kaua de Jesus Soares - 10254229; Luiz Augusto de Sa Alves Ribeiro - 10322522. */
import java.time.LocalDate;
import java.util.*;

/** Controlador do caso de uso; delega regras aos objetos do dominio e persistencia. */
public class AssinaturaController {
    private enum Etapa { INICIO, CODIGO, PLANO, PREFERENCIA, SELECAO, ENDERECO, PAGAMENTO, CONCLUIDO }
    private Etapa etapa = Etapa.INICIO;
    private final Catalogo catalogo;
    private final ProcessadorSMS sms;
    private final OperadoraCartao operadora;
    private final ArquivoAssinaturas arquivo;
    private Assinante assinante;
    private PlanoRefeicoes plano;
    private Preferencia preferencia;
    private final EnumMap<Categoria, List<ItemPedido>> selecao = new EnumMap<>(Categoria.class);
    private Assinatura assinatura;
    public AssinaturaController(Catalogo catalogo, ProcessadorSMS sms, OperadoraCartao operadora, ArquivoAssinaturas arquivo) {
        this.catalogo = catalogo; this.sms = sms; this.operadora = operadora; this.arquivo = arquivo;
    }
    private void exigir(Etapa esperada) {
        if (etapa != esperada) throw new IllegalStateException("Operacao fora da ordem do caso de uso: " + etapa);
    }
    public void iniciarAssinatura(String celular) {
        if (etapa != Etapa.INICIO && etapa != Etapa.CODIGO)
            throw new IllegalStateException("Assinatura ja iniciada.");
        assinante = new Assinante(celular);
        sms.enviarCodigo(celular); etapa = Etapa.CODIGO;
    }
    public boolean confirmarCodigo(String codigo) {
        exigir(Etapa.CODIGO);
        if (!sms.validarCodigo(codigo)) return false;
        etapa = Etapa.PLANO; return true;
    }
    public int tentativasRestantes() { return sms.tentativasRestantes(); }
    public List<PlanoRefeicoes> listarPlanos() { exigir(Etapa.PLANO); return catalogo.listarPlanos(); }
    public void confirmarPlano(String id) {
        exigir(Etapa.PLANO); plano = catalogo.buscarPlano(id); etapa = Etapa.PREFERENCIA;
    }
    public void registrarPreferencias(Preferencia p) {
        exigir(Etapa.PREFERENCIA); preferencia = Objects.requireNonNull(p); etapa = Etapa.SELECAO;
    }
    public List<Produto> listarProdutos(Categoria categoria) {
        exigir(Etapa.SELECAO); return catalogo.listar(categoria, preferencia);
    }
    public void confirmarItens(Categoria categoria, Map<String, Integer> quantidades) {
        exigir(Etapa.SELECAO);
        List<ItemPedido> itens = new ArrayList<>();
        long total = 0;
        for (var e : new TreeMap<>(quantidades).entrySet()) {
            Produto p = catalogo.buscarProduto(e.getKey());
            if (p.categoria() != categoria || !preferencia.aceita(p))
                throw new IllegalArgumentException("Produto incompativel com categoria ou preferencia.");
            ItemPedido item = new ItemPedido(p, e.getValue());
            total += item.quantidade();
            if (total > Integer.MAX_VALUE) throw new IllegalArgumentException("Quantidade excessiva.");
            itens.add(item);
        }
        selecao.put(categoria, List.copyOf(itens));
    }
    public Pedido resumoPedido() {
        return new Pedido(selecao.values().stream().flatMap(List::stream).toList(), StatusPedido.PENDENTE);
    }
    public void confirmarSelecao() {
        exigir(Etapa.SELECAO);
        Pedido p = resumoPedido();
        if (!plano.validarQuantidade(p.contarRefeicoes()))
            throw new IllegalArgumentException("Selecione de 1 a " + plano.quantidadeRefeicoes() + " refeicoes principais.");
        for (Categoria categoria : List.of(Categoria.ACOMPANHAMENTO, Categoria.SOBREMESA))
            if (p.contar(categoria) > p.contarRefeicoes())
                throw new IllegalArgumentException("Acompanhamentos e sobremesas nao podem superar o numero de refeicoes.");
        etapa = Etapa.ENDERECO;
    }
    public void confirmarEndereco(Endereco endereco) {
        exigir(Etapa.ENDERECO); Objects.requireNonNull(endereco);
        Assinatura pendente = new Assinatura(UUID.randomUUID(), assinante, plano, preferencia,
            resumoPedido(), endereco, new Entrega(LocalDate.now().plusDays(2)),
            new Pagamento(plano.valor(), false, ""), StatusAssinatura.AGUARDANDO_PAGAMENTO, "");
        arquivo.salvar(pendente);
        assinatura = pendente; etapa = Etapa.PAGAMENTO;
    }
    public boolean autorizarPagamento(String token) {
        exigir(Etapa.PAGAMENTO);
        var resultado = operadora.autorizar(assinatura.pagamento().valor(), token);
        if (!resultado.aprovado()) return false;
        Assinatura ativa = assinatura.ativar(resultado.autorizacao());
        arquivo.salvar(ativa);
        assinatura = ativa; etapa = Etapa.CONCLUIDO; return true;
    }
    public Assinatura assinatura() {
        if (assinatura == null) throw new IllegalStateException("Ainda nao existe assinatura persistida.");
        return assinatura;
    }
}
