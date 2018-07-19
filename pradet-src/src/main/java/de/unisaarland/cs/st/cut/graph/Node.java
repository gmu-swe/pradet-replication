package de.unisaarland.cs.st.cut.graph;

public class Node {

	private String method;
	private String clazz;
	
	public Node(String clazz, String method){
		this.method = method;
		this.clazz = clazz;
	}
	
	public String toString(){
		return clazz+"."+method;
	}
	
	public String getClassName(){
		return clazz;
	}
	public String getMethodName(){
		return method;
	}
	
	public boolean equals(Object o){
		if(o == null || !(o instanceof Node)){
			return false;
		}
		Node oo = (Node) o;
		return oo.toString().equals(toString());
	}
	
	public int hashCode(){
		return toString().hashCode();
	}
	
}
