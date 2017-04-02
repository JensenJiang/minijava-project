class Test {
	public static void main(String [] args) {
		int a;
		int b;
		int c;
		int[] t;
		boolean test;
		T ff;

		a = 1;
		b = 2;
		c = a.length;
		ff = a * b;
		test = !ff;
		c = true && a;
	}
}

class A{}

class B extends A{}

class C extends B{}

class base{
	public A hi(B t){return new A();}
}

class son extends base{
	public B test(){
		int c;
		int[] arr;
		base t;
		c = t.hi(new int[true]);
		arr[true] = t;
		return new A();
	}
}