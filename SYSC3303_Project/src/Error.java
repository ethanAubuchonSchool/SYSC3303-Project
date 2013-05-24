
public class Error {
	private int errorType;
	private byte blockType;
	private BlockNumber blockNumber;
	private int errorDetail;
	
	public Error(int et, byte bt, BlockNumber bn, int ed) {
		this.errorType = et;
		this.blockType = bt;
		this.blockNumber = bn;
		this.errorDetail = ed;
	}
	
	public Error(int et, byte bt, BlockNumber bn) {
		this(et, bt, bn, 0);
	}
	
	public Error() {
		this(0,(byte)0,new BlockNumber());
	}
	
	public int getErrorType() {
		return this.errorType;
	}
	
	public byte getBlockType() {
		return this.blockType;
	}
	
	public BlockNumber getBlockNumber() {
		return this.blockNumber;
	}
	
	public int getErrorDetail() {
		return this.errorDetail;
	}
}
