package mret.testfixtures;

public abstract class Animal {

    protected String name;
    private int age;

    public Animal(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String getName() { return name; }
    public int getAge() { return age; }
    public abstract String speak();
    protected void sleep(int hours) {}
}
