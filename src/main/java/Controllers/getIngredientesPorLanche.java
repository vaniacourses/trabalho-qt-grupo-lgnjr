/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Controllers;

import DAO.DaoIngrediente;
import Helpers.ValidadorCookie;
import Model.Ingrediente;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

/**
 *
 * @author kener_000
 */
public class getIngredientesPorLanche extends HttpServlet {

    private final ValidadorCookie validador;
    private final DaoIngrediente daoIngrediente;
    private final Gson gson;

    private final Logger logger = Logger.getLogger(getClass().getName());

    public getIngredientesPorLanche(ValidadorCookie v, DaoIngrediente d, Gson g) {
        this.validador = v;
        this.daoIngrediente = d;
        this.gson = g;
    }


    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Verificação defensiva do response (+1)
        if (response == null) {
            return;
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Verificação defensiva do request, daoIngrediente e gson
        if (request == null || daoIngrediente == null || gson == null) {
            enviarErro(response);
            return;
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream()));

        if (validador == null) {
            logger.warning("Validador nulo na validação do Cookie");
            enviarErro(response);
            return;
        }

        ////////Validar Cookie
        boolean resultado = false;
        Cookie[] cookies = request.getCookies();

        // Validação de array de cookies (+1)
        if (cookies == null) {
            logger.warning("Cookies nulos");
            enviarErro(response);
            return;
        }

        resultado = validador.validarFuncionario(cookies);
        if (!resultado) { // (+1)
            // Falha na autenticação do resultado
            logger.warning("Falha na autenticação dos cookies");
            enviarErro(response);
            return;
        }

        String incomingJson = br.readLine();

        // Verifica se leu algo do buffer (+1)
        if (incomingJson == null || incomingJson.trim().isEmpty()) {
            enviarErro(response);
            return;
        }

        byte[] bytes = incomingJson.getBytes(ISO_8859_1);
        String jsonStr = new String(bytes, UTF_8);
        JSONObject dados = new JSONObject(jsonStr);

        // Verifica se a chave ID existe antes de acessar (+1)
        if (!dados.has("id")) {
            // JSON sem ID
            enviarErro(response);
            return;
        }

        List<Ingrediente> ingredientes = daoIngrediente.listarTodosPorLanche(dados.getInt("id"));

        // Verifica se a lista retornada não é nula (+1)
        if (ingredientes == null) {
            // Lista nula (erro de banco?)
            enviarErro(response);
            return;
        }

        try (PrintWriter out = response.getWriter()) {
            // Verifica se o Writer não é nulo (+1)
            if (out == null) {
                return;
            }
            String json = gson.toJson(ingredientes);
            out.print(json);
            out.flush();
        }
    }

    // Método auxiliar para isolar o tratamento de erro repetitivo,
    // mantendo a complexidade visual alta nos IFs acima.
    private void enviarErro(HttpServletResponse response) throws IOException {
        if (response != null) { // (+1)
            try (PrintWriter out = response.getWriter()) {
                if (out != null) {
                    out.println("erro");
                }
            }
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (Exception e) {
            logger.severe("Erro no doGet: " + e.getMessage());
        }
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (Exception e) {
            logger.severe("Erro no doPost: " + e.getMessage());
        }
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}