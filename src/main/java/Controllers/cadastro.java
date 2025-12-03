package Controllers;

import DAO.DaoCliente;
import DAO.DaoEndereco;
import Model.Cliente;
import Model.Endereco;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONException;
import org.json.JSONObject;

@WebServlet(name = "cadastro", urlPatterns = {"/cadastro"})
public class cadastro extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        BufferedReader br = null;
        if (request.getInputStream() != null) {
             br = new BufferedReader(new InputStreamReader(request.getInputStream()));
        }
        
        String json = "";

        // O try-catch maior captura erros de parsing e validação de formato
        try {
            if (br != null) {
                json = br.readLine();
                
                if (json != null && !json.trim().isEmpty()) {
                    
                    byte[] bytes = json.getBytes(ISO_8859_1); 
                    String jsonStr = new String(bytes, UTF_8);            
                    JSONObject dados = new JSONObject(jsonStr);
                    
                    // --- VALIDAÇÃO DE FORMATO (CAMPOS CRÍTICOS) ---
                    
                    // JSONException: Falha se o campo for nulo, string vazia ou não-numérico
                    int numero = dados.getJSONObject("endereco").getInt("numero"); 
                    String telefone = dados.getJSONObject("usuario").getString("telefone");
                    
                    // 1. VALIDAÇÃO DE CONTEÚDO (REGEX): Verifica se o telefone tem apenas números
                    // Se falhar, lançamos IllegalArgumentException, que será pego no catch
                    if (!telefone.matches("\\d+") || telefone.length() < 8) { 
                        throw new IllegalArgumentException("Telefone inválido: Deve conter apenas dígitos.");
                    }
                    
                    // --- PERSISTÊNCIA ---
                    
                    Endereco endereco = new Endereco();
                    endereco.setBairro(dados.getJSONObject("endereco").getString("bairro"));
                    endereco.setCidade(dados.getJSONObject("endereco").getString("cidade"));
                    endereco.setEstado(dados.getJSONObject("endereco").getString("estado"));
                    endereco.setComplemento(dados.getJSONObject("endereco").getString("complemento"));
                    endereco.setRua(dados.getJSONObject("endereco").getString("rua"));
                    endereco.setNumero(numero); 

                    Cliente cliente = new Cliente();
                    cliente.setNome(dados.getJSONObject("usuario").getString("nome"));
                    cliente.setSobrenome(dados.getJSONObject("usuario").getString("sobrenome"));
                    cliente.setTelefone(telefone); 
                    cliente.setUsuario(dados.getJSONObject("usuario").getString("usuario"));
                    cliente.setSenha(dados.getJSONObject("usuario").getString("senha"));
                    cliente.setFg_ativo(1);
                    cliente.setEndereco(endereco);
                    
                    // MUDANÇA: Usa métodos protegidos para testabilidade
                    DaoCliente clienteDAO = getDaoCliente();
                    DaoEndereco enderecoDAO = getDaoEndereco();

                    enderecoDAO.salvar(endereco);
                    clienteDAO.salvar(cliente);
                    
                    // --- SUCESSO ---
                    try (PrintWriter out = response.getWriter()) {
                        out.println("Usuário Cadastrado!");
                    }

                } else {
                    enviarErro(response, "Dados de requisição vazios ou ausentes.");
                }
            } else {
                 enviarErro(response, "Requisição sem corpo (Buffer nulo).");
            }
            
        } catch (JSONException | IllegalArgumentException e) { 
            // Captura falha de conversão (numero) OU falha de validação manual (telefone)
            System.err.println("Erro de validação no cliente: " + e.getMessage());
            
            // Retorna erro 400 (Bad Request) com mensagem formatada
            enviarErro(response, "Erro nos dados: Verifique se campos numéricos (Número, Telefone) contêm apenas dígitos.");
        } catch (RuntimeException e) {
            // Captura falhas de persistência (erro de banco, etc.)
             System.err.println("Erro grave de persistência: " + e.getMessage());
             enviarErro(response, "Ops... Ocorreu um erro no Cadastro, Tente novamente mais Tarde!");
        }
    }
    
    // --- MÉTODOS AUXILIARES E HOOKS ---

    // Retorna mensagem formatada para erros de validação (HTTP 400)
    private void enviarErro(HttpServletResponse response, String mensagem) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST); 
        response.setContentType("application/json");

        try (PrintWriter out = response.getWriter()) {
            JSONObject erro = new JSONObject();
            erro.put("status", "erro");
            erro.put("mensagem", mensagem);
            out.print(erro.toString());
        } catch (Exception ignore) {}
    }

    // HOOKS (Ganchos para Testes)
    protected DaoCliente getDaoCliente() { return new DaoCliente(); }
    protected DaoEndereco getDaoEndereco() { return new DaoEndereco(); }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException { processRequest(request, response); }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException { processRequest(request, response); }

    @Override
    public String getServletInfo() { return "Short description"; }
}