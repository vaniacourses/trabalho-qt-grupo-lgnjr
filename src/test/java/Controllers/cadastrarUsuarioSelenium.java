package Controllers;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import static org.junit.jupiter.api.Assertions.assertEquals; 
import java.time.Duration;

class cadastrarUsuarioSelenium {

    private WebDriver driver;

    private WebDriverWait wait;

    private final String BASE_URL = "http://localhost:8080/";

    @BeforeEach
    void setUp() {

        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();

        options.addArguments("--remote-allow-origins=*");

        driver = new ChromeDriver(options);

        driver.manage().window().maximize();

        wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    }

    @AfterEach
    void tearDown() {

        if (driver != null) {

            driver.quit();

        }

    }

    @Test
    void deveCadastrarUsuario() throws InterruptedException {

        driver.get(BASE_URL + "/view/home/home.html");


        try {

            WebElement botao = wait.until(ExpectedConditions.elementToBeClickable(

                    By.xpath("//button[contains(text(),'Acessar Cardápio')]")));

            botao.click();

        } catch (Exception e) {

            System.out.println("Botão de cardápio não necessário ou não encontrado.");

        }

        try {

            Alert alerta = wait.until(ExpectedConditions.alertIsPresent());

            alerta.accept();

        } catch (TimeoutException e) {

        }

        Thread.sleep(2000);

        WebElement carrinho = wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Meu Carrinho")));

        carrinho.click();

        WebElement funcionarioLink = wait
                .until(ExpectedConditions.elementToBeClickable(By.partialLinkText("Funcionário?")));

        funcionarioLink.click();

        WebElement campoUsuario = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("loginInput")));

        WebElement campoSenha = driver.findElement(By.id("senhaInput"));

        WebElement botaoEntrar = driver.findElement(By.xpath("//button[contains(text(),'Entrar')]"));

        campoUsuario.sendKeys("admin");

        campoSenha.sendKeys("admin");

        botaoEntrar.click();

        Thread.sleep(2000);

        WebElement botaoAbrirLanchonete = wait.until(ExpectedConditions.elementToBeClickable(

                By.xpath("//button[contains(text(),'Abrir')]")));

        botaoAbrirLanchonete.click();

        Thread.sleep(2000);

        WebElement botaoLogout = wait.until(ExpectedConditions.elementToBeClickable(

                By.xpath("//button[contains(text(),'Logout')]")

        ));

        botaoLogout.click();

        Thread.sleep(2000);

        try {

            Alert alerta = wait.until(ExpectedConditions.alertIsPresent());

            alerta.accept();

        } catch (TimeoutException e) {

        }

        Thread.sleep(2000);

        try {

            WebElement botaoCa = wait.until(ExpectedConditions.elementToBeClickable(

                    By.xpath("//button[contains(text(),'Acessar Cardápio')]")));

            botaoCa.click();

        } catch (Exception e) {

            System.out.println("Botão de cardápio não necessário ou não encontrado.");

        }

        Thread.sleep(2000);

        WebElement carrinhoDois = wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Meu Carrinho")));

        carrinhoDois.click();

        Thread.sleep(2000);

        WebElement linkCadastro = wait.until(ExpectedConditions.elementToBeClickable(

                By.partialLinkText("Crie uma nova conta")

        ));

        linkCadastro.click();

        Thread.sleep(2000);

        WebElement campoNome = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("nome")));

        campoNome.sendKeys("Ana");

        WebElement sobrenomeUsuarioNovo = wait
                .until(ExpectedConditions.visibilityOfElementLocated(By.name("sobrenome")));

        sobrenomeUsuarioNovo.sendKeys("Silva");

        // TELEFONE COM LETRAS ---

        WebElement telefoneUsuarioNovo = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("telefone")));
        
        // Inserir valor inválido e tentar enviar
        telefoneUsuarioNovo.sendKeys("ABCDE"); 

        WebElement usuarioNovo = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("usuario")));
        usuarioNovo.sendKeys("anateste");

        WebElement campoSenhaNova = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("senha")));
        campoSenhaNova.sendKeys("12345");

        WebElement campoRua = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("rua")));
        campoRua.sendKeys("Rua");

        WebElement campoNumero = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("numero")));
        campoNumero.sendKeys("1"); // Valor numérico correto para não causar erro aqui

        WebElement campoBairro = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("bairro")));
        campoBairro.sendKeys("bairro");

        WebElement campoComplemento = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("complemento")));

        campoComplemento.sendKeys("A");

        WebElement campoCidade = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("cidade")));

        campoCidade.sendKeys("Cidade");

        WebElement selectElement = driver.findElement(By.name("estado"));

        Select selectEstado = new Select(selectElement);

        try {

            selectEstado.selectByIndex(1);

        } catch (Exception e) {

            selectElement.sendKeys(Keys.DOWN);

        }

        Thread.sleep(2000);

        WebElement btnCadastrar = wait.until(ExpectedConditions.elementToBeClickable(

                By.xpath("//button[contains(text(),'Cadastrar')]")

        ));

        btnCadastrar.click(); // Tenta cadastrar com erro no Telefone

        Thread.sleep(2000);

        // ERRO NO TELEFONE ---
        try {
            Alert alerta = wait.until(ExpectedConditions.alertIsPresent());
            String mensagemErro = alerta.getText();
            
            // Verifica se a mensagem é a esperada
            assertEquals("Ops... Ocorreu um erro no Cadastro, Tente novamente mais Tarde!", 
                         mensagemErro, 
                         "A mensagem de erro no Telefone está incorreta.");
            
            alerta.accept();
        } catch (TimeoutException e) {
        }
        Thread.sleep(2000);

        // NÚMERO COM LETRAS

        // Corrige o Telefone (Re-encontra e insere o valor correto)
        telefoneUsuarioNovo = driver.findElement(By.name("telefone"));
        telefoneUsuarioNovo.clear();
        telefoneUsuarioNovo.sendKeys("21999999999");
        
        //  ERRO NO NÚMERO
        campoNumero = driver.findElement(By.name("numero"));
        campoNumero.clear();
        campoNumero.sendKeys("Zero"); 

        btnCadastrar = driver.findElement(By.xpath("//button[contains(text(),'Cadastrar')]"));
        btnCadastrar.click(); // Tenta cadastrar com erro no Número

        // ERRO NO NÚMERO
        try {
            Alert alerta = wait.until(ExpectedConditions.alertIsPresent());
            String mensagemErro = alerta.getText();
            
            
            assertEquals("Ops... Ocorreu um erro no Cadastro, Tente novamente mais Tarde!", 
                         mensagemErro, 
                         "A mensagem de erro no Número está incorreta.");
            
            alerta.accept();
        } catch (TimeoutException e) {
        }

        // CORREÇÃO FINAL

        // Corrige o Número
        campoNumero = driver.findElement(By.name("numero"));
        campoNumero.clear();
        campoNumero.sendKeys("1"); 
        
        Thread.sleep(2000);

        btnCadastrar = driver.findElement(By.xpath("//button[contains(text(),'Cadastrar')]"));
        btnCadastrar.click(); 
        
        Thread.sleep(2000);

        try {

            Alert alerta = wait.until(ExpectedConditions.alertIsPresent());
            alerta.accept();

        } catch (TimeoutException e) {

        }
        Thread.sleep(2000);

    }
}