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

    private static final String FIELD_STATUS = "status";
    private static final String STATUS_ABERTO = "ABERTO";


   private final DaoStatusLanchonete dao;
   public alterarStatusLanchonete() {
       this.dao = new DaoStatusLanchonete();
   }
   
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
       
       if (json != null) { // (+1)
          
           // Verificação extra de string vazia (+1)
           if (!json.trim().isEmpty()) {
              
               try { // (+1 pelo catch)
                   JSONObject dados = new JSONObject(json);
                  
                   // Verificação se a chave existe antes de pegar (+1)
                   if (dados.has(FIELD_STATUS)) {
                      
                       if (dados.isNull(FIELD_STATUS)) {
                           processarStatusInvalido(response);
                           return;
                       }
                      
                       String novoStatus = dados.getString(FIELD_STATUS);
                       boolean statusValido = false;
                       // Quebrando a condicional composta em ifs separados
                       if (novoStatus != null) { // (+1)
                          
                           if (novoStatus.equals(STATUS_ABERTO)) { // (+1)
                               statusValido = true;
                           } else {
                               // Else if conta como ponto de decisão (+1)
                               if (novoStatus.equals("FECHADO")) {
                                   statusValido = true;
                               }
                           }
                           // Separação da lógica de persistência (+1)
                           if (statusValido) {
                               // Caminho Feliz: Status Correto
                               // Verificação defensiva do DAO (+1)
                               if (this.dao != null) {
                                   this.dao.alterarStatus(novoStatus);
                               }
                               enviarResposta(response, novoStatus);
                           } else {
                               // Caminho Alternativo: Status Inválido vira ABERTO
                               processarStatusInvalido(response);
                           }
                       } else {
                           // Caso status venha nulo no JSON
                           processarStatusInvalido(response);
                       }
                   } else {
                        // JSON sem campo status
                       processarStatusInvalido(response);
                   }
               } catch (JSONException e) {
                   // Tratamento de erro de parse (+1 implícito)
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
   // Métodos auxiliares
   private void processarStatusInvalido(HttpServletResponse response) throws IOException {
       if (this.dao != null) { // (+1)
           this.dao.alterarStatus(STATUS_ABERTO);
       }
       enviarResposta(response, STATUS_ABERTO);
   }
   private void enviarResposta(HttpServletResponse response, String status) throws IOException {
       JSONObject jsonResponse = new JSONObject();
       try {
           jsonResponse.put(FIELD_STATUS, status);
        } catch (JSONException e) {
            // Tratamento simples para evitar bloco vazio
            enviarErro(response);
        }
      
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
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {
            processRequest(request, response);
        } catch (ServletException | IOException e) {
            try {
                enviarErro(response);
            } catch (IOException ex) {
                // log opcional
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        try {
            processRequest(request, response);
        } catch (ServletException | IOException e) {
            try {
                enviarErro(response);
            } catch (IOException ex) {
                // log opcional
            }
        }
    }
}
