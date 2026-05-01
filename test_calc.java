import com.bench.Calculator;

public class test_calc {
    public static void main(String[] args) {
        try {
            double result = Calculator.calculateDouble(0.0, 0.0, "log");
        } catch (ArithmeticException e) {
            System.out.println("log(0) error: " + e.getMessage());
        }
        
        try {
            double result = Calculator.calculateDouble(-5.0, 0.0, "log");
        } catch (ArithmeticException e) {
            System.out.println("log(-5) error: " + e.getMessage());
        }
    }
}
