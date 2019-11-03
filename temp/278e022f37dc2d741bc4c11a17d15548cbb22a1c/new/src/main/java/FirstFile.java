import java.util.HashMap;
import java.util.Map;


public class FirstFile {
	
	private HashMap map;
	
	public FirstFile(HashMap inputMap) {
		this.map = inputMap;
	}
	
	public void method11(String input1) {
		System.out.println(input1);
	}
	
	public Integer method12(Integer input1, Integer input2) {
		System.out.println(input1);
		return input1 * input2;
	}
	
	public void method13() {
		System.out.println("To be deleted");
	}
}