package Controllers;

import DAO.DaoIngrediente;
import DAO.DaoLanche;
import Helpers.ValidadorCookie;
import Model.Ingrediente;
import Model.Lanche;

import javax.servlet.http.*;
import java.io.*;
import java.util.Iterator;
import org.json.JSONObject;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;

public class SalvarLanche extends HttpServlet {

    // ============================================================================
    // MÉTODOS HTTP
    // ============================================================================

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        try {
            processRequest(req, resp);
        } catch (IOException e) {
            enviarFalhaInterna(resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        try {
            processRequest(req, resp);
        } catch (IOException e) {
            enviarFalhaInterna(resp);
        }
    }

    // ============================================================================
    // PROCESSAMENTO PRINCIPAL
    // ============================================================================

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        if (!validarCookie(request)) {
            enviarErro(response);
            return;
        }

        String jsonStr = lerJson(request);
        if (jsonStr == null) {
            enviarErro(response);
            return;
        }

        JSONObject dados;

        try {
            dados = new JSONObject(jsonStr);
        } catch (Exception e) {
            enviarErro(response);
            return;
        }

        Lanche lanche = criarLanche(dados);
        DaoLanche lancheDao = getDaoLanche();
        DaoIngrediente ingredienteDao = getDaoIngrediente();

        lancheDao.salvar(lanche);

        Lanche lancheComID;
        try {
            lancheComID = lancheDao.pesquisaPorNome(lanche);
        } catch (Exception e) {
            enviarErro(response);
            return;
        }

        if (lancheComID == null) {
            enviarErro(response);
            return;
        }

        if (dados.has("ingredientes")) {
            JSONObject ingredientes = dados.getJSONObject("ingredientes");
            processarIngredientes(ingredientes, lancheComID, ingredienteDao, lancheDao);
        }

        try (PrintWriter out = response.getWriter()) {
            out.println("Lanche Salvo com Sucesso!");
        }
    }

    // ============================================================================
    // SUB-MÉTODOS (redução de complexidade)
    // ============================================================================

    private boolean validarCookie(HttpServletRequest request) {
        try {
            Cookie[] cookies = request.getCookies();
            return cookies != null && getValidadorCookie().validarFuncionario(cookies);
        } catch (Exception e) {
            return false;
        }
    }

    private String lerJson(HttpServletRequest request) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream()));
        String json = br.readLine();

        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        return new String(json.getBytes(ISO_8859_1), UTF_8);
    }

    private Lanche criarLanche(JSONObject dados) {
        Lanche lanche = new Lanche();
        lanche.setNome(dados.optString("nome"));
        lanche.setDescricao(dados.optString("descricao"));
        lanche.setValor_venda(dados.optDouble("ValorVenda"));
        return lanche;
    }

    private void processarIngredientes(JSONObject ingredientes, Lanche lancheComID,
                                       DaoIngrediente ingredienteDao, DaoLanche lancheDao) {

        Iterator<String> keys = ingredientes.keys();

        while (keys.hasNext()) {
            String key = keys.next();

            Ingrediente ingTemp = new Ingrediente();
            ingTemp.setNome(key);

            Ingrediente ingredienteComID;
            try {
                ingredienteComID = ingredienteDao.pesquisaPorNome(ingTemp);
            } catch (Exception e) {
                // ingrediente inexistente é ignorado propositalmente
                continue;
            }

            if (ingredienteComID != null) {
                ingredienteComID.setQuantidade(ingredientes.optInt(key, 0));
                lancheDao.vincularIngrediente(lancheComID, ingredienteComID);
            }
        }
    }

    // ============================================================================
    // RESPOSTAS DE ERRO
    // ============================================================================

    private void enviarErro(HttpServletResponse response) throws IOException {
        try (PrintWriter out = response.getWriter()) {
            out.println("erro");
        }
    }

    private void enviarFalhaInterna(HttpServletResponse response) {
        try (PrintWriter out = response.getWriter()) {
            out.println("erro");
        } catch (IOException ignored) {
            // Nada a fazer — falha silenciosa é intencional
        }
    }

    // ============================================================================
    // SEAMS PARA TESTES
    // ============================================================================

    protected ValidadorCookie getValidadorCookie() {
        return new ValidadorCookie();
    }

    protected DaoLanche getDaoLanche() {
        return new DaoLanche();
    }

    protected DaoIngrediente getDaoIngrediente() {
        return new DaoIngrediente();
    }
}
