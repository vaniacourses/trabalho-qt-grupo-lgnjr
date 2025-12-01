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

/**
 * Testes UNITÁRIOS da servlet alterarIngrediente.
 * Salvar este arquivo como: alterarIngredienteTest.java
 */
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
    class AlterarIngredienteControlado extends alterarIngrediente {
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

        alterarIngrediente servlet = new AlterarIngredienteControlado();
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

        alterarIngrediente servlet = new AlterarIngredienteControlado();
        servlet.processRequest(request, response);

        assertTrue(sw.toString().contains("Não autorizado"));
        verify(daoIngredienteMock, never()).alterar(any());
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    // 3. ID com letras (Erro de formato)
    @Test
    void testeAlteracao_ComLetrasNoID_DeveRetornarErro() throws Exception {

        when(validadorMock.validarFuncionario(any())).thenReturn(true);
        when(request.getCookies()).thenReturn(new Cookie[]{ new Cookie("auth", "ok") });

        mockInputStream("{\"id\":\"ABC\", \"nome\":\"X\"}");

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        alterarIngrediente servlet = new AlterarIngredienteControlado();
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

        mockInputStream(" ");

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        alterarIngrediente servlet = new AlterarIngredienteControlado();
        servlet.processRequest(request, response);

        assertTrue(sw.toString().contains("Dados ausentes"));
        verify(daoIngredienteMock, never()).alterar(any());
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    // 5. InputStream nulo 
    @Test
    void testeAlteracao_SemInputStream_MesmoAutorizado_DeveRetornarErro() throws Exception {

        when(request.getCookies()).thenReturn(new Cookie[]{ new Cookie("auth", "ok") });
        when(validadorMock.validarFuncionario(any())).thenReturn(true);

        when(request.getInputStream()).thenReturn(null);

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        alterarIngrediente servlet = new AlterarIngredienteControlado();
        servlet.processRequest(request, response);

        assertTrue(sw.toString().contains("Não autorizado")); // Ou erro genérico dependendo da lógica
        verify(daoIngredienteMock, never()).alterar(any());
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    // 6. Cookies nulos 
    @Test
    void testeAlteracao_SemCookies_DeveRetornarNaoAutorizado() throws Exception {

        when(request.getCookies()).thenReturn(null);
        // O validador não será chamado se cookies for null no seu código original
        // ou será chamado e deve retornar false/erro.
        
        mockInputStream("{\"id\": 1}");

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        alterarIngrediente servlet = new AlterarIngredienteControlado();
        servlet.processRequest(request, response);

        assertTrue(sw.toString().contains("Não autorizado"));
        verify(daoIngredienteMock, never()).alterar(any());
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    // 7. Validador nulo (Teste de robustez do método protegido)
    @Test
    void testeAlteracao_ValidadorNulo_DeveRetornarNaoAutorizado() throws Exception {

        when(request.getCookies()).thenReturn(new Cookie[]{ new Cookie("auth", "ok") });

        mockInputStream("{\"id\":1, \"nome\":\"X\"}");

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        // Aqui sobrescrevemos para retornar null propositalmente
        alterarIngrediente servlet = new alterarIngrediente() {
            @Override protected ValidadorCookie getValidadorCookie() { return null; }
            @Override protected DaoIngrediente getDaoIngrediente() { return daoIngredienteMock; }
        };

        servlet.processRequest(request, response);

        assertTrue(sw.toString().contains("Não autorizado"));
        verify(daoIngredienteMock, never()).alterar(any());
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    // 8. Validador lança NullPointerException
    @Test
    void testeAlteracao_ValidadorLancaNullPointer_DeveRetornarNaoAutorizado() throws Exception {

        Cookie[] cookies = { new Cookie("auth", "ok") };
        when(request.getCookies()).thenReturn(cookies);
        when(validadorMock.validarFuncionario(cookies))
                .thenThrow(new NullPointerException("falha simulada"));

        mockInputStream("{\"id\": 1, \"nome\": \"Teste\"}");

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        alterarIngrediente servlet = new AlterarIngredienteControlado();
        servlet.processRequest(request, response);

        assertTrue(sw.toString().contains("Não autorizado"));
        verify(daoIngredienteMock, never()).alterar(any());
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    //  9. doPost delega para processRequest 
    @Test
    void testeDoPost_DeveDelegarParaProcessRequest_FluxoFeliz() throws Exception {

        when(validadorMock.validarFuncionario(any())).thenReturn(true);
        when(request.getCookies()).thenReturn(new Cookie[]{ new Cookie("auth", "ok") });

        mockInputStream("{\"id\": 2, \"nome\": \"Teste Post\", \"descricao\": \"Desc\", \"quantidade\": 3, \"ValorCompra\": 1.0, \"ValorVenda\": 2.0, \"tipo\": \"TIPO\"}");

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        alterarIngrediente servlet = new AlterarIngredienteControlado();
        servlet.doPost(request, response);

        verify(daoIngredienteMock).alterar(any());
        assertTrue(sw.toString().contains("Ingrediente Alterado!"));
    }

    // 10. doGet delega para processRequest
    @Test
    void testeDoGet_DeveDelegarParaProcessRequest_ErroAutenticacao() throws Exception {

        when(validadorMock.validarFuncionario(any())).thenReturn(false);
        when(request.getCookies()).thenReturn(new Cookie[]{ new Cookie("auth", "bad") });

        mockInputStream("{\"id\": 3}");

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        alterarIngrediente servlet = new AlterarIngredienteControlado();
        servlet.doGet(request, response);

        verify(daoIngredienteMock, never()).alterar(any());
        assertTrue(sw.toString().contains("Não autorizado"));
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }
}