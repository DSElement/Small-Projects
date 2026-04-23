package mret.testfixtures;

public class Dog extends Animal implements Speakable {

    public static final String SPECIES = "Canis lupus familiaris";
    private String breed;

    public Dog(String name, int age, String breed) {
        super(name, age);
        this.breed = breed;
    }

    @Override
    public String speak() { return "Woof!"; }

    public String getBreed() { return breed; }

    // Uses GenericContainer — tests USES relationship detection
    public void fetch(GenericContainer<String> items) {}
}
