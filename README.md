# Mockito Object Injection

## Preamble

* Mock testing leads to highly focused tests where every variable is controlled.
* Dependency Injection + well structured programs + Mock testing = super clean codebase.

The problem is, how does one Mock a String, Boolean, or other final type if those types are being Injected?

## Summary 

This Junit Extension allows you to inject Strings (or any other object) into your `@InjectMocks` targets [objects under test] without booting a Spring, Weld, CDI, Arquillian, EJB, or other container. It's super lightweight, over 100,000x faster than booting Arquillian, and easy to use.

## Example Problem

Take this Spring Controller (or if you're using the far superior and modern CDI framework, think `@AppplicationScoped` instead of `@Controller` and `@Inject` instead of `@Autowired`)

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
class MyControllerTest {
 @InjectMocks
 private MyController myController;
 @Mock
 private Logger log;
 @Mock
 private Authenticator auther;
  
  @Test
  void testDoSomething() throws Exception {
   myController.doSomething();
   // results in NPE because myController.securityEnabled is null
  }
 }
```

* Testing a "unit" of code is a unit test. In Java, typically a class is the smallest unit of code. A static funciton can also be considered a unit of code, but static functions should not be tested using Mock testing for reasons beyond the scope of this document. See: http://misko.hevery.com/2008/12/15/static-methods-are-death-to-testability

## Example Solution

This JUnit5 extension allows you to arbitrarily set any field on your `@InjectMocks` [class under test] target. The injections happen _very late_; they happen when you call any non-private method on the class under test.


```
import com.github.exabrial.junit5.injectmap.InjectionMap;
import com.github.exabrial.junit5.injectmap.InjectMapExtension;

@TestInstance(Lifecycle.PER_CLASS)
@ExtendWith({ MockitoExtension.class, InjectExtension.class })
class MyControllerTest {
 @InjectMocks
 private MyController myController;
 @Mock
 private Logger log;
 @Mock
 private Authenticator auther;
 @InjectionSource
 private Boolean securityEnabled;
 
 @Test
 void testDoSomething_secEnabled() throws Exception {
  securityEnabled = Boolean.TRUE;
  myController.doSomething();
  // wahoo no NPE! Test the "if then" half of the branch
 }
 
 @Test 
 void testDoSomething_secDisabled() throws Exception {
  securityEnabled = Boolean.FALSE;
  myController.doSomething();
  // wahoo no NPE! Test the "if else" half of branch
 }
}
```

## @PostConstruct invocation

CDI and SpringFramework allow the use of `@PostConstruct`. This is like a constructor, except the method annotated will be invoked _after_ dependency injection is complete. This extension can be commanded to invoke the method annotated with `@PostConstruct` like so:


```
@ApplicationScoped
public class MyController {
 @Inject
 private Logger log; 
 
 @PostConstruct
 private void postConstruct() {
  log.info("initializing myController...");
  ... some initialization code
 }
}
```

```
@TestInstance(Lifecycle.PER_CLASS)
@ExtendWith({ MockitoExtension.class, InjectExtension.class })
class MyControllerTest {

 @InjectMocks
 @InvokePostConstruct
 // The postConstruct on this controller will be invoked
 private MyController myController;
 
 @Test
 void testSometing()
```

Ref: https://docs.oracle.com/javaee/7/api/javax/annotation/PostConstruct.html

## Enabling/Disabling Injection Behavior

The InjectExtension provides methods to turn on and off the behavior as well as query the status of injection.

```
InjectExtension.enable();
InjectExtension.bypass();
InjectExtension.status(); // returns true if enabled
```

It's recommended that if you're using the above APIs that you reset the Injector status between tests.

```
@AfterEach
void afterEach() {
    InjectExtension.enable();
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
 <version>5.8.2</version>
 <scope>test</scope>
</dependency>
<dependency>
 <groupId>org.mockito</groupId>
 <artifactId>mockito-core</artifactId>
 <version>4.6.1</version>
 <scope>test</scope>
</dependency>
<dependency>
 <groupId>com.github.exabrial</groupId>
 <artifactId>mockito-object-injection</artifactId>
 <version>2.1.0</version>
 <scope>test</scope>
</dependency>
```

The final note is that it should extraordinarily obvious that multithreading on the same test instances will cause problems. Set your forking policies accordingly to avoid race conditions.
