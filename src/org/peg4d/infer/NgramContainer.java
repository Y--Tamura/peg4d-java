package org.peg4d.infer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NgramContainer<T> {
	private final List<T> body;
	
	@SuppressWarnings("unchecked")
	NgramContainer(T... args) {
		ArrayList<T> tmp = new ArrayList<T>();
		for (T arg : args) {
			if (arg == null) throw new IllegalArgumentException();
			tmp.add(arg);
		}
		this.body = Collections.unmodifiableList(tmp);
	}
	NgramContainer(List<T> args) {
		for (T arg : args) {
			if (arg == null) throw new IllegalArgumentException();
		}
		this.body = Collections.unmodifiableList(args);
	}
	
	T get(int n) {
		return this.body.get(n);
	}
	
	int size() {
		return this.body.size();
	}
	
	@Override
	public int hashCode() {
		int i, ret;
		for (ret = this.body.hashCode(), i = 0; i < this.body.size(); i++) {
			ret = ret ^ this.body.get(i).hashCode();
		}
		return ret;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		else if (obj == null) {
			return false;
		}
		else if (!(obj instanceof NgramContainer<?>)) {
			return false;
		}
		else {
			NgramContainer<T> obj2 = (NgramContainer<T>)obj;
			int size;
			if ((size = this.size()) != obj2.size()) {
				return false;
			}
			else {
				for (int i = 0; i < size; i++) {
					if (!this.get(i).equals(obj2.get(i))) return false;
				}
				return true;
			}
		}
	}

	@Override
	public String toString() {
		return this.body.toString();
	}
}
