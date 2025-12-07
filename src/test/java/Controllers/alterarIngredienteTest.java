package Controllers;

import DAO.DaoIngrediente;
import Helpers.ValidadorCookie;
import Model.Ingrediente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class alterarIngredienteTest {

    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;
    @Mock ValidadorCookie validadorMock;
    @Mock DaoIngrediente daoIngredienteMock;

    @BeforeEach
    void iniciar() {
        MockitoAnnotations.openMocks(this);
    }

    // Helper para simular InputStream com JSON
    private void mockInputStream(String json) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(json.getBytes());
        ServletInputStream servletInputStream = new ServletInputStream() {
            @Override
            public int read() { return bais.read(); }
            @Override
            public boolean isFinished() { return bais.available() == 0; }
            @Override
            public boolean isReady() { return true; }
            @Override
            public void setReadListener(ReadListener readListener) {}
        };
        when(request.getInputStream()).thenReturn(servletInputStream);
    }

    // Subclasse para injetar mocks (Costura/Seam)
    // Permite testar sem conectar no banco de dados real
    class AlterarIngredienteControlado extends AlterarIngrediente {
        @Override protected ValidadorCookie getValidadorCookie() { return validadorMock; }
        @Override protected DaoIngrediente getDaoIngrediente() { return daoIngredienteMock; }
    }

    //  1. FLUXO FELIZ - Tudo certo
    @Test
    void testeAlteracao_ComDadosValidos_DeveChamarAlterarDAO() throws Exception {

        when(validadorMock.validarFuncionario(any())).thenReturn(true);
        when(request.getCookies()).thenReturn(new Cookie[]{ new Cookie("auth", "ok") });

        String jsonInput =
                "{\"id\": 15, \"nome\": \"Pao Frances\", \"descricao\": \"Pao fresquinho\", " +
                        "\"quantidade\": 10, \"ValorCompra\": 0.50, \"ValorVenda\": 1.00, \"tipo\": \"PAO\"}";

        mockInputStream(jsonInput);

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        AlterarIngrediente servlet = new AlterarIngredienteControlado();
        servlet.processRequest(request, response);

        // Verifica cabeçalhos (mata mutantes que removem setContentType)
        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");

        // Resposta deve conter a frase de sucesso
        assertTrue(sw.toString().contains("Ingrediente Alterado!"));

        // Captura o Ingrediente enviado para o DAO e valida os dados
        ArgumentCaptor<Ingrediente> captor = ArgumentCaptor.forClass(Ingrediente.class);
        verify(daoIngredienteMock, times(1)).alterar(captor.capture());

        Ingrediente ing = captor.getValue();

        // Verificações completas (matam mutantes de alteração de valor)
        assertEquals(15, ing.getId_ingrediente());
        assertEquals("Pao Frances", ing.getNome());
        assertEquals("Pao fresquinho", ing.getDescricao());
        assertEquals(10, ing.getQuantidade());
        assertEquals(0.50, ing.getValor_compra());
        assertEquals(1.00, ing.getValor_venda());
        assertEquals("PAO", ing.getTipo());
        assertEquals(1, ing.getFg_ativo());
    }

    //  2. Autenticação inválida
    @Test
    void testeAlteracao_SemAutenticacao_DeveRetornarErro() throws Exception {

        when(validadorMock.validarFuncionario(any())).thenReturn(false);
        when(request.getCookies()).thenReturn(new Cookie[]{ new Cookie("auth", "bad") });

        mockInputStream("{\"id\": 15}");

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        AlterarIngrediente servlet = new AlterarIngredienteControlado();
        servlet.processRequest(request, response);

        assertTrue(sw.toString().contains("Não autorizado"));
        verify(daoIngredienteMock, never()).alterar(any());
        // controller manda 401
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    // 3. ID com letras (Erro de formato)
    @Test
    void testeAlteracao_ComLetrasNoID_DeveRetornarErro() throws Exception {

        when(validadorMock.validarFuncionario(any())).thenReturn(true);
        when(request.getCookies()).thenReturn(new Cookie[]{ new Cookie("auth", "ok") });

        mockInputStream("{\"id\":\"ABC\", \"nome\":\"X\"}");

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        AlterarIngrediente servlet = new AlterarIngredienteControlado();
        servlet.processRequest(request, response);

        assertTrue(sw.toString().contains("Erro no formato"));
        verify(daoIngredienteMock, never()).alterar(any());
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    // 4. JSON vazio 
    @Test
    void testeAlteracao_ComJsonNuloOuVazio_DeveRetornarErro() throws Exception {

        when(validadorMock.validarFuncionario(any())).thenReturn(true);
        when(request.getCookies()).thenReturn(new Cookie[]{ new Cookie("auth", "ok") });

        // aqui é string vazia MESMO, não espaço
        mockInputStream("");

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        AlterarIngrediente servlet = new AlterarIngredienteControlado();
        servlet.processRequest(request, response);
        // não alterar o banco e retornar 400
        verify(daoIngredienteMock, never()).alterar(any());
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    // 6. Cookies nulos 
    @Test
    void testeAlteracao_SemCookies_DeveRetornarNaoAutorizado() throws Exception {

        when(request.getCookies()).thenReturn(null);
        
        mockInputStream("{\"id\": 1}");

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        AlterarIngrediente servlet = new AlterarIngredienteControlado();
        servlet.processRequest(request, response);

        assertTrue(sw.toString().contains("Não autorizado"));
        verify(daoIngredienteMock, never()).alterar(any());
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    // 7. Validador nulo (Teste de robustez do método protegido)
    @Test
    void testeAlteracao_ValidadorNulo_DeveRetornarNaoAutorizado() throws Exception {

        when(request.getCookies()).thenReturn(new Cookie[]{ new Cookie("auth", "ok") });

        mockInputStream("{\"id\":1, \"nome\":\"X\"}");

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        AlterarIngrediente servlet = new AlterarIngrediente() {
            @Override protected ValidadorCookie getValidadorCookie() { return null; }
            @Override protected DaoIngrediente getDaoIngrediente() { return daoIngredienteMock; }
        };

        servlet.processRequest(request, response);

        assertTrue(sw.toString().contains("Não autorizado"));
        verify(daoIngredienteMock, never()).alterar(any());
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
