
public class Main {
    public static void main(String[] args) {
        Double a = Double.valueOf(34.434);
        Double b = Double.valueOf(34.434);
        double c = 34.434;
        synchronized (a) {
            
        }
        System.out.println("start___________________");
        System.out.println(a == c);
        System.out.println("end___________________");
    }
}
