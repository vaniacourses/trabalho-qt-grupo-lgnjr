package Controllers;

import DAO.DaoLanche;
import Helpers.ValidadorCookie;
import Model.Lanche;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class getLanchesTest {

    // mocks
    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;
    @Mock ValidadorCookie validadorMock;
    @Mock DaoLanche daoLancheMock;
    
    @BeforeEach
    public void setup() {
        // inicializa os mocks
        MockitoAnnotations.openMocks(this);
    }

    // subclasse para injetar os mocks e Gson 
    class getLanchesControlado extends getLanches {
        @Override protected ValidadorCookie getValidadorCookie() { return validadorMock; }
        @Override protected DaoLanche getDaoLanche() { return daoLancheMock; }
        
        
        @Override protected Gson getGson() { return new Gson(); }
    }

    @Test
    public void testProcessRequest_FuncionarioNaoAutorizado_RetornaErro() throws Exception {
        
        // validador diz "não" 
        when(validadorMock.validarFuncionario(any())).thenReturn(false);
        
        // o request tem um cookie qualquer
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("token", "invalido")});

        // prepara captura da resposta
        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        
        getLanches servlet = new getLanchesControlado();
        servlet.processRequest(request, response);

        //deve imprimir "erro" e NÃO deve chamar o banco
        assertTrue(sw.toString().trim().contains("erro"), "Deveria retornar erro");
        verify(daoLancheMock, never()).listarTodos();
    }

    @Test
    public void testProcessRequest_FuncionarioAutorizado_RetornaJson() throws Exception {

    	//validador diz "SIM" (true)
        when(validadorMock.validarFuncionario(any())).thenReturn(true);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("token", "valido")});

        //banco de dados retorna uma lista com 1 lanche
        List<Lanche> listaFake = new ArrayList<>();
        Lanche lanche = new Lanche();
        lanche.setNome("X-Tudo");
        lanche.setValor_venda(25.50);
        listaFake.add(lanche);
        
        when(daoLancheMock.listarTodos()).thenReturn(listaFake);

        // prepara captura da resposta
        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

       
        getLanches servlet = new getLanchesControlado();
        servlet.processRequest(request, response);

        // verificação
        String saida = sw.toString().trim();
        
        // verifica se o Gson real converteu o objeto para texto
        // a string deve conter o nome do lanche criado
        assertTrue(saida.contains("X-Tudo"), "O JSON deveria conter o lanche X-Tudo");
        assertTrue(saida.contains("25.5"), "O JSON deveria conter o preço");

        // verifica se o DAO foi chamado 1 vez
        verify(daoLancheMock, times(1)).listarTodos();
    }
}