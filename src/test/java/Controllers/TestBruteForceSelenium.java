package Controllers;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

class TestBruteForceSelenium {

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
    void testeBruteForceLoginAdministrador() throws InterruptedException {

        driver.get(BASE_URL + "view/home/home.html");
        Thread.sleep(1000);

        try {
            WebElement botao = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(text(),'Acessar Cardápio')]")));
            botao.click();
        } catch (Exception ignored) {}

        try {
            Alert alerta = wait.until(ExpectedConditions.alertIsPresent());
            alerta.accept();
        } catch (TimeoutException ignored) {}

        wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Meu Carrinho"))).click();
        wait.until(ExpectedConditions.elementToBeClickable(By.partialLinkText("Funcionário?"))).click();

        WebElement campoUsuario = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("loginInput")));
        WebElement campoSenha = driver.findElement(By.id("senhaInput"));
        WebElement botaoEntrar = driver.findElement(By.xpath("//button[contains(text(),'Entrar')]"));

        String usuario = "admin";
        String[] tentativas = {"123", "senhaErrada", "test3", "admin1", "xyz"};

        for (int i = 0; i < tentativas.length; i++) {

            campoUsuario.clear();
            campoSenha.clear();
            campoUsuario.sendKeys(usuario);
            campoSenha.sendKeys(tentativas[i]);
            botaoEntrar.click();

            Thread.sleep(800);

            boolean loginBloqueado = false;

            try {
                // Procura alguma mensagem de erro
                driver.findElement(
                    By.xpath("//*[contains(text(),'senha incorreta') or contains(text(),'inválida') or contains(text(),'erro')]")
                );
                loginBloqueado = true;

            } catch (Exception e) {
                // Não encontrou mensagem de erro,
                // mas isso NÃO significa que logou corretamente → precisa checar
                try {
                    driver.findElement(By.id("logoutButton"));
                    Assertions.fail("O login foi aceito com senha inválida na tentativa " + (i + 1));
                } catch (NoSuchElementException ignored2) {
                    loginBloqueado = true; // Continua mesmo sem mensagem explícita
                }
            }

            System.out.println("Tentativa " + (i + 1) + " → bloqueada = " + loginBloqueado);
            Assertions.assertTrue(loginBloqueado, "Tentativa " + (i + 1) + " não deveria permitir login.");

        }

        System.out.println("Teste de brute force com 5 tentativas executado sem falhas.");
    }
}
