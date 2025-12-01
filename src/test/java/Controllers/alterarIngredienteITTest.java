package Controllers;

import DAO.DaoIngrediente;
import DAO.DaoUtil; // <--- Importante: Precisamos importar para mockar a conexão
import Helpers.ValidadorCookie;
import Model.Ingrediente;
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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.List;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Teste de INTEGRAÇÃO:
 * Servlet alterarIngrediente + DaoIngrediente real + Postgres real (via Localhost).
 */
public class alterarIngredienteITTest { 

    private alterarIngrediente servlet;
    private ValidadorCookie validadorMock;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws Exception {
        // Mocks só para parte HTTP/autenticação
        validadorMock = mock(ValidadorCookie.class);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);

        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        // Servlet usando Validador mockado, mas DAO ORIGINAL
        servlet = new alterarIngrediente() {
            @Override
            protected ValidadorCookie getValidadorCookie() {
                return validadorMock;
            }
            // NÃO sobrescrevemos getDaoIngrediente() aqui, pois queremos que ele chame o DAO real.
            // O problema de conexão do DAO será resolvido via MockedConstruction no teste.
        };
    }

    @Test
    void deveAlterarIngredienteNoBanco_QuandoDadosValidos() throws Exception {
        // 1) Prepara estado inicial no banco (Conexão direta localhost)
        prepararIngredienteInicialNoBanco(1);

        // 2) Configura request/autenticação
        Cookie[] cookies = { new Cookie("auth", "token_valido") };
        when(request.getCookies()).thenReturn(cookies);
        when(validadorMock.validarFuncionario(cookies)).thenReturn(true);

        // JSON com os NOVOS valores
        String json = "{"
                + "\"id\": 1,"
                + "\"nome\": \"Farinha\","
                + "\"descricao\": \"Farinha fina de trigo\","
                + "\"quantidade\": 10,"
                + "\"ValorCompra\": 5.5,"
                + "\"ValorVenda\": 8.0,"
                + "\"tipo\": \"UN\""
            + "}";

        when(request.getInputStream()).thenReturn(criarInput(json));

        // --- INTERCEPTAÇÃO DA CONEXÃO DO DAO ---
        // Isso resolve o erro "UnknownHostException: db".
        // Quando o DaoIngrediente fizer "new DaoUtil()", nós interceptamos e
        // mandamos o método .conecta() retornar uma conexão real para o LOCALHOST.
        try (MockedConstruction<DaoUtil> mockedDaoUtil = Mockito.mockConstruction(DaoUtil.class,
                (mock, context) -> {
                    when(mock.conecta()).thenAnswer(invocation -> 
                        DriverManager.getConnection(
                            "jdbc:postgresql://localhost:5432/lanchonete", // URL Local
                            "postgres", 
                            "123456" // Senha do seu banco local/docker
                        )
                    );
                })) {

            // 3) Executa a servlet real
            // A servlet vai chamar getDaoIngrediente() -> new DaoIngrediente() -> new DaoUtil()
            // O DaoUtil será o nosso mock configurado acima, conectando no localhost.
            servlet.doPost(request, response);

            // 4) Consulta NO BANCO para validar (usando um DAO auxiliar que também cairá no mock)
            DaoIngrediente daoVerificador = new DaoIngrediente();
            Ingrediente ing = buscarIngredientePorId(daoVerificador, 1);

            assertNotNull(ing, "Ingrediente ID=1 deveria existir no banco");
            assertEquals(1, ing.getId_ingrediente());
            assertEquals("Farinha", ing.getNome());
            assertEquals("Farinha fina de trigo", ing.getDescricao());
            assertEquals(10, ing.getQuantidade());
            assertEquals(5.5, ing.getValor_compra());
            assertEquals(8.0, ing.getValor_venda());
            assertEquals("UN", ing.getTipo());
            assertEquals(1, ing.getFg_ativo());
        }

        // 5) Verifica resposta HTTP
        String resposta = responseWriter.toString();
        assertTrue(resposta.contains("Ingrediente Alterado!"));
        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");
    }

    // --- MÉTODOS AUXILIARES ---

    private Ingrediente buscarIngredientePorId(DaoIngrediente dao, int id) {
        List<Ingrediente> todos = dao.listarTodos();
        for (Ingrediente i : todos) {
            if (i.getId_ingrediente() == id) {
                return i;
            }
        }
        return null;
    }

    // Prepara um registro inicial na tabela tb_ingredientes usando JDBC puro e localhost
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