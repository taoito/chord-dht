public class FingerTable{

	private int start;
	private int intervalBegin;
	private int intervalEnd;
	private Node successor;

	//Setters
	public void setStart(int newStart){
		this.start = newStart;
	}	

	public void setInterval(int begin, int end){
		this.intervalBegin = begin;
		this.intervalEnd = end;
	}

	public void setSuccessor(Node newSuccessor){
		this.successor = newSuccessor;
	}

	//Getters
	public int getStart(){
		return this.start;
	}

	public int getIntervalBegin(){
		return this.intervalBegin;
	}

	public int getIntervalEnd(){
		return this.intervalEnd;
	}

	public Node getSuccessor(){
		return this.successor;
	}

	public FingerTable(){
	}

	public FingerTable(int startID, Node succ) {
		start = startID;
		successor = succ;
	}
}
