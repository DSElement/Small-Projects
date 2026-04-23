package mret.testfixtures;

import java.util.List;

public class GenericContainer<T> {

    private List<T> items;
    private int size;

    public GenericContainer() {
        this.items = List.of();
        this.size = 0;
    }

    public void add(T item) {}
    public T get(int index) { return items.get(index); }
    public List<T> getAll() { return items; }
}
