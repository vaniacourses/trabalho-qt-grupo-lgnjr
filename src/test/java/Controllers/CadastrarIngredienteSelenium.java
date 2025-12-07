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

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;

class CadastrarIngredienteSelenium {

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
        if (driver != null) driver.quit();
    }

    @Test
    void deveAcessarLoginFuncionarioECadastrarIngrediente() throws InterruptedException {

        // 1. Home
        driver.get(BASE_URL + "/view/home/home.html");
        Thread.sleep(2000);

        // 2. Acessar cardápio
        try {
            WebElement botao = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(text(),'Acessar Cardápio')]")));
            botao.click();
        } catch (Exception ignored) {}

        // 3. Lidar com alerta inicial
        try {
            Alert alerta = wait.until(ExpectedConditions.alertIsPresent());
            alerta.accept();
        } catch (TimeoutException ignored) {}

        // 4. Abrir login funcionário
        wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Meu Carrinho"))).click();
        wait.until(ExpectedConditions.elementToBeClickable(By.partialLinkText("Funcionário?"))).click();

        // 5. Login admin
        WebElement campoUsuario = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("loginInput")));
        WebElement campoSenha = driver.findElement(By.id("senhaInput"));
        WebElement botaoEntrar = driver.findElement(By.xpath("//button[contains(text(),'Entrar')]"));

        campoUsuario.sendKeys("admin");
        campoSenha.sendKeys("admin");
        botaoEntrar.click();

        // 6. Botão Cadastrar Ingredientes
        wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(text(),'Cadastrar Ingredientes')]"))).click();

        // 7. Preencher dados
        WebElement campoNome = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("nome")));
        campoNome.sendKeys("Pão Integral");

        WebElement selectElement = driver.findElement(By.name("tipo"));
        Select select = new Select(selectElement);
        select.selectByIndex(1);

        driver.findElement(By.name("quantidade")).sendKeys("10");

        WebElement compra = driver.findElement(By.name("ValorCompra"));
        compra.clear();
        compra.sendKeys("2.50");

        WebElement venda = driver.findElement(By.name("ValorVenda"));
        venda.clear();
        venda.sendKeys("4.00");

        try {
            driver.findElement(By.id("textArea1")).sendKeys("Pão Integral, sem gluten");
        } catch (Exception e) {
            driver.findElement(By.name("TextArea1")).sendKeys("Pão Integral, sem gluten");
        }

        // 8. Salvar
        driver.findElement(By.name("salvar")).click();

     // 9. Verificar Sucesso (Alerta)
        try {
            Alert alertaSucesso = wait.until(ExpectedConditions.alertIsPresent());
            String textoAlerta = alertaSucesso.getText();

   

            // Usa trim() para remover \n, \r, espaços extras no fim/início
            assertEquals(
                    "Ingrediente Salvo!",
                    textoAlerta.trim(),
                    "Mensagem do alerta de sucesso está incorreta!"
            );

            alertaSucesso.accept();

        } catch (TimeoutException e) {
            fail("O alerta de sucesso não apareceu!");
        }
        
        
        // 10. Ir para Estoque
        wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Estoque"))).click();
        Thread.sleep(5000);
    }
}
