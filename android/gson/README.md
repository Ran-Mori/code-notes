## RFC

* [RFC7159 - The JavaScript Object Notation (JSON) Data Interchange Format](https://www.ietf.org/rfc/rfc7159.txt)

## JsonReader

* what for? - reads a json encoded value as a stream of tokens.

* feature 

  * the tokens are traversed in deepth-first order.
  * it is a recursive descent parser.

* code

  ```java
  public class JsonReader implements Closeable {
    public void beginArray() {}
    public void endObject() {}
    public boolean hasNext() {}
    public JsonToken peek() {}
    public String nextName() {}
    public String nextString() {}
  }
  ```

## TypeAdapter

* what for? - Converts Java objects to and from JSON.

* code

  ```java
  public abstract class TypeAdapter<T> {
    // Writes one JSON value (an array, object, string, number, boolean or null) for value.
    public abstract void write(JsonWriter out, T value);
    
    // Reads one JSON value and converts it to a Java object. Returns the converted object.
    public abstract T read(JsonReader in);
  }
  ```

* NumberTypeAdapter.java

  ```java
  public final class NumberTypeAdapter extends TypeAdapter<Number> {
    public Number read(JsonReader in) {
      JsonToken jsonToken = in.peek();
      switch (jsonToken) {
      case NUMBER:
      case STRING:
        return toNumberStrategy.readNumber(in);
      default:
        throw new JsonSyntaxException("Expecting number, got: ");
      }
    }
    
    public void write(JsonWriter out, Number value) {
      out.value(value);
    }
  }
  ```

* CollectionTypeAdapter.java

  ```java
  class Adapter<E> extends TypeAdapter<Collection<E>> {
    // adapter for handling element from/to json
    private final TypeAdapter<E> elementTypeAdapter;
    // use this to new a Collection
    private final ObjectConstructor<? extends Collection<E>> constructor;
    
    public Collection<E> read(JsonReader in) throws IOException {
      Collection<E> collection = constructor.construct();
      in.beginArray();
      while (in.hasNext()) {
        E instance = elementTypeAdapter.read(in);
        collection.add(instance);
      }
      in.endArray();
      return collection;
    }
    
    public void write(JsonWriter out, Collection<E> collection) {
      out.beginArray();
      for (E element : collection) {
        elementTypeAdapter.write(out, element);
      }
      out.endArray();
    }
  }
  ```

* FieldReflectionAdapter.java

  ```java
  FieldReflectionAdapter
    - ObjectConstructor: com.gson.Staff
  	- boundFields:
  		"age" to TypeAdapter,
  		"name" to TypeAdapter,
  		"position" to TypeAdapter,
  		"salary" to TypeAdapter,
  		"skills" to TypeAdapter,
  ```

## ConstructorConstructor

* code

  ```java
  public <T> ObjectConstructor<T> get(TypeToken<T> typeToken) {
    // user inject
    InstanceCreator<T> typeCreator = (InstanceCreator<T>) instanceCreators.get(type);
    // for specical collections
    ObjectConstructor<T> specialConstructor = newSpecialCollectionConstructor(type, rawType);
    // default constructor
    ObjectConstructor<T> defaultConstructor = newDefaultConstructor(rawType, filterResult);
    // for special collections
    ObjectConstructor<T> defaultImplementation = newDefaultImplementationConstructor(type, rawType);
    // use unsafe
    newUnsafeAllocator(rawType);
  }
  ```
