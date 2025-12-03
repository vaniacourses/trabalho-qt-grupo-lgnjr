package Controllers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.gson.Gson;

import DAO.DaoIngrediente;
import Helpers.ValidadorCookie;
import Model.Ingrediente;

public class getIngredientesPorLancheTest {

    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;
    @Mock ValidadorCookie validadorMock;
    @Mock DaoIngrediente daoIngredienteMock;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void UNITARIO_FluxoFeliz_RetornaListaDeIngredientes() throws Exception {
        // 1. Configura Login OK
        when(validadorMock.validarFuncionario(any())).thenReturn(true);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("auth", "ok")});

        // 2. Simula JSON de entrada {"id": 10}
        mockInputStream("{\"id\": 10}");

        // 3. Configura retorno do DAO (Lista com Bacon)
        List<Ingrediente> lista = new ArrayList<>();
        Ingrediente ing = new Ingrediente();
        ing.setNome("Bacon");
        lista.add(ing);
        when(daoIngredienteMock.listarTodosPorLanche(10)).thenReturn(lista);

        // 4. Captura Resposta
        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        // 5. Executa (injeção via construtor)
        getIngredientesPorLanche servlet = new getIngredientesPorLanche(validadorMock, daoIngredienteMock, new Gson());
        servlet.processRequest(request, response);

        // 6. Verifica se o JSON contem "Bacon"
        String saida = sw.toString();
        assertTrue(saida.contains("Bacon"), "Deveria retornar o JSON com ingredientes");
        verify(daoIngredienteMock).listarTodosPorLanche(10);
    }

    @Test
    public void UNITARIO_SemPermissao_RetornaErro() throws Exception {
        when(validadorMock.validarFuncionario(any())).thenReturn(false);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("auth", "bad")});
        mockInputStream("{}");

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        getIngredientesPorLanche servlet = new getIngredientesPorLanche(validadorMock, daoIngredienteMock, new Gson());
        servlet.processRequest(request, response);

        assertTrue(sw.toString().contains("erro"), "Deveria dar erro de permissão");
        verify(daoIngredienteMock, never()).listarTodosPorLanche(anyInt());
    }

    @Test
    public void UNITARIO_JsonSemId_RetornaErro() throws Exception {
        when(validadorMock.validarFuncionario(any())).thenReturn(true);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("auth", "ok")});

        mockInputStream("{\"nome\": \"teste\"}");

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        getIngredientesPorLanche servlet = new getIngredientesPorLanche(validadorMock, daoIngredienteMock, new Gson());
        servlet.processRequest(request, response);

        assertTrue(sw.toString().contains("erro"), "JSON sem ID deve dar erro");
    }

    @Test
    public void UNITARIO_DaoRetornaNulo_RetornaErro() throws Exception {
        when(validadorMock.validarFuncionario(any())).thenReturn(true);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("auth", "ok")});
        mockInputStream("{\"id\": 5}");

        when(daoIngredienteMock.listarTodosPorLanche(5)).thenReturn(null);

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        getIngredientesPorLanche servlet = new getIngredientesPorLanche(validadorMock, daoIngredienteMock, new Gson());
        servlet.processRequest(request, response);

        assertTrue(sw.toString().contains("erro"), "Lista nula deve retornar erro");
    }

    // Helper para simular o corpo da requisição (JSON)
    private void mockInputStream(String json) throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(json.getBytes());
        ServletInputStream servletInputStream = new ServletInputStream() {
            public int read() throws IOException { return byteArrayInputStream.read(); }
            public boolean isFinished() { return byteArrayInputStream.available() == 0; }
            public boolean isReady() { return true; }
            public void setReadListener(ReadListener readListener) {}
        };
        when(request.getInputStream()).thenReturn(servletInputStream);
    }
}