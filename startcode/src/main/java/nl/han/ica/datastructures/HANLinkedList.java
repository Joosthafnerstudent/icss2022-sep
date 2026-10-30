package nl.han.ica.datastructures;

public class HANLinkedList<T> implements IHANLinkedList<T> {


    private Node<T> first = null;
    private int size = 0;

    // Voegt een waarde vooraan toe aan de lijst
    @Override
    public void addFirst(T value) {
    first = new Node<>(value, first);
    size++;
    }

    // Maakt de lijst leeg
    @Override
    public void clear() {
    first = null;
    size = 0;
    }

    // Voegt een waarde toe op de aangegeven positie
    @Override
    public void insert(int index, T value) {
    if( index < 0 || index > size){
        throw new IndexOutOfBoundsException("Index: " + index + "size: " + size);
    }
      if (index == 0){
          addFirst(value);
          return;
      }
      Node<T> previous = nodeAt(index - 1);
      previous.next = new Node<>(value, previous.next);
      size++;
    }

    // Verwijderd het element op de aangegeven positie
    @Override
    public void delete(int pos) {
    checkIndex(pos);
    if (pos == 0){
        removeFirst();
        return;
    }
    Node<T> previous = nodeAt(pos - 1);
    previous.next = previous.next.next;
    size--;
    }

    // Geeft het element op de juiste positie
    @Override
    public T get(int pos) {
        checkIndex(pos);
        return nodeAt(pos).value;
    }

    // Verwijdert het eerste element
    @Override
    public void removeFirst() {
    if (first == null){
        return;
    }
    first = first.next;
    size--;
    }

    // Geeft het eerste element terug
    @Override
    public T getFirst() {
        return first == null ? null: first.value;
    }


    @Override
    public int getSize() {
        return size;

    }

    // Controleerd of de positie een bestaand element aanwijst
    private void checkIndex(int pos){
        if (pos < 0 || pos >= size){
            throw new IndexOutOfBoundsException("Index: " + pos + "size: " + size);
        }
    }

    /*
    Loopt vanaf de eerste node door de lijst en geeft de bereikte node terug
     */
    private Node<T> nodeAt(int pos){
        Node<T> current = first;
        for (int i = 0; i < pos; i++){
            current = current.next;
        }
        return current;
    }
}
