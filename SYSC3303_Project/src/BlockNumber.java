
public class BlockNumber {
	private byte[] numbers;
	public final static byte MAX = 127;
	
	public BlockNumber (byte[] start) {
		if (start.length > 2 || start.length <= 0) System.exit(1);
		numbers = new byte[2];
		if (start.length == 1) numbers[1] = 0;
		else numbers[1] = start[1];
		
		numbers[0] = start[0];
	}
	
	public BlockNumber () {
		numbers = new byte[2];
		numbers[0] = 0;
		numbers[1] = 0;
	}
	
	public BlockNumber (byte x) {
		numbers = new byte[2];
		numbers[1] = x;
		numbers[0] = 0;
	}
	
	public byte[] getNext() {
		if (numbers[1] == MAX) {
			numbers[1] = 0;
			if (numbers[0] == MAX) {
				numbers[0] = 0;
			} else {
				numbers[0]++;
			}
		} else {
			numbers[1]++;
		}
		return numbers;
	}
	
	public byte[] getCurrent() {
		return this.numbers;
	}
	
	public void increment() {
		this.numbers = this.getNext();
	}
	
	public boolean compare(byte n[]) {
		boolean tester = true;
		if(n.length != numbers.length) tester =  false;
		if(n[0] != numbers[0]) tester =  false;
		if(n[1] != numbers[1]) tester =  false;
		return tester;
	}
	
	public boolean lessThanOrEqualTo(byte n[]) {
		if(n[1] < this.numbers[1]) return true;
		if(n[1] == this.numbers[1] && n[0] <= this.numbers[0])return true;
		return false;
	}
}
