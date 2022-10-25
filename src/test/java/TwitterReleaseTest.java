import berlin.yuna.wiserjunit.model.TestCase;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Collections.emptyList;

class TwitterReleaseTest extends TestCase {

    private final AtomicReference<ChromeDriver> atomicDriver = new AtomicReference<>();
    private static final Integer TIMEOUT_MS = 10000;
    private static final Logger LOG = LoggerFactory.getLogger(TwitterReleaseTest.class);

    @BeforeEach
    public void setUp() {
        WebDriverManager.chromedriver().setup();
        atomicDriver.set(new ChromeDriver(new ChromeOptions()
//                .setHeadless(true)
                .addArguments("--no-sandbox")
                .addArguments("--disable-extensions")
                .addArguments("--disable-dev-shm-usage")
                .addArguments("--user-agent=\"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Safari/537.36\"")
        ));
    }


    @AfterEach
    public void tearDown() {
        try {
            Optional.ofNullable(atomicDriver.get()).ifPresent(RemoteWebDriver::quit);
            WebDriverManager.chromedriver().clearResolutionCache();
        } catch (Exception e) {
            LOG.warn("Browser quit error", e);
        } finally {
            atomicDriver.set(null);
        }
    }

    //https://webhook.site
    @Test
    void testSimple() throws InterruptedException {
        final var driver = atomicDriver.get();
        driver.get("https://twitter.com/home");
        final var document = Jsoup.parse(driver.getPageSource());
        findElement("input[autocomplete=username]").orElseThrow(() -> new RuntimeException("Login field not found [username]")).sendKeys("yuna-@web.de");
        findElements("div[role=button]").stream().filter(element -> element.getAttribute("innerHTML").toLowerCase().contains("next")).findFirst().orElseThrow(() -> new RuntimeException("Login field submit [next]")).click();

        findElement("input[autocomplete=on]").ifPresent(verification -> {
            verification.sendKeys("YunaMorgenstern");
            findElements("div[role=button]").stream().filter(element -> element.getAttribute("innerHTML").toLowerCase().contains("next")).findFirst().orElseThrow(() -> new RuntimeException("Login field submit [next]")).click();
        });

        findElement("input[autocomplete=current-password]").orElseThrow(() -> new RuntimeException("Login field not found [password]")).sendKeys("Zee1-ooso");
        findElements("div[role=button]").stream().filter(element -> element.getAttribute("innerHTML").toLowerCase().contains("log in")).findFirst().orElseThrow(() -> new RuntimeException("Login field submit [log in]")).click();
        System.out.println("Done");
        //yuna-@web.de Zee1-ooso
//        assertThat("Google", is(this.driver.getTitle()));
    }


    private Optional<WebElement> findElement(final String classSelector) {
        return timeOut(
                webDriver -> {
                    final List<WebElement> elements = findElements(classSelector);
                    return elements.isEmpty() ? Optional.empty() : Optional.ofNullable(elements.iterator().next());
                },
                Optional::isPresent,
                Optional.empty()
        );
    }

    private List<WebElement> findElements(final String classSelector) {
        try {
            return timeOut(
                    webDriver -> webDriver.findElements(By.cssSelector(classSelector)),
                    elements -> !elements.isEmpty(),
                    emptyList()
            );
        } catch (Exception ignored) {
            return emptyList();
        }
    }

    private <T> T timeOut(final Function<WebDriver, T> function, final Predicate<T> stop, final T fallback) {
        final var endTime = System.currentTimeMillis() + TIMEOUT_MS;
        do {
            final T result = function.apply(atomicDriver.get());
            if (stop.test(result)) {
                return result;
            }
            sleep(256);
        } while (System.currentTimeMillis() < endTime);
        return fallback;
    }

    private static void sleep(final long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
