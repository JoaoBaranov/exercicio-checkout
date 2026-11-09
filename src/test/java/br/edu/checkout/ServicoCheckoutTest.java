package br.edu.checkout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class ServicoCheckoutTest {

    @Mock
    private EstoqueGateway estoqueGateway;

    @Mock
    private PagamentoGateway pagamentoGateway;

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private Notificador notificador;

    private ServicoCheckout servico;

    @BeforeEach
    void criarServicoReal() {
        servico = new ServicoCheckout(
                estoqueGateway, pagamentoGateway, pedidoRepository, notificador);
    }

    @Test
    void deveRejeitarSolicitacaoNulaSemConsultarColaboradores() {
        assertThrows(IllegalArgumentException.class, () -> servico.finalizar(null));
        verifyNoInteractions(
                estoqueGateway, pagamentoGateway, pedidoRepository, notificador);
    }

    @ParameterizedTest
    @MethodSource("solicitacoesInvalidas")
    void deveRejeitarSolicitacoesInvalidasSemEfeitos(SolicitacaoCheckout solicitacao) {
        assertThrows(IllegalArgumentException.class, () -> servico.finalizar(solicitacao));
        verifyNoInteractions(estoqueGateway, pagamentoGateway, pedidoRepository, notificador);
    }

    static Stream<Arguments> solicitacoesInvalidas() {
        SolicitacaoCheckout base = new SolicitacaoCheckout("cliente", "email", "sku", 1, new BigDecimal("10"));
        return Stream.of(
                Arguments.of(new SolicitacaoCheckout(null, base.email(), base.sku(), 1, base.precoUnitario())),
                Arguments.of(new SolicitacaoCheckout("", base.email(), base.sku(), 1, base.precoUnitario())),
                Arguments.of(new SolicitacaoCheckout(" ", base.email(), base.sku(), 1, base.precoUnitario())),
                Arguments.of(new SolicitacaoCheckout(base.clienteId(), null, base.sku(), 1, base.precoUnitario())),
                Arguments.of(new SolicitacaoCheckout(base.clienteId(), "", base.sku(), 1, base.precoUnitario())),
                Arguments.of(new SolicitacaoCheckout(base.clienteId(), " ", base.sku(), 1, base.precoUnitario())),
                Arguments.of(new SolicitacaoCheckout(base.clienteId(), base.email(), null, 1, base.precoUnitario())),
                Arguments.of(new SolicitacaoCheckout(base.clienteId(), base.email(), "", 1, base.precoUnitario())),
                Arguments.of(new SolicitacaoCheckout(base.clienteId(), base.email(), " ", 1, base.precoUnitario())),
                Arguments.of(new SolicitacaoCheckout(base.clienteId(), base.email(), base.sku(), 0, base.precoUnitario())),
                Arguments.of(new SolicitacaoCheckout(base.clienteId(), base.email(), base.sku(), -1, base.precoUnitario())),
                Arguments.of(new SolicitacaoCheckout(base.clienteId(), base.email(), base.sku(), 1, null)),
                Arguments.of(new SolicitacaoCheckout(base.clienteId(), base.email(), base.sku(), 1, BigDecimal.ZERO)),
                Arguments.of(new SolicitacaoCheckout(base.clienteId(), base.email(), base.sku(), 1, new BigDecimal("-1"))));
    }

    @Test
    void deveFinalizarCompraAprovadaComOrdemEPedidoCorretos() {
        SolicitacaoCheckout solicitacao = solicitacao("cliente", "email", "sku", 3, "19.90");
        when(estoqueGateway.reservar("sku", 3)).thenReturn(true);
        when(pagamentoGateway.cobrar("cliente", new BigDecimal("59.70")))
                .thenReturn(new Pagamento(true, "tx-1"));
        when(pedidoRepository.salvar(any())).thenReturn("pedido-1");

        ResultadoCheckout resultado = servico.finalizar(solicitacao);

        assertEquals("pedido-1", resultado.numeroPedido());
        assertEquals(new BigDecimal("59.70"), resultado.total());
        assertTrue(resultado.notificacaoEnviada());
        ArgumentCaptor<PedidoConfirmado> captor = ArgumentCaptor.forClass(PedidoConfirmado.class);
        verify(pedidoRepository, times(1)).salvar(captor.capture());
        assertEquals(new PedidoConfirmado("cliente", "email", "sku", 3, new BigDecimal("59.70"), "tx-1"), captor.getValue());
        verify(notificador).enviarConfirmacao("email", "pedido-1", new BigDecimal("59.70"));
        verify(estoqueGateway, never()).liberar(any(), any(Integer.class));
        InOrder ordem = org.mockito.Mockito.inOrder(estoqueGateway, pagamentoGateway, pedidoRepository, notificador);
        ordem.verify(estoqueGateway).reservar("sku", 3);
        ordem.verify(pagamentoGateway).cobrar("cliente", new BigDecimal("59.70"));
        ordem.verify(pedidoRepository).salvar(any());
        ordem.verify(notificador).enviarConfirmacao("email", "pedido-1", new BigDecimal("59.70"));
    }

    @Test
    void devePreservarPrecisaoDoTotal() {
        SolicitacaoCheckout solicitacao = solicitacao("cliente", "email", "sku", 3, "0.005");
        when(estoqueGateway.reservar("sku", 3)).thenReturn(true);
        when(pagamentoGateway.cobrar("cliente", new BigDecimal("0.015")))
                .thenReturn(new Pagamento(true, "tx"));
        when(pedidoRepository.salvar(any())).thenReturn("pedido");
        assertEquals(new BigDecimal("0.015"), servico.finalizar(solicitacao).total());
    }

    @Test
    void deveRecusarQuandoNaoHaEstoque() {
        when(estoqueGateway.reservar("sku", 1)).thenReturn(false);
        assertThrows(EstoqueInsuficienteException.class, () -> servico.finalizar(solicitacaoValida()));
        verifyNoInteractions(pagamentoGateway, pedidoRepository, notificador);
        verify(estoqueGateway, never()).liberar(any(), any(Integer.class));
    }

    @Test
    void deveLiberarReservaQuandoPagamentoRecusado() {
        when(estoqueGateway.reservar("sku", 1)).thenReturn(true);
        when(pagamentoGateway.cobrar("cliente", new BigDecimal("10"))).thenReturn(new Pagamento(false, null));
        assertThrows(PagamentoRecusadoException.class, () -> servico.finalizar(solicitacaoValida()));
        verify(estoqueGateway).liberar("sku", 1);
        verifyNoInteractions(pedidoRepository, notificador);
    }

    @Test
    void devePropagarMesmaFalhaTecnicaDePagamento() {
        FalhaPagamentoException falha = new FalhaPagamentoException("falha");
        when(estoqueGateway.reservar("sku", 1)).thenReturn(true);
        when(pagamentoGateway.cobrar("cliente", new BigDecimal("10"))).thenThrow(falha);
        assertSame(falha, assertThrows(FalhaPagamentoException.class, () -> servico.finalizar(solicitacaoValida())));
        verify(estoqueGateway).liberar("sku", 1);
        verifyNoInteractions(pedidoRepository, notificador);
    }

    @Test
    void deveRetornarCompraRegistradaQuandoNotificacaoFalha() {
        when(estoqueGateway.reservar("sku", 1)).thenReturn(true);
        when(pagamentoGateway.cobrar("cliente", new BigDecimal("10"))).thenReturn(new Pagamento(true, "tx"));
        when(pedidoRepository.salvar(any())).thenReturn("pedido");
        doThrow(new FalhaNotificacaoException("falha")).when(notificador)
                .enviarConfirmacao("email", "pedido", new BigDecimal("10"));
        ResultadoCheckout resultado = servico.finalizar(solicitacaoValida());
        assertFalse(resultado.notificacaoEnviada());
        verify(pagamentoGateway, times(1)).cobrar("cliente", new BigDecimal("10"));
        verify(pedidoRepository, times(1)).salvar(any());
        verify(estoqueGateway, never()).liberar(any(), any(Integer.class));
    }

    private SolicitacaoCheckout solicitacaoValida() {
        return solicitacao("cliente", "email", "sku", 1, "10");
    }

    private SolicitacaoCheckout solicitacao(String cliente, String email, String sku, int quantidade, String preco) {
        return new SolicitacaoCheckout(cliente, email, sku, quantidade, new BigDecimal(preco));
    }
}
