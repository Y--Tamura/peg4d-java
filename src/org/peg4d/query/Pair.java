package org.peg4d.query;

public class Pair<L, R> {
	private L left;
	private R right;

	public Pair(L left, R right) {
		this.left = left;
		this.right = right;
	}

	public void setLeft(L left) {
		this.left = left;
	}

	public L getLeft() {
		return this.left;
	}

	public void setRight(R right) {
		this.right = right;
	}

	public R getRight() {
		return this.right;
	}

	@Override
	public String toString() {
		return "(" + this.left + ", " + this.right + ")";
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		else if (obj == null) {
			return false;
		}
		else if (!(obj instanceof Pair<?, ?>)) {
			return false;
		}
		else {
			Pair<L, R> obj2 = (Pair<L, R>)obj;
			if ((this.left != null && this.left.equals(obj2.getLeft())) &&
				(this.right != null && this.right.equals(obj2.getRight()))) {
				return true;
			}
			else {
				return false;
			}
		}
	}
}
