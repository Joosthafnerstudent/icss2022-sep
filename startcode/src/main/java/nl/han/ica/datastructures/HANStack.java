package nl.han.ica.datastructures;

public class HANStack<T> implements IHANStack<T>{
    private Node<T> top = null;

    // Plaatst een waarde bovenop de stack
    @Override
    public void push(T value) {
        top = new Node<>(value, top);
    }

    //Verwijderd de bovenste waarde van de stack
    @Override
    public T pop() {
        T value = peek();
        top = top.next;
        return value;
    }

    /*
    Geeft de boveste waarde terug

    returnt een IlligalArgumentException als de stack leeg is
     */
    @Override
    public T peek() {
      if (top == null){
          throw new IllegalArgumentException("Stack is empty");
      }
      return top.value;
    }
}
