# Mockito Object Injection

Inject Strings (or other objects) into your `@InjectMocks` targets [objects under test] without booting a Spring, Weld, CDI, Arquillian, EJB, or other container. Super lightweight and easy to use. Skip straight to Examples and Usage if you know what you're looking for.

## Problem

Take this Spring Controller (or if you're using the much better CDI Framework, imagine this is `@AppplicationScoped`)

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

* _A true unit test means testing one unit of code, so firing up an Arquillian, Spring, or CDI container means you are probably not testing just one thing. In Java, a class is generally considered the smallest testable unit of code testable, but there are also static methods and lambdas which might also fall into this category._

Well... A lot of folks on the interwebs at this point will sigh loudly, rock back in their rocking chair, and hike up their trousers and yammer: "Well that kids is why we use CONSTRUCTOR INJECTION. You gootta expose that Boolean as a CONSTRUCTOOOR variable!!!" then proceed to continue to yell at the kids on their lawn.

There are a lot of well-defined benefits to constructor injection that I won't go into here. I don't like boilerplate code however, and I don't like generated code. So for me, constructor injection, despite it's advantages, brings a lot of verbosity and repetition to the table.

## Examples

This JUnit5 extension allows you to arbitrarily set any field on your `@InjectMocks` [class under test] target. The injections happen _very late_; they happen when you call any non-private method on the class under test.


```
import com.github.exabrial.junit5.injectmap.InjectMap;
import com.github.exabrial.junit5.injectmap.InjectMapExtension;

@ExtendWith({ MockitoExtension.class, InjectMapExtension.class })
public class MyControllerTest {
 @InjectMocks
 private MyController myController;
 @Mock
 private Logger log;
 @Mock
 private Authenticator auther;
 @InjectMap
 private Map<String, Object> injectMap = new HashMap<>();
 
 @BeforeEach
 public void beforeEach() throws Exception {
  injectMap.put("securityEnabled", Boolean.TRUE);
 }

 @AfterEach
 public void afterEach() throws Exception {
  injectMap.clear();
 }
  
 public void testDoSomething_secEnabled() throws Exception {
  myController.doSomething();
  // wahoo no NPE! Test the "if then" half of the branch
 }
  
 public void testDoSomething_secDisabled() throws Exception {
  injectMap.put("securityEnabled", Boolean.FALSE);
  myController.doSomething();
  // wahoo no NPE! Test the "if else" half of branch
 }
}
```
