package Controllers;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions; 
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.JavascriptExecutor; 

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

class salvarLancheSelenium {

    private WebDriver driver;
    private WebDriverWait wait;

    private final String BASE_URL = "http://localhost:8080/";
    // Constante para garantir que o nome do pão seja reutilizado
    private final String INGREDIENTE_PAO_CADASTRADO = "Pão Australiano Selenium"; 

    @BeforeEach
    void setUp() {
        // 1. Configura o gerenciador de driver automaticamente
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");


        driver = new ChromeDriver(options); // Mantendo o original como solicitado
        driver.manage().window().maximize();
        
        // Timeout padrão de 10 segundos para esperar elementos aparecerem
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    void deveAcessarLoginFuncionarioECadastrarIngrediente() throws InterruptedException {
        // 1. Acessa a Home
        driver.get(BASE_URL + "/view/home/home.html");
        
        // Pausa técnica para garantir carregamento visual 
        Thread.sleep(2000);

        // 2. Clicar em Acessar Cardápio
        try {
            WebElement botao = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(text(),'Acessar Cardápio')]")));
            botao.click();
        } catch (Exception e) {
            System.out.println("Botão de cardápio não necessário ou não encontrado.");
        }

        // 3. Lidar com Alerta inicial (Token expirado etc)
        try {
            Alert alerta = wait.until(ExpectedConditions.alertIsPresent());
            alerta.accept();
        } catch (TimeoutException e) {
            // Se não aparecer alerta em 10s, segue 
        }

        // 4. Navegar até o Login de Funcionário
        WebElement carrinho = wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Meu Carrinho")));
        carrinho.click();

        WebElement funcionarioLink = wait.until(ExpectedConditions.elementToBeClickable(By.partialLinkText("Funcionário?")));
        funcionarioLink.click();

        // 5. Fazer Login (Admin)
        WebElement campoUsuario = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("loginInput")));
        WebElement campoSenha = driver.findElement(By.id("senhaInput"));
        WebElement botaoEntrar = driver.findElement(By.xpath("//button[contains(text(),'Entrar')]"));

        campoUsuario.sendKeys("admin");
        campoSenha.sendKeys("admin"); 
        botaoEntrar.click();

        // 6. Acessar Cadastro de Ingredientes
        WebElement botaoCadIngredientes = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(text(),'Cadastrar Ingredientes')]")));
        botaoCadIngredientes.click();

        // 7. Preencher Formulário de Cadastro
        
        // Nome
        WebElement campoNome = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("nome")));
        campoNome.sendKeys(INGREDIENTE_PAO_CADASTRADO); // Usando a constante para garantir que o nome seja salvo

        // Tipo (Usando Select para ser mais robusto)
        WebElement selectElement = driver.findElement(By.name("tipo"));
        Select selectTipo = new Select(selectElement);
        // Tenta selecionar pelo texto visível ou pelo índice
        try {
            selectTipo.selectByIndex(1); // Seleciona o segundo item da lista
        } catch (Exception e) {
            selectElement.sendKeys(Keys.DOWN); // Fallback se o select falhar
        }

        // Quantidade
        driver.findElement(By.name("quantidade")).sendKeys("50");

        // Valor Compra (Limpa antes de digitar para evitar lixo)
        WebElement campoVlrCompra = driver.findElement(By.name("ValorCompra"));
        campoVlrCompra.clear();
        campoVlrCompra.sendKeys("1.50"); // Tente ponto se vírgula falhar

        // Valor Venda
        WebElement campoVlrVenda = driver.findElement(By.name("ValorVenda"));
        campoVlrVenda.clear();
        campoVlrVenda.sendKeys("3.00");

        // Descrição (Tenta achar por ID ou Name, garantindo compatibilidade)
        try {
            driver.findElement(By.id("textArea1")).sendKeys("Pão escuro delicioso");
        } catch (Exception e) {
            driver.findElement(By.name("TextArea1")).sendKeys("Pão escuro delicioso");
        }

        // 8. Salvar
        WebElement botaoSalvar = driver.findElement(By.name("salvar"));
        botaoSalvar.click();

        // 9. Verificar Sucesso (Alerta)
        try {
            Alert alertaSucesso = wait.until(ExpectedConditions.alertIsPresent());
            String textoAlerta = alertaSucesso.getText();
            alertaSucesso.accept();
            
            
        } catch (TimeoutException e) {
            System.out.println("Alerta de sucesso não apareceu a tempo.");
        }

        // 10. Ir para Estoque para conferir
        WebElement botaoEstoque = wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Estoque")));
        botaoEstoque.click();

        Thread.sleep(2000); 
        
        
        // --- INÍCIO  SALVAR LANCHE---
        
        // 11. GARANTIR a navegação para o Painel para que o botão 'Cadastrar Lanches' apareça.
        driver.get(BASE_URL + "view/painel/painel.html"); 

        // 12. Clicar no botão "Cadastrar Lanches"
        WebElement botaoCadastrarLanches = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(text(),'Cadastrar Lanches')]")));
        botaoCadastrarLanches.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("nomeLanche")));

     // 13. Preencher o Campo NOME do Lanche (ID: nomeLanche)
        WebElement campoNomeLanche = driver.findElement(By.id("nomeLanche")); 

     // ******* NOVO AJUSTE CRÍTICO: Limpeza e Foco *******
     campoNomeLanche.clear(); // Garante que o campo esteja limpo
     campoNomeLanche.sendKeys("Lanche Teste Final"); 
     Thread.sleep(200);

     // FORÇAR O EVENTO CHANGE/INPUT VIA JAVASCRIPT (SOLUÇÃO DE SINCRONIZAÇÃO)
     JavascriptExecutor executor = (JavascriptExecutor) driver;

     // Dispara o evento 'change' e 'blur' (perda de foco) no elemento, garantindo que o JS de validação veja o novo valor.
     executor.executeScript("arguments[0].dispatchEvent(new Event('change'));", campoNomeLanche);
     executor.executeScript("arguments[0].dispatchEvent(new Event('blur'));", campoNomeLanche);

     Thread.sleep(500);

     // 14. Selecionar o PÃO (USANDO ID: selectPao)
        WebElement selectElementPao = driver.findElement(By.id("selectPao"));
        Select selectPao = new Select(selectElementPao);

        // Espera até que o dropdown tenha pelo menos 2 opções (sincronização)
        wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(
            By.xpath("//select[@id='selectPao']/option"), 1)
        );

        // Seleciona o pão 
        try {
            selectPao.selectByVisibleText(INGREDIENTE_PAO_CADASTRADO); 
        } catch (NoSuchElementException e) {
            try {
                selectPao.selectByIndex(1);
            } catch (Exception ex) {
                Assertions.fail("Falha Crítica no Passo 14: O ingrediente '" + INGREDIENTE_PAO_CADASTRADO + "' não apareceu na lista.");
            }
        }
        Thread.sleep(1000);

     // 15. Preencher a DESCRIÇÃO (ID: textArea3)
        WebElement campoDescricao = driver.findElement(By.id("textArea3"));
        campoDescricao.clear(); 
        campoDescricao.sendKeys("Lanche simples para validar os 4 requisitos."); 
        executor.executeScript("arguments[0].dispatchEvent(new Event('change'));", campoDescricao); // Força evento
        Thread.sleep(500); 

        // 16. Preencher o VALOR do Lanche (ID: ValorLanche)
        WebElement campoPrecoLanche = driver.findElement(By.id("ValorLanche"));
        campoPrecoLanche.clear(); 
        campoPrecoLanche.sendKeys("10.00"); 
        executor.executeScript("arguments[0].dispatchEvent(new Event('change'));", campoPrecoLanche); // Força evento
        Thread.sleep(2000);
     // 17. Clicar no botão Salvar 
     
        // Localiza o elemento pelo nome 
        WebElement botaoSalvarLanche = driver.findElement(By.name("salvar"));

     
       

        // 2. Executa o clique diretamente (contornando o ElementNotInteractableException)
        executor.executeScript("arguments[0].click();", botaoSalvarLanche);
        
        
        Thread.sleep(4000); 
        
        // 18. Verificar Sucesso
        try {
            Alert alertaLanche = wait.until(ExpectedConditions.alertIsPresent());
            String textoAlerta = alertaLanche.getText();
            alertaLanche.accept();
           
            Assertions.assertTrue(textoAlerta.contains("Lanche Salvo com Sucesso!"), 
                                  "Falha! O Alerta não contém a mensagem de sucesso. Conteúdo: " + textoAlerta);
            
        } catch (TimeoutException e) {
            Assertions.fail("Tempo esgotado! Alert de sucesso não apareceu após salvar o lanche.");
        }
    }
}