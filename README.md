# Mockito Object Injection

Inject Strings (or other objects) into your `@InjectMocks` targets [objects under test] without booting a Spring, Weld, CDI, Arquillian, EJB, or other container. Super lightweight and easy to use. Skip straight to Examples and Usage if you know what you're looking for.

## Problem

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
public class MyControllerTest {
 @InjectMocks
 private MyController myController;
 @Mock
 private Logger log;
 @Mock
 private Authenticator auther;
  
  public void testDoSomething() throws Exception {
   myController.doSomething();
   // results in NPE because myController.securityEnabled is null
  }
 }
```

* Testing a "unit" of code is a unit test. In Java, typically a class is the smallest unit of code.

## Examples

This JUnit5 extension allows you to arbitrarily set any field on your `@InjectMocks` [class under test] target. The injections happen _very late_; they happen when you call any non-private method on the class under test.


```
import com.github.exabrial.junit5.injectmap.InjectionMap;
import com.github.exabrial.junit5.injectmap.InjectMapExtension;

@TestInstance(Lifecycle.PER_CLASS)
@ExtendWith({ MockitoExtension.class, InjectExtension.class })
public class MyControllerTest {
 @InjectMocks
 private MyController myController;
 @Mock
 private Logger log;
 @Mock
 private Authenticator auther;
 @InjectionSource
 private Boolean securityEnabled;
 
 public void testDoSomething_secEnabled() throws Exception {
  securityEnabled = Boolean.TRUE;
  myController.doSomething();
  // wahoo no NPE! Test the "if then" half of the branch
 }
  
 public void testDoSomething_secDisabled() throws Exception {
  securityEnabled = Boolean.FALSE;
  myController.doSomething();
  // wahoo no NPE! Test the "if else" half of branch
 }
}
```

## PostConstruct invocation

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
 @InjectMocks
 @InvokePostConstruct
 private MyController myController;
```

Ref: https://docs.oracle.com/javaee/7/api/javax/annotation/PostConstruct.html


## License

All files are licensed Apache Source License 2.0. Please consider contributing any improvements you make back to the project.

## Usage

Maven Coordinates:

```
<dependency>
 <groupId>org.junit.jupiter</groupId>
 <artifactId>junit-jupiter-api</artifactId>
 <version>5.6.2</version>
 <scope>test</scope>
</dependency>
<dependency>
 <groupId>org.mockito</groupId>
 <artifactId>mockito-core</artifactId>
 <version>3.5.0</version>
 <scope>test</scope>
</dependency>
<dependency>
 <groupId>com.github.exabrial</groupId>
 <artifactId>mockito-object-injection</artifactId>
 <version>2.0.0</version>
 <scope>test</scope>
</dependency>
```

The final note is that it should extraordinarily obvious that multithreading on the same test instances will cause problems. Set your forking policies accordingly to avoid race conditions.
