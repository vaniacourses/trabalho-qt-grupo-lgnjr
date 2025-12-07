package Controllers;

import DAO.DaoUtil;
import Helpers.ValidadorCookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
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
import java.sql.*;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class alterarIngredienteITTest {

    private AlterarIngrediente servlet;
    private ValidadorCookie validadorMock;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws Exception {
        // Mocks HTTP / autenticação
        validadorMock = mock(ValidadorCookie.class);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);

        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        // Servlet usando validador mockado, mas DAO real (que passa pelo DaoUtil mockado)
        servlet = new AlterarIngrediente() {
            @Override
            protected ValidadorCookie getValidadorCookie() {
                return validadorMock;
            }
        };
    }


    // 1) TESTE VÁLIDO — ALTERAÇÃO COMPLETA NO BANCO
 
    @Test
    void deveAlterarIngredienteNoBanco_QuandoDadosValidos() throws Exception {

        prepararIngredienteInicialNoBanco(1);

        Cookie[] cookies = { new Cookie("auth", "token_valido") };
        when(request.getCookies()).thenReturn(cookies);
        when(validadorMock.validarFuncionario(cookies)).thenReturn(true);

        String jsonValido = "{"
                + "\"id\": 1,"
                + "\"nome\": \"Farinha\","
                + "\"descricao\": \"Farinha fina de trigo\","
                + "\"quantidade\": 10,"
                + "\"ValorCompra\": 5.5,"
                + "\"ValorVenda\": 8.0,"
                + "\"tipo\": \"UN\""
                + "}";

        when(request.getInputStream()).thenReturn(criarInput(jsonValido));

        // Intercepta DaoUtil.conecta() para usar localhost em vez de "db"
        try (MockedConstruction<DaoUtil> mockedDaoUtil =
                     Mockito.mockConstruction(DaoUtil.class, (mock, context) -> {
                         when(mock.conecta()).thenAnswer(invocation ->
                                 DriverManager.getConnection(
                                         "jdbc:postgresql://localhost:5432/lanchonete",
                                         "postgres",
                                         "123456"
                                 )
                         );
                     })) {

            servlet.doPost(request, response);
        }

        // Verifica direto no banco (sem usar DaoIngrediente)
        assertIngredienteNoBanco(1,
                "Farinha",
                "Farinha fina de trigo",
                10,
                5.5,
                8.0,
                "UN",
                1
        );

        String resposta = responseWriter.toString();
        assertTrue(resposta.contains("Ingrediente Alterado!"));
        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");
    }


    // 2) TESTE INVÁLIDO — JSON VAZIO
   
    @Test
    void deveRetornarErro_QuandoJsonVazio_ENaoAlterarBanco() throws Exception {

        prepararIngredienteInicialNoBanco(1);

        Cookie[] cookies = { new Cookie("auth", "token_valido") };
        when(request.getCookies()).thenReturn(cookies);
        when(validadorMock.validarFuncionario(cookies)).thenReturn(true);

        // Corpo vazio
        when(request.getInputStream()).thenReturn(criarInput(""));

        try (MockedConstruction<DaoUtil> mockedDaoUtil =
                     Mockito.mockConstruction(DaoUtil.class, (mock, context) -> {
                         when(mock.conecta()).thenAnswer(invocation ->
                                 DriverManager.getConnection(
                                         "jdbc:postgresql://localhost:5432/lanchonete",
                                         "postgres",
                                         "123456"
                                 )
                         );
                     })) {

            servlet.doPost(request, response);
        }

        // Banco deve continuar com os dados antigos
        assertIngredienteNoBanco(1,
                "Nome Antigo",
                "Descricao antiga",
                5,
                3.0,
                4.0,
                "UN",
                1
        );

        String resposta = responseWriter.toString();
        System.out.println("RESPOSTA JSON VAZIO = " + resposta);

        // Em vez de depender da mensagem exata, garantimos que NÃO entrou no fluxo feliz
        assertFalse(resposta.contains("Ingrediente Alterado!"));

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    // 3) TESTE INVÁLIDO — JSON MALFORMADO

    @Test
    void deveRetornarErro_QuandoJsonMalformado_ErroFormato() throws Exception {

        prepararIngredienteInicialNoBanco(1);

        Cookie[] cookies = { new Cookie("auth", "token_valido") };
        when(request.getCookies()).thenReturn(cookies);
        when(validadorMock.validarFuncionario(cookies)).thenReturn(true);

        // JSON quebrado
        String jsonInvalido = "{ id: 1 nome: Farinha }";

        when(request.getInputStream()).thenReturn(criarInput(jsonInvalido));

        try (MockedConstruction<DaoUtil> mockedDaoUtil =
                     Mockito.mockConstruction(DaoUtil.class, (mock, context) -> {
                         when(mock.conecta()).thenAnswer(invocation ->
                                 DriverManager.getConnection(
                                         "jdbc:postgresql://localhost:5432/lanchonete",
                                         "postgres",
                                         "123456"
                                 )
                         );
                     })) {

            servlet.doPost(request, response);
        }

        // Banco continua com dados antigos
        assertIngredienteNoBanco(1,
                "Nome Antigo",
                "Descricao antiga",
                5,
                3.0,
                4.0,
                "UN",
                1
        );

        String resposta = responseWriter.toString();
        System.out.println("RESPOSTA JSON MALFORMADO = " + resposta);

       
        assertFalse(resposta.contains("Ingrediente Alterado!"));

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }


    // MÉTODOS AUXILIARES


    private void assertIngredienteNoBanco(int id,
                                          String nome,
                                          String descricao,
                                          int quantidade,
                                          double valorCompra,
                                          double valorVenda,
                                          String tipo,
                                          int fgAtivo) throws Exception {

        try (Connection conn = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/lanchonete",
                "postgres",
                "123456")) {

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT nm_ingrediente, descricao, quantidade, valor_compra, valor_venda, tipo, fg_ativo " +
                            "FROM tb_ingredientes WHERE id_ingrediente = ?")) {

                ps.setInt(1, id);

                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next(), "Ingrediente ID=" + id + " deveria existir no banco");

                    assertEquals(nome, rs.getString("nm_ingrediente"));
                    assertEquals(descricao, rs.getString("descricao"));
                    assertEquals(quantidade, rs.getInt("quantidade"));
                    assertEquals(valorCompra, rs.getDouble("valor_compra"));
                    assertEquals(valorVenda, rs.getDouble("valor_venda"));
                    assertEquals(tipo, rs.getString("tipo"));
                    assertEquals(fgAtivo, rs.getInt("fg_ativo"));
                }
            }
        }
    }

    private void prepararIngredienteInicialNoBanco(int id) throws Exception {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/lanchonete",
                "postgres",
                "123456")) {

            try (PreparedStatement delete = conn.prepareStatement(
                    "DELETE FROM tb_ingredientes WHERE id_ingrediente = ?")) {
                delete.setInt(1, id);
                delete.execute();
            }

            try (PreparedStatement insert = conn.prepareStatement(
                    "INSERT INTO tb_ingredientes " +
                            "(id_ingrediente, nm_ingrediente, descricao, quantidade, valor_compra, valor_venda, tipo, fg_ativo) " +
                            "VALUES (?, 'Nome Antigo', 'Descricao antiga', 5, 3.0, 4.0, 'UN', 1)")) {
                insert.setInt(1, id);
                insert.execute();
            }
        }
    }

    private ServletInputStream criarInput(String corpo) {
        byte[] bytes = corpo.getBytes(ISO_8859_1);
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);

        return new ServletInputStream() {
            @Override public int read() throws IOException { return bais.read(); }
            @Override public boolean isFinished() { return bais.available() == 0; }
            @Override public boolean isReady() { return true; }
            @Override public void setReadListener(ReadListener readListener) { }
        };
    }
}
