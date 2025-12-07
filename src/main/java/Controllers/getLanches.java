package Controllers;

import DAO.DaoLanche;
import Helpers.ValidadorCookie;
import Model.Lanche;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class getLanches extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Verificação defensiva 1
        if (response != null) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
        }

        boolean resultado = false;

        try {
            // Verificação defensiva 2
            if (request != null) {
                Cookie[] cookies = request.getCookies();

                // Verificação defensiva 3
                if (cookies != null) {
                    // AQUI MUDOU: Chama o método protegido em vez de 'new'
                    ValidadorCookie validar = getValidadorCookie();

                    // Verificação defensiva 4
                    if (validar != null) {
                        // Atenção: no seu código original era validarFuncionario
                        resultado = validar.validarFuncionario(cookies);
                    }
                }
            }
        } catch (java.lang.NullPointerException e) {
            System.out.println(e);
        }

        // Caminho de Sucesso
        if (resultado) {

            // AQUI MUDOU: Pega o DAO pelo método
            DaoLanche lancheDAO = getDaoLanche();

            // Verificação defensiva 5
            if (lancheDAO != null) {
                List<Lanche> lanches = lancheDAO.listarTodos();

                // Verificação defensiva 6
                if (lanches != null) {

                    // AQUI MUDOU: Pega o Gson pelo método
                    Gson gson = getGson();

                    // Verificação defensiva 7
                    if (gson != null) {
                        String json = gson.toJson(lanches);

                        // Verificação defensiva 8
                        if (json != null) {
                            if (response != null) {
                                try (PrintWriter out = response.getWriter()) {
                                    if (out != null) {
                                        out.print(json);
                                        out.flush();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Caminho de Erro (Não autorizado)
            if (response != null) {
                try (PrintWriter out = response.getWriter()) {
                    if (out != null) {
                        out.println("erro");
                    }
                }
            }
        }
    }

    // ==========================================================================
    // MÉTODOS "HOOKS" (Ganchos) PARA O TESTE UNITÁRIO
    // ==========================================================================

    protected ValidadorCookie getValidadorCookie() {
        return new ValidadorCookie();
    }

    protected DaoLanche getDaoLanche() {
        return new DaoLanche();
    }

    protected Gson getGson() {
        return new Gson();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    public String getServletInfo() {
        return "Short description";
    }
}