package Controllers;

import DAO.DaoStatusLanchonete;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class alterarStatusLanchoneteTest {

    private DaoStatusLanchonete mockDao;
    private alterarStatusLanchonete controller;

    private HttpServletRequest mockRequest;
    private HttpServletResponse mockResponse;

    private PrintWriter writer;
    private StringWriter responseWriter;

    @BeforeEach
    void setup() throws Exception {
        mockDao = mock(DaoStatusLanchonete.class);
        controller = new alterarStatusLanchonete(mockDao);

        mockRequest = mock(HttpServletRequest.class);
        mockResponse = mock(HttpServletResponse.class);

        responseWriter = new StringWriter();
        writer = new PrintWriter(responseWriter);

        when(mockResponse.getWriter()).thenReturn(writer);
    }

    // Utilitário para simular JSON no corpo do request
    private void mockJsonBody(String json) throws Exception {
        BufferedReader br = new BufferedReader(new StringReader(json));
        when(mockRequest.getReader()).thenReturn(br);
    }

    // 1) Status = "ABERTO" → Caminho Feliz
    @Test
    void deveAlterarParaAbertoQuandoStatusForAberto() throws Exception {
        mockJsonBody("{\"status\":\"ABERTO\"}");

        controller.processRequest(mockRequest, mockResponse);

        verify(mockDao).alterarStatus("ABERTO");
        assertTrue(responseWriter.toString().contains("\"status\":\"ABERTO\""));
    }

    // 2) Status = "FECHADO"
    @Test
    void deveAlterarParaFechadoQuandoStatusForFechado() throws Exception {
        mockJsonBody("{\"status\":\"FECHADO\"}");

        controller.processRequest(mockRequest, mockResponse);

        verify(mockDao).alterarStatus("FECHADO");
        assertTrue(responseWriter.toString().contains("\"status\":\"FECHADO\""));
    }

    // 3) Status inválido → vira ABERTO
    @Test
    void deveDefinirAbertoQuandoStatusForInvalido() throws Exception {
        mockJsonBody("{\"status\":\"OUTRO\"}");

        controller.processRequest(mockRequest, mockResponse);

        verify(mockDao).alterarStatus("ABERTO");
        assertTrue(responseWriter.toString().contains("\"status\":\"ABERTO\""));
    }

    // 4) JSON válido, mas sem campo status
    @Test
    void deveTratarComoInvalidoQuandoNaoTemStatus() throws Exception {
        mockJsonBody("{\"x\":123}");

        controller.processRequest(mockRequest, mockResponse);

        verify(mockDao).alterarStatus("ABERTO");
        assertTrue(responseWriter.toString().contains("\"status\":\"ABERTO\""));
    }

    // 5) status = null
    @Test
    void deveTratarStatusNullComoInvalido() throws Exception {
        mockJsonBody("{\"status\":null}");

        controller.processRequest(mockRequest, mockResponse);

        verify(mockDao).alterarStatus("ABERTO");
        assertTrue(responseWriter.toString().contains("\"status\":\"ABERTO\""));
    }

    // 6) JSON malformado → JSONException
    @Test
    void deveRetornarErroEmJsonMalformado() throws Exception {
        mockJsonBody("{status:,,,,}");

        controller.processRequest(mockRequest, mockResponse);

        assertTrue(responseWriter.toString().contains("Status inválido"));
        verify(mockDao, never()).alterarStatus(anyString());
    }

    // 7) JSON vazio
    @Test
    void deveRetornarErroJsonVazio() throws Exception {
        mockJsonBody("");

        controller.processRequest(mockRequest, mockResponse);

        assertTrue(responseWriter.toString().contains("Status inválido"));
        verify(mockDao, never()).alterarStatus(anyString());
    }

    // 8) JSON nulo → request sem corpo
    @Test
    void deveRetornarErroQuandoJsonForNulo() throws Exception {
        when(mockRequest.getReader()).thenReturn(null);

        controller.processRequest(mockRequest, mockResponse);

        assertTrue(responseWriter.toString().contains("Status inválido"));
        verify(mockDao, never()).alterarStatus(anyString());
    }
}
