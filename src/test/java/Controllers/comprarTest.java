package Controllers;

import DAO.DaoCliente;
import DAO.DaoPedido;
import Helpers.ValidadorCookie;
import Model.Cliente;
import Model.Pedido;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class comprarTest {

    //injetar mocks
    public class comprarParaTeste extends comprar {
        ValidadorCookie validadorMock;
        DaoPedido daoPedidoMock;
        DaoCliente daoClienteMock;

        public comprarParaTeste(ValidadorCookie v, DaoPedido dp, DaoCliente dc) {
            this.validadorMock = v;
            this.daoPedidoMock = dp;
            this.daoClienteMock = dc;
        }

        @Override protected ValidadorCookie getValidadorCookie() { return validadorMock; }
        @Override protected DaoPedido getDaoPedido() { return daoPedidoMock; }
        @Override protected DaoCliente getDaoCliente() { return daoClienteMock; }
        
        //null nos outros pois com o json simples eles não serão chamados
        @Override protected DAO.DaoLanche getDaoLanche() { return null; }
        @Override protected DAO.DaoBebida getDaoBebida() { return null; }
    }

    @Test
    public void testeSimples_TudoCerto() throws Exception {
        // mocks
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        
        // mocks das classes de Banco e Regra
        ValidadorCookie validador = mock(ValidadorCookie.class);
        DaoPedido daoPedido = mock(DaoPedido.class);
        DaoCliente daoCliente = mock(DaoCliente.class); // <--- Adicionado para não dar erro

        //cookie válido
        when(validador.validar(any())).thenReturn(true);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("teste", "teste")});

        //cliente existe
        Cliente clienteFake = new Cliente();
        clienteFake.setId_cliente(1);
        when(daoCliente.pesquisaPorID(anyString())).thenReturn(clienteFake);

        //json de Entrada
        String jsonSimples = "{\"id\": 1}";
        mockInputStream(request, jsonSimples);

        //captura de resposta
        StringWriter textoSaida = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(textoSaida));

  
        comprar servlet = new comprarParaTeste(validador, daoPedido, daoCliente);
        
        servlet.processRequest(request, response);

        //verifica se tentou salvar o pedido
        verify(daoPedido, times(1)).salvar(any(Pedido.class));
    }

    
    //método para fazer o request fingir que enviou um json
    
    private void mockInputStream(HttpServletRequest req, String json) throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(json.getBytes());
        ServletInputStream servletInputStream = new ServletInputStream() {
            public int read() throws IOException { return byteArrayInputStream.read(); }
            public boolean isFinished() { return byteArrayInputStream.available() == 0; }
            public boolean isReady() { return true; }
            public void setReadListener(ReadListener readListener) {}
        };
        when(req.getInputStream()).thenReturn(servletInputStream);
    }
}