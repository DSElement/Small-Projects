package mret.testfixtures;

@MyAnnotation("test-class")
public class AnnotatedClass {

    @MyAnnotation("test-field")
    public String label;

    @MyAnnotation("test-method")
    public void doSomething() {}
}
