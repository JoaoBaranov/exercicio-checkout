package br.edu.checkout;

import java.math.BigDecimal;
import java.util.Objects;

public final class ServicoCheckout {

    private final EstoqueGateway estoqueGateway;
    private final PagamentoGateway pagamentoGateway;
    private final PedidoRepository pedidoRepository;
    private final Notificador notificador;

    public ServicoCheckout(
            EstoqueGateway estoqueGateway,
            PagamentoGateway pagamentoGateway,
            PedidoRepository pedidoRepository,
            Notificador notificador) {
        this.estoqueGateway = Objects.requireNonNull(estoqueGateway);
        this.pagamentoGateway = Objects.requireNonNull(pagamentoGateway);
        this.pedidoRepository = Objects.requireNonNull(pedidoRepository);
        this.notificador = Objects.requireNonNull(notificador);
    }

    public ResultadoCheckout finalizar(SolicitacaoCheckout solicitacao) {
        validar(solicitacao);
        BigDecimal total = solicitacao.precoUnitario().multiply(BigDecimal.valueOf(solicitacao.quantidade()));
        boolean reservado = estoqueGateway.reservar(solicitacao.sku(), solicitacao.quantidade());
        if (!reservado) {
            throw new EstoqueInsuficienteException();
        }
        Pagamento pagamento;
        try {
            pagamento = pagamentoGateway.cobrar(solicitacao.clienteId(), total);
        } catch (FalhaPagamentoException exception) {
            estoqueGateway.liberar(solicitacao.sku(), solicitacao.quantidade());
            throw exception;
        }
        if (!pagamento.aprovado()) {
            estoqueGateway.liberar(solicitacao.sku(), solicitacao.quantidade());
            throw new PagamentoRecusadoException();
        }
        PedidoConfirmado pedido = new PedidoConfirmado(
                solicitacao.clienteId(), solicitacao.email(), solicitacao.sku(),
                solicitacao.quantidade(), total, pagamento.transacaoId());
        String numeroPedido = pedidoRepository.salvar(pedido);
        try {
            notificador.enviarConfirmacao(solicitacao.email(), numeroPedido, total);
            return new ResultadoCheckout(numeroPedido, total, true);
        } catch (FalhaNotificacaoException exception) {
            return new ResultadoCheckout(numeroPedido, total, false);
        }
    }

    private void validar(SolicitacaoCheckout solicitacao) {
        if (solicitacao == null
                || vazio(solicitacao.clienteId())
                || vazio(solicitacao.email())
                || vazio(solicitacao.sku())
                || solicitacao.quantidade() <= 0
                || solicitacao.precoUnitario() == null
                || solicitacao.precoUnitario().signum() <= 0) {
            throw new IllegalArgumentException("Solicitação inválida");
        }
    }

    private boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }
}
