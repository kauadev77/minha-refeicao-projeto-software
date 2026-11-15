# Modelos da implementação — Minha Refeição

Versão de 15/11/2026, correspondente ao código da Entrega 4. Atualiza os modelos da Entrega 3 a partir do feedback da professora: inclui objetos de domínio antes ausentes, separa fluxos alternativos e explicita criação e persistência. Os modelos abaixo descrevem a implementação atual; não representam uma alteração retroativa dos arquivos já avaliados.

## Classes e responsabilidades

```mermaid
classDiagram
class TelaAssinatura {
  <<boundary>>
  +executar()
}
class AssinaturaController {
  <<control>>
  +iniciarAssinatura(celular)
  +confirmarCodigo(codigo) boolean
  +confirmarPlano(id)
  +registrarPreferencias(preferencia)
  +confirmarItens(categoria,quantidades)
  +confirmarSelecao()
  +confirmarEndereco(endereco)
  +autorizarPagamento(token) boolean
}
class Catalogo {
  <<collection>>
  +listarPlanos()
  +buscarPlano(id)
  +buscarProduto(id)
  +listar(categoria,preferencia)
}
class ArquivoAssinaturas {
  <<persistence>>
  +salvar(assinatura)
  +carregar(id) Assinatura
}
class Assinante {
  <<entity>>
  +celular String
}
class PlanoRefeicoes {
  <<entity>>
  +id String
  +quantidadeRefeicoes int
  +periodicidadeEntrega String
  +valor BigDecimal
  +validarQuantidade(qtd) boolean
}
class Produto {
  <<entity>>
  +id String
  +categoria Categoria
  +vegetariano boolean
  +semLactose boolean
}
class Pedido {
  <<entity>>
  +status StatusPedido
  +contarRefeicoes() int
  +aprovar() Pedido
}
class ItemPedido {
  <<entity>>
  +quantidade int
}
class Assinatura {
  <<entity>>
  +id UUID
  +status StatusAssinatura
  +protocolo String
  +ativar(autorizacao) Assinatura
  +confirmacao() String
}
class Endereco {
  <<value object>>
  +rua String
  +numero String
  +bairro String
  +cidade String
  +uf String
  +cep String
}
class Entrega {
  <<entity>>
  +previsao LocalDate
}
class Pagamento {
  <<entity>>
  +valor BigDecimal
  +aprovado boolean
  +autorizacao String
  +registrarAprovacao(codigo) Pagamento
}
class OperadoraCartao {
  <<interface>>
  +autorizar(valor,token) Resultado
}
class ProcessadorSMS {
  <<control>>
  +enviarCodigo(celular)
  +validarCodigo(codigo) boolean
}
TelaAssinatura --> AssinaturaController : delega
AssinaturaController --> Catalogo : consulta
AssinaturaController --> ProcessadorSMS : valida
AssinaturaController --> OperadoraCartao : autoriza
AssinaturaController --> ArquivoAssinaturas : grava
AssinaturaController --> Assinatura : cria
Catalogo "1" o-- "1..*" PlanoRefeicoes : oferece
Catalogo "1" o-- "1..*" Produto : oferece
Assinatura "0..*" --> "1" Assinante : pertence a
Assinatura "0..*" --> "1" PlanoRefeicoes : contrata
Assinatura "1" *-- "1" Pedido : possui
Assinatura "1" *-- "1" Endereco : entrega em
Assinatura "1" *-- "1" Entrega : agenda
Assinatura "1" *-- "1" Pagamento : registra
Pedido "1" *-- "1..*" ItemPedido : contem
ItemPedido "0..*" --> "1" Produto : seleciona
```

As entidades e objetos de valor usam `record` e cópias imutáveis de listas. As transições retornam novos objetos; `ativar()` não modifica um registro antigo em memória. `Catalogo` representa as coleções de planos/produtos; `ArquivoAssinaturas` concentra a leitura/gravação. Não se atribui ao arquivo a criação das entidades de negócio.

## Fluxo principal — validação e composição

```mermaid
sequenceDiagram
actor Cliente
participant Tela as TelaAssinatura
participant C as AssinaturaController
participant S as ProcessadorSMS
participant A as Assinante
participant Cat as Catalogo
participant Plano as PlanoRefeicoes
participant I as ItemPedido
participant P as Pedido
Cliente->>Tela: informar celular
Tela->>C: iniciarAssinatura(celular)
C->>A: new Assinante(celular)
C->>S: enviarCodigo(celular)
S-->>Cliente: código no SMS simulado
Cliente->>Tela: informar código
Tela->>C: confirmarCodigo(codigo)
C->>S: validarCodigo(codigo)
S-->>C: true
Tela->>C: listarPlanos()
C->>Cat: listarPlanos()
Cat-->>Tela: planos e periodicidades
Cliente->>Tela: selecionar plano
Tela->>C: confirmarPlano(id)
C->>Cat: buscarPlano(id)
Cliente->>Tela: informar preferência
Tela->>C: registrarPreferencias(preferencia)
loop Principal, acompanhamento e sobremesa
Tela->>C: listarProdutos(categoria)
C->>Cat: listar(categoria,preferencia)
Cat-->>Tela: produtos compatíveis
Cliente->>Tela: selecionar IDs e quantidades
Tela->>C: confirmarItens(categoria,quantidades)
C->>Cat: buscarProduto(id)
C->>I: new ItemPedido(produto,quantidade)
end
Tela->>C: resumoPedido()
C->>P: new Pedido(itens,PENDENTE)
P-->>Tela: composição completa
Cliente->>Tela: confirmar composição
Tela->>C: confirmarSelecao()
C->>P: contarRefeicoes()
C->>Plano: validarQuantidade(qtd)
Plano-->>C: true
```

O plano é o objeto selecionado por `confirmarPlano`. As mensagens de escolha confirmam um plano, uma preferência ou itens específicos.

## Fluxo principal — endereço, pagamento e conclusão

```mermaid
sequenceDiagram
actor Cliente
participant Tela as TelaAssinatura
participant C as AssinaturaController
participant E as Endereco
participant A as Assinatura
participant O as OperadoraCartao
participant Pg as Pagamento
participant P as Pedido
participant D as ArquivoAssinaturas
Cliente->>Tela: informar endereço
Tela->>E: new Endereco(rua,numero,bairro,cidade,uf,cep)
Cliente->>Tela: confirmar endereço
Tela->>C: confirmarEndereco(endereco)
Note over C,A: new Pedido, Entrega, Pagamento e Assinatura
C->>A: new Assinatura(...,AGUARDANDO_PAGAMENTO)
C->>D: salvar(pendente)
D-->>C: arquivo TXT gravado
Tela-->>Cliente: total do plano
Cliente->>Tela: informar token de cartão SIMULADO
Tela->>C: autorizarPagamento(token)
C->>O: autorizar(valor,token)
O-->>C: aprovado e autorização
C->>A: ativar(autorizacao)
A->>P: aprovar()
P-->>A: novo Pedido APROVADO
A->>Pg: registrarAprovacao(autorizacao)
Pg-->>A: novo Pagamento aprovado
A-->>C: nova Assinatura ATIVA com protocolo
C->>D: salvar(ativa)
D-->>C: arquivo substituído
Tela->>A: confirmacao()
A-->>Cliente: plano, refeições, periodicidade, endereço, previsão e protocolo
```

## FA1 — código inválido

```mermaid
sequenceDiagram
actor Cliente
participant Tela as TelaAssinatura
participant C as AssinaturaController
participant S as ProcessadorSMS
loop Até três tentativas inválidas
Cliente->>Tela: informar código inválido
Tela->>C: confirmarCodigo(codigo)
C->>S: validarCodigo(codigo)
S-->>C: false, decrementa tentativas
C-->>Tela: false
Tela-->>Cliente: erro e tentativas restantes
end
Note over S: Três erros inutilizam o código
Tela-->>Cliente: solicitar novo código ou encerrar
Cliente->>Tela: pedir novo código
Tela->>C: iniciarAssinatura(celular)
C->>S: enviarCodigo(celular)
Note over Cliente,S: Retorna à validação; não avança sem código válido
```

## FA2 — quantidade incompatível

```mermaid
sequenceDiagram
actor Cliente
participant Tela as TelaAssinatura
participant C as AssinaturaController
participant P as Pedido
participant Plano as PlanoRefeicoes
Tela->>C: confirmarSelecao()
C->>P: contarRefeicoes()
P-->>C: 6
C->>Plano: validarQuantidade(6)
Plano-->>C: false, limite 5
C-->>Tela: IllegalArgumentException com limite
Tela-->>Cliente: solicitar ajuste da composição
Cliente->>Tela: escolher 5 refeições
Tela->>C: confirmarItens(PRINCIPAL,quantidades)
Tela->>C: confirmarSelecao()
Note over C: Só avança ao endereço após validação
```

## FA3 — pagamento não autorizado

```mermaid
sequenceDiagram
actor Cliente
participant Tela as TelaAssinatura
participant C as AssinaturaController
participant O as OperadoraCartao
Tela->>C: autorizarPagamento(RECUSAR)
C->>O: autorizar(valor,token)
O-->>C: aprovado=false
C-->>Tela: false
Note over C: Assinatura permanece AGUARDANDO_PAGAMENTO; pedido PENDENTE
Tela-->>Cliente: tentar outro token ou desistir
Cliente->>Tela: APROVAR
Tela->>C: autorizarPagamento(APROVAR)
C->>O: autorizar(valor,token)
O-->>C: aprovado=true e autorização
Note over C: Retorna ao registro de pagamento e ativação do fluxo principal
```

## Estados da assinatura

```mermaid
stateDiagram-v2
[*] --> EmComposicao: validar cliente e escolher plano/itens
EmComposicao --> AguardandoPagamento: validar quantidade e confirmar endereço / salvar TXT
AguardandoPagamento --> AguardandoPagamento: pagamento recusado
AguardandoPagamento --> Ativa: pagamento aprovado / aprovar pedido e salvar TXT
Ativa --> [*]: mostrar confirmação
```

`EmComposicao` é um estado conceitual da interação; o registro `Assinatura` só é criado ao confirmar endereço. O código protege a ordem por uma enumeração interna de etapas.

## Correspondência com a Entrega 3

| Modelo anterior | Implementação atual | Justificativa |
|---|---|---|
| TelaAssinatura | TelaAssinatura | Fronteira de console |
| AssinaturaController | AssinaturaController | Orquestra as etapas |
| ServicoSMS | ProcessadorSMS + adaptador de saída | Limite de três tentativas; simulador explícito |
| CatalogoRepository | Catalogo | Coleções de planos e produtos, como solicitado no feedback |
| AssinaturaRepository | ArquivoAssinaturas | Grava objetos criados pelo controlador |
| ItemCardapio | Produto + ItemPedido | Separa o produto da quantidade escolhida |
| Assinatura/Pedido/Pagamento | Records homônimos | Transições imutáveis e persistência coerente |
| OperadoraCartao | Interface OperadoraCartao | Autorização simulada, substituível |
| Objetos faltantes | Assinante, Endereco, Entrega | Agora explícitos no modelo e no código |
| Sobremesas ausentes no PDF anterior | Categoria.SOBREMESA | Inclui passos 16–18 do enunciado original |

## Storyboard da fronteira

1. **Identificação:** campo de celular fictício, mensagem do simulador e campo de código. Erro mostra tentativas e oferece reenvio após o terceiro erro.
2. **Planos:** lista com quantidade, periodicidade e valor; confirmação por ID.
3. **Preferências:** tradicional, vegetariana, sem lactose ou vegetariana sem lactose.
4. **Cardápio:** listas filtradas, na ordem principal → acompanhamento → sobremesa; cada escolha tem ID e quantidade.
5. **Resumo:** composição completa e confirmação. Excesso retorna à seleção sem persistir uma adesão inválida.
6. **Endereço:** rua, número, bairro, cidade, UF e CEP; conferência antes de gravar adesão pendente.
7. **Pagamento:** total e token de simulação; recusa permite tentar novamente ou sair.
8. **Confirmação:** plano, itens, periodicidade, endereço, previsão, estados e protocolo.

Este storyboard documenta a fronteira implementada. Não equivale ao vídeo individual dos integrantes exigido para a Entrega 1.
