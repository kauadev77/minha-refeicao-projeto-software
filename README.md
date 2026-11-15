# Minha Refeição — Projeto de Software 2026/2

Caso de uso: **Assinar plano de refeições**. Integrantes: Kauã de Jesus Soares (RA 10254229) e Luiz Augusto de Sá Alves Ribeiro (RA 10322522, nome conforme a Entrega 3).

Implementação acadêmica em Java 17 ou superior, sem dependências externas. Interface no terminal, objetos fronteira/controlador/entidade, planos e cardápio em coleções, persistência TXT com leitura e gravação. SMS e operadora de cartão são **simuladores locais**, sem envio de mensagens, cobrança ou uso de dados reais. Valores dos planos, cardápio e previsão de primeira entrega (dois dias) são dados/regras de demonstração; o enunciado não fixa esses valores.

## Executar

Com JDK 17+ no PATH, abra o terminal na raiz do repositório:

```sh
mkdir out
javac -encoding UTF-8 -d out src/*.java
java -cp out Main
```

No Windows PowerShell os mesmos comandos funcionam. Compile os fontes do diretório `src`; não compile um arquivo isoladamente. A aplicação cria `dados/` somente ao confirmar o endereço. Use dados fictícios.

Para demonstrar automaticamente o fluxo principal e FA1/FA2/FA3:

```sh
java -cp out Main --demo
```

O modo demonstrativo gera arquivos TXT em `dados-demo/`. Para reler uma assinatura do modo interativo após encerrar o programa:

```sh
java -cp out Main --consultar UUID_DA_ASSINATURA
```

O UUID aparece ao final do protocolo, sem o prefixo `MR-`. A consulta mostra o registro; retomar o pagamento após reiniciar a aplicação não está implementado.

## Testar

```sh
java -cp out CenarioTest
```

A suíte testa as regras e grava/lê arquivos reais em diretório temporário. Cobre fluxo principal, bloqueio após três códigos inválidos, novo código, quantidade acima do plano, preferências, pagamento recusado/repetido, aprovação e recarga da assinatura. Os testes não exigem internet.

## Exemplo de uso

1. Informe `11999990000` e o código exibido no SMS simulado.
2. Escolha `SEMANAL` e `VEGETARIANA_SEM_LACTOSE`.
3. Principal `P2:5`; acompanhamento `A1:5`; sobremesa `S1:5`.
4. Confirme a composição e um endereço fictício.
5. Digite `RECUSAR` para FA3 e depois `APROVAR` para concluir.

Cada prato principal corresponde a uma refeição; acompanhamentos e sobremesas são complementos. O limite do plano aplica-se às refeições principais; cada categoria de complemento pode ter no máximo a mesma quantidade de refeições. Selecionar menos que a capacidade é permitido e não altera o preço fixo do plano. Esses critérios de composição explicitam decisões onde o enunciado não detalha a contagem.

## Organização

| Arquivo | Responsabilidade |
|---|---|
| `src/TelaAssinatura.java` | Fronteira: coleta e apresentação no terminal |
| `src/AssinaturaController.java` | Controlador e ordem das etapas |
| `src/Dominio.java` | Entidades/valores imutáveis e regras do pedido/plano |
| `src/Catalogo.java` | Coleções de planos e produtos |
| `src/ProcessadorSMS.java` | Validação do código e limite de tentativas |
| `src/OperadoraCartao.java` | Contrato e simulador de autorização |
| `src/ArquivoAssinaturas.java` | Persistência de todo o agregado em TXT |
| `src/Main.java` | Montagem da aplicação, consulta e demonstração |
| `src/CenarioTest.java` | Testes executáveis sem framework |

Os tipos de domínio sem modificador `public` estão agrupados em `Dominio.java`, no pacote padrão, para compilar diretamente com o SDK. A interface de cartão permite substituir o simulador sem alterar o controlador. A persistência usa um arquivo por assinatura, com gravação em temporário e substituição atômica quando suportada. Tokens de cartão e códigos SMS não são gravados.

## Documentação e autoria

Vídeo: [demonstração técnica automática com narração sintética](demonstracao-tecnica.mp4). O vídeo explica um recorte UML, o código e a saída real dos fluxos principal e alternativos. Não é uma gravação dos integrantes. A [saída textual capturada](DEMONSTRACAO.txt) permite conferir a demonstração.

Consulte a [Wiki do projeto](https://github.com/kauadev77/minha-refeicao-projeto-software/wiki) e o arquivo `MODELOS.md` para diagramas e correspondência UML → Java. A versão de implementação corrige a ausência de Assinante, Produto, ItemPedido, Endereço e Entrega apontada no feedback da Entrega 3 e inclui sobremesas, presentes no enunciado original.

Código, documentação e testes preparados com auxílio de OpenAI Codex em 15/11/2026. O material deve ser revisado e compreendido pelos integrantes; não atribui ao grupo uma divisão de trabalho que não foi informada. A gravação da apresentação dos integrantes e a divisão real das responsabilidades precisam ser acrescentadas pelo grupo.

Referências: enunciado do projeto no Moodle, Entrega 3 previamente enviada pelo grupo e feedback da professora em 07/11/2026.
