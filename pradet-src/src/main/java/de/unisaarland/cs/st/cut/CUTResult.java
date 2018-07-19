package de.unisaarland.cs.st.cut;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;

/**
 * TODO Get rid of this, just make everything JUnit Specific
 * 
 * @author gambi
 *
 */
public class CUTResult implements Serializable {
	private static final long serialVersionUID = 6731002233970415389L;

	private HashMap<Description, List<Failure>> failing;
	private HashMap<Description, AtomicInteger> total;

	public CUTResult(HashMap<Description, AtomicInteger> total, HashMap<Description, List<Failure>> failing) {
		this.total = total;
		this.failing = failing;
	}

	public HashMap<Description, List<Failure>> getFailing() {
		return failing;
	}

	public int getTestsCount() {
		int count = 0;
		for (AtomicInteger i : total.values())
			count = count + i.get();
		return count;
	}
}
