public class Word {
	private int myKey;
	private String myWord;
	private String myMeaning;

	public void setKey (int key) {
		this.myKey = key;
	}
	public void setWord (String word) {
		this.myWord = word;
	}
	public void setMeaning (String meaning) {
		this.myMeaning = meaning;
	}
	public int getKey() {
		return this.myKey;
	}
	public String getWord() {
		return this.myWord;
	}
	public String getMeaning() {
		return this.myMeaning;
	}
	public Word(int key, String word, String meaning){
		myKey = key;
		myWord = word;
		myMeaning = meaning;
	}
}

