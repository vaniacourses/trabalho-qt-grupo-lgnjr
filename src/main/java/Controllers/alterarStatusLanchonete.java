package Controllers;

import DAO.DaoStatusLanchonete;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONException;
import org.json.JSONObject;

public class alterarStatusLanchonete extends HttpServlet {

    private DaoStatusLanchonete dao;

    public alterarStatusLanchonete(DaoStatusLanchonete mockDao) {
        this.dao = mockDao;
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        // Verificação defensiva inicial (+1)
        if (response != null) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
        }

        BufferedReader br = null;
        // Verificação defensiva do request (+1)
        if (request != null) {
            br = request.getReader();
        }

        String json = null;
        // Verificação defensiva do buffer (+1)
        if (br != null) {
            json = br.readLine();
        }

        // Ponto de decisão principal
        if (json != null) { // (+1)
            
            // Verificação extra de string vazia (+1)
            if (!json.trim().isEmpty()) {
                
                try { // (+1 pelo catch)
                    JSONObject dados = new JSONObject(json);
                    
                    // Verificação se a chave existe antes de pegar (+1)
                    if (dados.has("status")) {
                        
                        if (dados.isNull("status")) {
                            processarStatusInvalido(response, this.dao);
                            return;
                        }
                        String novoStatus = dados.getString("status");
                        boolean statusValido = false;

                        // Quebrando a condicional composta (ABERTO || FECHADO) em ifs separados
                        if (novoStatus != null) { // (+1)
                            
                            if (novoStatus.equals("ABERTO")) { // (+1)
                                statusValido = true;
                            } else {
                                // Else if conta como ponto de decisão (+1)
                                if (novoStatus.equals("FECHADO")) { 
                                    statusValido = true;
                                }
                            }

                            // Separação da lógica de persistência baseada na flag (+1)
                            if (statusValido) {
                                // Caminho Feliz: Status Correto
                                this.dao.alterarStatus(novoStatus);
                                enviarResposta(response, novoStatus);
                            } else {
                                // Caminho Alternativo: Status Inválido vira ABERTO
                                processarStatusInvalido(response, this.dao);
                            }
                        } else {
                            // Caso status venha nulo no JSON
                            processarStatusInvalido(response, this.dao);
                        }
                    } else {
                         // JSON sem campo status, trata como inválido (default ABERTO)
                        processarStatusInvalido(response, this.dao);
                    }
                } catch (JSONException e) {
                    // Tratamento de erro de parse (+1 implícito no fluxo)
                    enviarErro(response);
                }
            } else {
                // JSON vazio mas não nulo
                enviarErro(response);
            }
        } else {
            // JSON nulo (request sem corpo)
            enviarErro(response);
        }
    }

    // Método auxiliar para isolar o "Default Case" (Status inválido vira ABERTO)
    // Extrair métodos não reduz a complexidade total do sistema, mas ajuda a organizar o caos criado acima.
    private void processarStatusInvalido(HttpServletResponse response, DaoStatusLanchonete dao) throws IOException {
        if (this.dao != null) { // (+1)
            this.dao.alterarStatus("ABERTO");
        }
        enviarResposta(response, "ABERTO");
    }

    private void enviarResposta(HttpServletResponse response, String status) throws IOException {
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("status", status);
        
        if (response != null) { // (+1)
            try (PrintWriter out = response.getWriter()) {
                if (out != null) { // (+1)
                    out.print(jsonResponse.toString());
                    out.flush();
                }
            }
        }
    }

    private void enviarErro(HttpServletResponse response) throws IOException {
        if (response != null) {
            try (PrintWriter out = response.getWriter()) {
                out.println("Status inválido");
            }
        }
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
}