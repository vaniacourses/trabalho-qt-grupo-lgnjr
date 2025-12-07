package Controllers;

import DAO.DaoIngrediente;
import Helpers.ValidadorCookie;
import Model.Ingrediente;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;

public class AlterarIngrediente extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AlterarIngrediente.class.getName());

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        BufferedReader br;
        try {
            br = new BufferedReader(new InputStreamReader(request.getInputStream()));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erro ao obter input stream da requisição.", e);
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Erro ao ler dados.");
            return;
        }

       
        Cookie[] cookies = request.getCookies();
        ValidadorCookie validar = getValidadorCookie();

        boolean autorizado = validar != null && cookies != null && validar.validarFuncionario(cookies);

        if (!autorizado) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Não autorizado");
            return;
        }

      
        String jsonLinha;
        try {
            jsonLinha = br.readLine();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Erro ao ler o JSON enviado.", e);
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Erro ao ler JSON.");
            return;
        }

        if (jsonLinha == null || jsonLinha.trim().isEmpty()) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "JSON vazio.");
            return;
        }

        // Converte para UTF-8
        String jsonStr = new String(jsonLinha.getBytes(ISO_8859_1), UTF_8);

       
        Ingrediente ingrediente = new Ingrediente();
        try {
            JSONObject dados = new JSONObject(jsonStr);

            ingrediente.setId_ingrediente(dados.getInt("id"));
            ingrediente.setNome(dados.getString("nome"));
            ingrediente.setDescricao(dados.getString("descricao"));
            ingrediente.setQuantidade(dados.getInt("quantidade"));
            ingrediente.setValor_compra(dados.getDouble("ValorCompra"));
            ingrediente.setValor_venda(dados.getDouble("ValorVenda"));
            ingrediente.setTipo(dados.getString("tipo"));
            ingrediente.setFg_ativo(1);

        } catch (JSONException | NumberFormatException e) {
           
            LOGGER.log(Level.WARNING, "Erro no formato do JSON enviado: {0}", jsonStr);
            LOGGER.log(Level.WARNING, "Detalhes do erro ao processar JSON.", e);

            sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Erro no formato dos dados: " + e.getMessage());
            return;
        }

        DaoIngrediente dao = getDaoIngrediente();
        try {
            dao.alterar(ingrediente);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Falha ao alterar ingrediente no banco de dados.", e);
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Erro ao atualizar ingrediente.");
            return;
        }

    
        sendSuccess(response, "Ingrediente Alterado!");
    }

 
    private void sendError(HttpServletResponse response, int status, String mensagem) {
        try {
            response.setStatus(status);
            try (PrintWriter out = response.getWriter()) {
                out.println(mensagem);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Falha ao enviar resposta de erro ao cliente.", e);
        }
    }

    private void sendSuccess(HttpServletResponse response, String mensagem) {
        try (PrintWriter out = response.getWriter()) {
            out.println(mensagem);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Falha ao enviar resposta de sucesso ao cliente.", e);
        }
    }


    protected ValidadorCookie getValidadorCookie() {
        return new ValidadorCookie();
    }

    protected DaoIngrediente getDaoIngrediente() {
        return new DaoIngrediente();
    }

   
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        processRequest(request, response);
    }

    @Override
    public String getServletInfo() {
        return "Servlet responsável por alterar ingredientes.";
    }
}
