package mret.testfixtures;

public interface Speakable {
    String speak();
    default String whisper() { return "..."; }
}
