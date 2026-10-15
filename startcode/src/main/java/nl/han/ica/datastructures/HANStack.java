package nl.han.ica.datastructures;

public class HANStack<T> implements IHANStack<T>{



    private Node<T> top = null;
    @Override
    public void push(T value) {
        top = new Node<>(value, top);
    }

    @Override
    public T pop() {
        T value = peek();
        top = top.next;
        return value;
    }

    @Override
    public T peek() {
      if (top == null){
          throw new IllegalArgumentException("Stack is empty");
      }
      return top.value;
    }
}
