# Mockito Object Injection

Inject Strings (or other objects) into your `@InjectMocks` targets [objects under test] without booting a Spring, Weld, CDI, Arquillian, EJB, or other container. Super lightweight and easy to use. Skip straight to Examples and Usage if you know what you're looking for.

## Problem

Take this Spring Controller (or if you're using the far superior and modern CDI framework, imagine this is `@AppplicationScoped`)

```
@Controller
public class MyController {
 @Value("securityEnabled")
 private Boolean securityEnabled;
 @Autowired
 private Authenticator auther;
 @Autowired
 private Logger log;

 public void doSomething() {
  if (securityEnabled) {
    auther.something();
  } else {
    log.warn("sec disabled");
  }
 }
}
```

If you wanted to write a _true unit test*_ with no external dependencies, you'd probably want to use Mockito mock your dependencies:

```
@ExtendWith({ MockitoExtension.class })
public class MyControllerTest {
 @InjectMocks
 private MyController myController;
 @Mock
 private Logger log;
 @Mock
 private Authenticator auther;
  
  public void testDoSomething() throws Exception {
   myController.doSomething();
   // results in NPE
  }
 }
```

## Examples

This JUnit5 extension allows you to arbitrarily set any field on your `@InjectMocks` [class under test] target. The injections happen _very late_; they happen when you call any non-private method on the class under test.


```
import com.github.exabrial.junit5.injectmap.InjectionMap;
import com.github.exabrial.junit5.injectmap.InjectMapExtension;

@ExtendWith({ MockitoExtension.class, InjectMapExtension.class })
public class MyControllerTest {
 @InjectMocks
 private MyController myController;
 @Mock
 private Logger log;
 @Mock
 private Authenticator auther;
 @InjectionMap
 private Map<String, Object> injectionMap = new HashMap<>();
 
 @BeforeEach
 public void beforeEach() throws Exception {
  injectionMap.put("securityEnabled", Boolean.TRUE);
 }

 @AfterEach
 public void afterEach() throws Exception {
  injectionMap.clear();
 }
  
 public void testDoSomething_secEnabled() throws Exception {
  myController.doSomething();
  // wahoo no NPE! Test the "if then" half of the branch
 }
  
 public void testDoSomething_secDisabled() throws Exception {
  injectionMap.put("securityEnabled", Boolean.FALSE);
  myController.doSomething();
  // wahoo no NPE! Test the "if else" half of branch
 }
}
```

## License

All files are licensed Apache Source License 2.0. Please consider contributing any improvements you make back to the project.

## Usage

Maven Coordinates:

```
<dependency>
 <groupId>org.junit.jupiter</groupId>
 <artifactId>junit-jupiter-api</artifactId>
 <version>5.2.0</version>
 <scope>test</scope>
</dependency>
<dependency>
 <groupId>org.mockito</groupId>
 <artifactId>mockito-core</artifactId>
 <version>2.22.0</version>
 <scope>test</scope>
</dependency>
<dependency>
 <groupId>com.github.exabrial</groupId>
 <artifactId>mockito-object-injection</artifactId>
 <version>1.0.1</version>
 <scope>test</scope>
</dependency>
```

The final note is that it should extraordinarily obvious that per-test forking will produce undefined results when using `@InjectMocks`, and as such, per-test forking is not thread-safe for this test extension.
