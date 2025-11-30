package Controllers;

import static org.junit.jupiter.api.Assertions.*;

import DAO.DaoStatusLanchonete;
import DAO.DaoUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.*;

public class DaoStatusLanchoneteIntegracaoTest {

    private static Connection connection;
    private static DaoStatusLanchonete dao;

    @BeforeAll
    static void setup() {
        // Usa o DaoUtil REAL
        DaoUtil util = new DaoUtil();
        connection = util.conecta();

        dao = new DaoStatusLanchonete();

        limparTabela();
    }

    private static void limparTabela() {
        try {
            PreparedStatement stmt = connection.prepareStatement("DELETE FROM tb_status_lanchonete");
            stmt.execute();
            stmt.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void resetarTabela() {
        limparTabela();
    }

    @Test
    void testAlterarStatusInsereNoBanco() throws Exception {
        dao.alterarStatus("ABERTO");

        PreparedStatement stmt = connection.prepareStatement(
            "SELECT status FROM tb_status_lanchonete ORDER BY id_status DESC LIMIT 1"
        );

        ResultSet rs = stmt.executeQuery();
        assertTrue(rs.next());
        assertEquals("ABERTO", rs.getString("status"));
    }

    @Test
    void testGetStatusRetornaUltimoStatus() throws Exception {
        dao.alterarStatus("FECHADO");
        dao.alterarStatus("ABERTO");

        String status = dao.getStatus();

        assertEquals("ABERTO", status);
    }

    @Test
    void testGetStatusRetornaFechadoQuandoTabelaVazia() {
        String status = dao.getStatus();
        assertEquals("FECHADO", status);
    }
}