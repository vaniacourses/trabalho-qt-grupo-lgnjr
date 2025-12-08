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

        BufferedReader br = null;
        // Verificação defensiva do request (+1)
        if (request != null) {
            br = new BufferedReader(new InputStreamReader(request.getInputStream()));
        }

        String incomingJson = "";

        ////////Validar Cookie
        boolean resultado = false;

        try{
            // Validação extra dentro do try (+1)
            if (request != null) {
                Cookie[] cookies = request.getCookies();

                // Validação de array de cookies (+1)
                if (cookies != null) {
                    // Validação da instância do helper (+1)
                    if (validador != null) {
                        resultado = validador.validarFuncionario(cookies);
                    }
                }
            }
        } catch(java.lang.NullPointerException e){
            // Catch conta como ponto de decisão (+1)
            logger.warning("Erro na validação do Cookie: " + e.getMessage());
        }
        //////////////

        // Decompondo 'if((br != null) && resultado)' em estrutura aninhada

        if (br != null) { // (+1)

            if (resultado) { // (+1)

                incomingJson = br.readLine();

                // Verifica se leu algo do buffer (+1)
                if (incomingJson != null) {

                    // Verifica se não está vazio (+1)
                    if (!incomingJson.trim().isEmpty()) {

                        byte[] bytes = incomingJson.getBytes(ISO_8859_1);
                        String jsonStr = new String(bytes, UTF_8);
                        JSONObject dados = new JSONObject(jsonStr);

                        // Verifica se o JSON foi criado corretamente (+1)
                        if (dados != null) {

                            // Verifica se a chave ID existe antes de acessar (+1)
                            if (dados.has("id")) {
                                // Verifica se o DAO foi instanciado (+1)
                                if (daoIngrediente != null) {
                                    List<Ingrediente> ingredientes = daoIngrediente.listarTodosPorLanche(dados.getInt("id"));

                                    // Verifica se a lista retornada não é nula (+1)
                                    if (ingredientes != null) {
                                        String json = gson.toJson(ingredientes);

                                        try (PrintWriter out = response.getWriter()) {
                                            // Verifica se o Writer não é nulo (+1)
                                            if (out != null) {
                                                out.print(json);
                                                out.flush();
                                            }
                                        }
                                    } else {
                                        // Lista nula (erro de banco?)
                                        enviarErro(response);
                                    }
                                } else {
                                    enviarErro(response);
                                }
                            } else {
                                // JSON sem ID
                                enviarErro(response);
                            }
                        } else {
                            enviarErro(response);
                        }
                    } else {
                        // JSON string vazia
                        enviarErro(response);
                    }
                } else {
                    // Buffer retornou linha nula
                    enviarErro(response);
                }
            } else {
                // Falha de autenticação (resultado = false)
                enviarErro(response);
            }
        } else {
            // Falha no BufferedReader
            enviarErro(response);
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