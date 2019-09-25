import java.util.Random;
import java.math.BigInteger;

public class LargeInteger {
	
	private final byte[] ONE = {(byte) 1};

	private byte[] val;

	// constructor if the large integer is created without any parameters, used in methods
	public LargeInteger()
	{
		// let val be a new byte array of size 1
		val = new byte[1];
		// add 00000000 as the only byte in the integer
		val[0] = 0x00;
	}

	/**
	 * Construct the LargeInteger from a given byte array
	 * @param b the byte array that this LargeInteger should represent
	 */
	public LargeInteger(byte[] b) {
		val = b;
	}

	// constructor when passed in just a character string of bits
	public LargeInteger(String bitString)
	{
		// the val array that back the large int is the length of the string of bits divided by 8 (i byte = 8 bits)
		val = new byte[bitString.length()/8];
		// index is in bytes
		int index = 0;
		// for each bit from the most significant to least, and increasing by 8 (so a single byte is looked at at a time)
		for(int i = 0; i < bitString.length(); i+=8)
		{
			// byte string is an 8 character substring of bit string
			String byteString = bitString.substring(i, i+8);
			// val at index index is the binary parsed version of byte string
			val[index] = (byte)Integer.parseInt(byteString, 2);
			// incriment the byte index
			index++;
		}
	}

	/**
	 * Construct the LargeInteger by generatin a random n-bit number that is
	 * probably prime (2^-100 chance of being composite).
	 * @param n the bitlength of the requested integer
	 * @param rnd instance of java.util.Random to use in prime generation
	 */
	public LargeInteger(int n, Random rnd) {
		val = BigInteger.probablePrime(n, rnd).toByteArray();
	}
	
	/**
	 * Return this LargeInteger's val
	 * @return val
	 */
	public byte[] getVal() {
		return val;
	}

	/**
	 * Return the number of bytes in val
	 * @return length of the val byte array
	 */
	public int length() {
		return val.length;
	}

	// add leading 0s to the front of the integer so comparisons can easily be done
	public void pad(int numBytes)
	{
		// newv is the array that will be padded, it is the original length of the array + the number of bytes added for padding
		byte[] newv = new byte[val.length + numBytes];
		// add each index of the original integer to the new integer, the remaining will be bytes with value 0
		for (int i = 0; i < val.length; i++) {
			// add the old value to the index that is its original place + the number of bytes added for padding
			newv[i + numBytes] = val[i];
		}
		// set val to be the new padded array
		val = newv;
	}

	/** 
	 * Add a new byte as the most significant in this
	 * @param extension the byte to place as most significant
	 */
	public void extend(byte extension) {
		byte[] newv = new byte[val.length + 1];
		newv[0] = extension;
		for (int i = 0; i < val.length; i++) {
			newv[i + 1] = val[i];
		}
		val = newv;
	}

	/**
	 * If this is negative, most significant bit will be 1 meaning most 
	 * significant byte will be a negative signed number
	 * @return true if this is negative, false if positive
	 */
	public boolean isNegative() {
		return (val[0] < 0);
	}

	/**
	 * Computes the sum of this and other
	 * @param other the other LargeInteger to sum with this
	 */
	public LargeInteger add(LargeInteger other) {
		byte[] a, b;
		// If operands are of different sizes, put larger first ...
		if (val.length < other.length()) {
			a = other.getVal();
			b = val;
		}
		else {
			a = val;
			b = other.getVal();
		}

		// ... and normalize size for convenience
		if (b.length < a.length) {
			int diff = a.length - b.length;

			byte pad = (byte) 0;
			if (b[0] < 0) {
				pad = (byte) 0xFF;
			}

			byte[] newb = new byte[a.length];
			for (int i = 0; i < diff; i++) {
				newb[i] = pad;
			}

			for (int i = 0; i < b.length; i++) {
				newb[i + diff] = b[i];
			}

			b = newb;
		}

		// Actually compute the add
		int carry = 0;
		byte[] res = new byte[a.length];
		for (int i = a.length - 1; i >= 0; i--) {
			// Be sure to bitmask so that cast of negative bytes does not
			//  introduce spurious 1 bits into result of cast
			carry = ((int) a[i] & 0xFF) + ((int) b[i] & 0xFF) + carry;

			// Assign to next byte
			res[i] = (byte) (carry & 0xFF);

			// Carry remainder over to next byte (always want to shift in 0s)
			carry = carry >>> 8;
		}

		LargeInteger res_li = new LargeInteger(res);
	
		// If both operands are positive, magnitude could increase as a result
		//  of addition
		if (!this.isNegative() && !other.isNegative()) {
			// If we have either a leftover carry value or we used the last
			//  bit in the most significant byte, we need to extend the result
			if (res_li.isNegative()) {
				res_li.extend((byte) carry);
			}
		}
		// Magnitude could also increase if both operands are negative
		else if (this.isNegative() && other.isNegative()) {
			if (!res_li.isNegative()) {
				res_li.extend((byte) 0xFF);
			}
		}

		// Note that result will always be the same size as biggest input
		//  (e.g., -127 + 128 will use 2 bytes to store the result value 1)
		return res_li;
	}

	/**
	 * Negate val using two's complement representation
	 * @return negation of this
	 */
	public LargeInteger negate() {
		byte[] neg = new byte[val.length];
		int offset = 0;

		// Check to ensure we can represent negation in same length
		//  (e.g., -128 can be represented in 8 bits using two's 
		//  complement, +128 requires 9)
		if (val[0] == (byte) 0x80) { // 0x80 is 10000000
			boolean needs_ex = true;
			for (int i = 1; i < val.length; i++) {
				if (val[i] != (byte) 0) {
					needs_ex = false;
					break;
				}
			}
			// if first byte is 0x80 and all others are 0, must extend
			if (needs_ex) {
				neg = new byte[val.length + 1];
				neg[0] = (byte) 0;
				offset = 1;
			}
		}

		// flip all bits
		for (int i  = 0; i < val.length; i++) {
			neg[i + offset] = (byte) ~val[i];
		}

		LargeInteger neg_li = new LargeInteger(neg);
	
		// add 1 to complete two's complement negation
		return neg_li.add(new LargeInteger(ONE));
	}

	/**
	 * Implement subtraction as simply negation and addition
	 * @param other LargeInteger to subtract from this
	 * @return difference of this and other
	 */
	public LargeInteger subtract(LargeInteger other) {
		return this.add(other.negate());
	}

	/**
	 * Compute the product of this and other
	 * @param other LargeInteger to multiply by this
	 * @return product of this and other
	 */
	public LargeInteger multiply(LargeInteger b) {
		// clone the integer used to call this so that the original does not get changed
		LargeInteger a = this.clone();
		// set be to be its clone so that the original does not get changed
		b = b.clone();
		
		boolean negativeResult = finalNegative(a.isNegative(), b.isNegative());

		if(a.isNegative())
		{
			a = a.negate();
		}
		if(b.isNegative())
		{
			b = b.negate();
		}

		// create a new integer that will be the result of the multiplication
		LargeInteger result = new LargeInteger();

		// while a is not zero
		while(!a.isZero())
		{
			// is the least significant bit of a is 1, then shouldAdd will be true, otherwise it will be false
			boolean shouldAdd = a.getLSB();

			// if a number should be added to the result
			if(shouldAdd)
			{
				// result is equal to itself + b
				result = result.add(b);
			}

			// right shift a
			a.shiftRightLogical();
			// left shift b
			b.shiftLeftLogical();
		}

		if(negativeResult)
		{
			result = result.negate();
		}

		// return the resulting value from the multiplication
		return result;
	}
	
	// struct that will hold the large integers that are used in XGCD
	private class  XGCD_Struct
	{
		// a is the large int calling the method
		// b is the large int being passed into method
		// d is the gcd
		// s and t are the extended multipliers
		LargeInteger a, b, d, s, t;
	}

	/**
	 * Run the extended Euclidean algorithm on this and other
	 * @param other another LargeInteger
	 * @return an array structured as follows:
	 *   0:  the GCD of this and other
	 *   1:  a valid x value
	 *   2:  a valid y value
	 * such that this * x + other * y == GCD in index 0
	 */
	public LargeInteger[] XGCD(LargeInteger other) {
		// result will hold the struct of the xgcd
		XGCD_Struct result = new XGCD_Struct();
		// set a to be what called the method
		result.a = this;
		// bset b to be what is passed into the method
		result.b = other;
		// call the helper method that will recursively solve the problem
		helperXGCD(result);
		// finalResult is the large int array that will be returned with the answers
		LargeInteger[] finalResult = new LargeInteger[3];
		// gcd is put in index 0
		finalResult[0] = result.d;
		// first multiplier is put in index 1
		finalResult[1] = result.s;
		// second multiplier is put in index 2
		finalResult[2] = result.t;

		// return the finalResult array containing the answer
		return finalResult;
	}

	// helper method for xgcd that will recursively solve the problem
	private void helperXGCD(XGCD_Struct struct)
	{
		// Base Case for the recursive call, if b is 0
		if(struct.b.isZero())
		{
			// create a new byte array only holding byte 00000001
			byte[] one = {(byte) 0x01};
			// set s equal to 00000001
			struct.s = new LargeInteger(one);
			// construct t with the default constructor that creates t as 00000000
			struct.t = new LargeInteger();
			// d is a, because at this point a must be the answer to gcd
			struct.d = struct.a;
			return;
		}

		// mod is a%b
		LargeInteger mod = struct.a.modulus(struct.b);
		// div is a/b
		LargeInteger div = struct.a.division(struct.b);

		// next will be used in the next call of helperXGCD
		XGCD_Struct next = new XGCD_Struct();
		// in the next call, a is the previous call's b
		next.a = struct.b;
		// in the next call, b is the previous call's a%b
		next.b = mod;

		// do the recursive call
		helperXGCD(next);

		// s = next call's t
		struct.s = next.t;
		// t = next call's s-(a/b)*next's t
		struct.t = next.s.subtract(div.multiply(next.t));
		// d = next's d
		struct.d = next.d;
	}

	 /**
	  * Compute the result of raising this to the power of y mod n
	  * @param b exponent to raise this to
	  * @param c modulus value to use
	  * @return this^b mod c
	  */
	public LargeInteger modularExp(LargeInteger b, LargeInteger c) {
		if(this.isNegative() || b.isNegative() || c.isNegative())
		{
			System.out.println("Number cannot be negative, in modular exponentiation.");
			return null;
		}
		
		//result = (a^b) mod c
		// a is what called this method
		LargeInteger a = this;
		// clone b so the large int does not change
		b = b.clone();
		// byte array that will be used in subtraction
		byte[] one = {(byte) 0x01};
		// result that will be returned
		LargeInteger result = new LargeInteger(one);
		char[] bitString = b.toStringWithoutSpace().toCharArray();

		for(char bit_ : bitString)
		// while b is not zero
		{
			int bit = bit_ == '1'? 1 :  0;
			result = result.multiply(result);
			if(bit == 1)
			{
				result = result.multiply(a);
			}
			result = result.modulus(c);
		}
		
		return result;
	}

	// trim the unnecessary leading 00000000 or 11111111, depending on if the large int is pos or neg
	public void trim()
	{
		// start the numBytesToTrim counter of number of bytes to tirm at 0
		int numByteToTrim = 0;
		// if the large int that called this method is positive
		if(!this.isNegative())
		{
			// while the value of the byte array at index numBytesToTrim is 00000000
			while(numByteToTrim < val.length && val[numByteToTrim] == 0)
			{
				// incriment numBytesToTrim
				numByteToTrim++;
			}
		}
		// else the large int that called with method is negative
		else
		{
			// while the value of the byte array at index numBytesToTrim is 11111111
			while(numByteToTrim < val.length && val[numByteToTrim] == (byte) 0xFF)
			{
				// incriment numBytesToTrim
				numByteToTrim++;
			}
		}

		// decriment numBytesToTrim by 1, because this will allow us to not have to 
		// not have to make an additional check as to whether the sign of the large int has changed
		numByteToTrim--;

		// if there is a number of bytes that need trimmed
		if(numByteToTrim > 0)
		{
			// the new length of the byte array will be either the current length-numBytesToTrim, or it will be 1, whichever is greater
			int newLength = Math.max((length() - numByteToTrim), 1);
			// the offset for what is being trimmed is the current length - NewLength
			int offset = length() - newLength;
			// create a byte array that is the size of the newLength
			byte[] newVal = new byte[newLength];

			// from the first index until the end of the newVal array
			for(int i = 0; i < newLength; i++)
			{
				// newVal at that index = the old value at that index + the offset
				newVal[i] = val[i + offset];
			}
			
			// set val to be the newVal
			val = newVal;
		}
	}

	// create a new integer that has the same value as the one that called this method
	public LargeInteger clone()
	{
		// create a new byte array for the clone that is the same length as what is being cloned
		byte[] clone = new byte[val.length];
		// for every index in the original array
		for(int i = 0; i < val.length; i++)
		{
			// clone of that index is equal to val of that index
			clone[i] = val[i];
		}
		// return a new integer that uses clone as its byte array
		return new LargeInteger(clone);
	}

	// format the byte array to be an easily readable printout
	private String byteToString(byte num)
	{
		return String.format("%8s", Integer.toBinaryString(num & 0xFF)).replace(' ', '0');
	}

	// use the byteToString method to print out an easily read version of the large integer as a sequence of bytes
	public String toString()
	{
		String print = "";
		for(int i = 0; i < val.length; i++)
		{
			print += byteToString(val[i]);
			print += " ";
		}
		return print;
	}

	public String toStringWithoutSpace()
	{
		String print = "";
		for(int i = 0; i < val.length; i++)
		{
			print += byteToString(val[i]);
		}
		return print;
	}

	// shift the bytes to the left by one bit
	public void shiftLeftLogical()
	{
		// if the most significant bit of the current byte is 1, then this will be true, so that a 1 is shifted in easily
		boolean shiftInOne = false;
		// true if the value started out negative, false otherwise
		boolean isNegativeBefore = isNegative();

		// for every index in the val array, starting from the last index and moving to the first index
		for(int i = val.length - 1; i >= 0; i--)
		{
			// temp is true if the current byte would be negativeon its own, that is, that its most significant bit is 1
			boolean temp = val[i] < 0;
			// shift the current index of val to the left by one bit
			val[i] = (byte) (val[i] << 1);
			// if the most significant bit of the previous index was 1
			if(shiftInOne)
			{
				// the current val should have shifted in a 1 instead of a 0, add 1 to as the new least significant bit in the byte
				val[i] = (byte) ((val[i] & 0xFF) + 1);
			}
			// the next iteration's shiftInOne is the current iteration's temp
			shiftInOne = temp;
		}

		// if after the for loop breaks, and there should still be a 1 shifted in
		if(shiftInOne)
		{
			// extend the current integer by one byte containing 00000001
			extend((byte) 0x01);
		}
		// if the integer changed from positive to negative
		if(!isNegativeBefore && isNegative())
		{
			// extend the current integer by one byte containing 00000000
			extend((byte) 0x00);
		}
	}

	// shift the bytes to the left by one bit
	public void shiftLeftLogicalNoExtend()
		{
		// if the most significant bit of the current byte is 1, then this will be true, so that a 1 is shifted in easily
		boolean shiftInOne = false;
	
		// for every index in the val array, starting from the last index and moving to the first index
		for(int i = val.length - 1; i >= 0; i--)
		{
			// temp is true if the current byte would be negativeon its own, that is, that its most significant bit is 1
			boolean temp = val[i] < 0;
			// shift the current index of val to the left by one bit
			val[i] = (byte) (val[i] << 1);
			// if the most significant bit of the previous index was 1
			if(shiftInOne)
			{
				// the current val should have shifted in a 1 instead of a 0, add 1 to as the new least significant bit in the byte
				val[i] = (byte) ((val[i] & 0xFF) + 1);
			}
			// the next iteration's shiftInOne is the current iteration's temp
			shiftInOne = temp;
		}
	}

	// shift the bytes to the right by 1
	public void shiftRightLogical()
	{
		// if the least significant bit of the current byte is 1 then true, otherwise false
		boolean shiftInOne = false;
		// for every index in the val array, starting from first index and moving to the last index
		for(int i = 0; i < val.length; i++)
		{
			// if the loeast significant bit of the current byte is 1, then temp is true, otherwise temp is false
			boolean temp = ((val[i] & 0xFF) & 0x01) == 1;
			//shift the current byte right by 1, without trying to retain the lost bit
			val[i] = (byte) ((val[i] & 0xFF) >>> 1);
			// if the least significant bit of he previous byte was 1
			if(shiftInOne)
			{
				// the current val should have shifted in a 1 instead of a 0, add 1 as the new most significant bit of the byte
				val[i] = (byte) ((val[i] & 0xFF) | 0x80);
			}
			// the shiftInOne for the next iteration is the current iteration's temp
			shiftInOne = temp;
		}
	}

	// this will determine whether the result of multiplication, division, and modulus should be reurned as a positive of negative number
	private boolean finalNegative(boolean a, boolean b)
	{
		// a is true if the first large int is negative
		// b is true is the second large int is negative
		// the finalNegative is true if only either a or b is negative, not if both or neither are
		// if a or b is negative, and a and b is not negative, return true, the finalNegative is true
		return (a || b) && !(a && b);
	}

	// return true if the most significant bit is 1, false if it is 0
	public boolean getMSB()
	{
		return val[0] < 0;
	}

	// return true if the least significant bit is 1, false if it is 0
	public boolean getLSB()
	{
		return ((val[val.length-1] & 0xFF) & 0x01) == 1;
	}

	// is the current integer 0, return true if yes, false if no
	public boolean isZero()
	{
		// for every index in the val array from first index to last
		for(int i = 0; i < val.length; i++)
		{
			// if the current byte is not 0
			if((val[i] & 0xFF) != 0)
			{
				// then the integer is not 0, return false
				return false;
			}
		}
		// if the program made it this far, then the integer is 0, return true
		return true;
	}

	// returns a boolean value for whether the large int is equal to 1
	public boolean isOne()
	{
		// for each byte in  val from first byte to one before the last byte
		for(int i = 0; i < val.length - 1; i++)
		{
			// if the value of that byte is not 0, then the large int is not equal to 1
			if(val[i] != 0)
			{
				// return false
				return false;
			}
		}
		// if the program made it this far, then the last byte must be checked to make sure that it is equal to 1
		// if val at index length - 1 = 1, returns true, else returns false
		return val[val.length-1] == 1;
	}

	// change the least significant bit to be whatever is passed in, 1 if true, 0 if false
	public void setLSB(boolean LSB)
	{
		val[val.length - 1] = (byte) (val[val.length - 1] & 0xFE);
		if(LSB)
		{
			val[val.length - 1] = (byte) ((val[val.length - 1] & 0xFF) + 1);
		}
	}

	// change the most significant bit to be whatever ispassed in, 1 if true, 0 if false
	public void setMSB(boolean MSB)
	{
		val[0] = (byte) (val[0] & 0x7F);
		if(MSB)
		{
			val[0] = (byte) ((val[0] & 0xFF) | 0x80);
		}
	}

	// divide the integer that called with method by the integer passed into this method, return quotient
	public LargeInteger division(LargeInteger divisor)
	{
		// set divisor to be a clone of the large int that was passed into the method
		divisor = divisor.clone();
		// set dividend to be a clone of the large int that called the method
		LargeInteger dividend = this.clone();

		// determine if the result shoudl be positive or negative
		boolean negativeResult = finalNegative(divisor.isNegative(), dividend.isNegative());

		// if dividend is negative
		if(dividend.isNegative())
		{
			// negate dividend so it is positive
			dividend = dividend.negate();
		}

		// if divisor is negative
		if(divisor.isNegative())
		{
			// negate divisor so it is positive
			divisor = divisor.negate();
		}

		// max will be the larger length of the byte array between dividend and divisor
		int max = Math.max(dividend.length(), divisor.length());
		
		// create remainder so that it is the length of max
		LargeInteger remainder = new LargeInteger(new byte[max]);
		// crete quotient so that it is the length of max
		LargeInteger quotient = new LargeInteger(new byte[max]);

		// amount is the number of times the loop will run, it runs for each bit in dividend
		int amount = dividend.length() * 8 + 1;
	
		// for each bit in dividend
		for(int i = 0; i < amount; i++)
		{
			// shift quotient left by 1 bit
			quotient.shiftLeftLogical();
			// if divisor is less than or equal to remainder
			if(divisor.lessThanOrEqual(remainder))
			{
				// remainder = remainder - divisor
				remainder = remainder.subtract(divisor);
				// set quotient's least significant bit = 1
				quotient.setLSB(true);
			}
			// if i is less than amount - 1
			if(i < (amount - 1))
			{
				// shift remainder left by one bit
				remainder.shiftLeftLogical();
				// set remainder's least significant bit to be dividend's most significant bit
				remainder.setLSB(dividend.getMSB());
				// shift dividend left by one bit without extending it
				dividend.shiftLeftLogicalNoExtend();
			}
		}
		
		// if negativeResult is true
		if(negativeResult)
		{
			// negate quotient
			quotient = quotient.negate();
		}
		// return quotient
		return quotient;
	}

	// divide the integer that called with method by the integer passed into this method, return remainder
	public LargeInteger modulus(LargeInteger divisor)
	{
		// divisor is a clone of the large int that was passed into the method
		divisor = divisor.clone();
		// dividend is a clone of the large int that called this method
		LargeInteger dividend = this.clone();

		// determine whether the result should be negative
		boolean negativeResult = finalNegative(divisor.isNegative(), dividend.isNegative());

		// if dividend is negative
		if(dividend.isNegative())
		{
			// negate dividend
			dividend = dividend.negate();
		}

		// if divisor is negative
		if(divisor.isNegative())
		{
			// negate divisor
			divisor = divisor.negate();
		}

		// max is the max length between the byte arrays in divisor and dividend
		int max = Math.max(dividend.length(), divisor.length());
		
		// set remainder to be the size of the max
		LargeInteger remainder = new LargeInteger(new byte[max]);
		// set quotient to be the size of the max
		LargeInteger quotient = new LargeInteger(new byte[max]);
		// amount is the number of bits + 1 that the loop will run
		int amount = dividend.length() * 8 + 1;
	
		// for each bit in the dividend
		for(int i = 0; i < amount; i++)
		{
			// shift quotient left by one bit
			quotient.shiftLeftLogical();
			// if divisor is less than or equal to remainder
			if(divisor.lessThanOrEqual(remainder))
			{
				// remainder = remainder - divisor
				remainder = remainder.subtract(divisor);
				// set quotient's least significant bit to be 1
				quotient.setLSB(true);
			}
			// if i is less than amount - 1
			if(i < (amount - 1))
			{
				// shift remainder left by 1 bit
				remainder.shiftLeftLogical();
				// set remainder's least significant bit to be dividend's most significant bit
				remainder.setLSB(dividend.getMSB());
				// shift dividend left by 1 bit without extending it
				dividend.shiftLeftLogicalNoExtend();
			}
		}

		// if the finalresult should be negative
		if(negativeResult)
		{
			// negate remainder
			remainder = remainder.negate();
		}

		// trim remainder, in case it has leading 11111111s or 00000000s
		remainder.trim();

		// return remainder
		return remainder;
	}

	// determine whether the integer that calls this method is less than or equal to the integer that is passed into the method
	public boolean lessThanOrEqual(LargeInteger other)
	{
		// max will be the maxiumum of the number of bytes within the two integers (in case one has more bytes than the other)
		int max = Math.max(this.length(), other.length());

		// these two integers will be clones of the two used when calling this method, so that padding can be done without changing the value of any integers
		LargeInteger a_ = this.clone();
		LargeInteger b_ = other.clone();

		// if a_ has less byte than the maximum number of bytes
		if(a_.length() < max)
		{
			// pad a_ with max - the number of bytes that a_ already has, therefore making it have the same number of bytes as b_
			a_.pad(max-a_.length());
		}
		// else b_ will have less than the maximum number of bytes
		else
		{
			// pad b_ with max - the number of bytes that b_ already has, therefore making it have the same number of bytes as a_
			b_.pad(max-b_.length());
		}

		// create new byte arrays that will hold the values of a_ and b_
		byte[] a = a_.getVal();
		byte[] b = b_.getVal();

		// for each index within the a and b byte arrays, from first index to last
		for(int i = 0; i < a.length; i++)
		{
			// if the integer version of a is less than the integer version of b at that index
			if((a[i]&0xFF) < (b[i]& 0xFF))
			{
				// then a is less than b, therefore return true
				return true;
			}
			// else if the integer version of a is greater than the integer version of b at that index
			else if((a[i]&0xFF) > (b[i]& 0xFF))
			{
				// then a is greater than b, therefore return false
				return false;
			}
			// else the integer version at this index for both are equal
			else
			{
				// continue to the next iteration of the loop
				continue;
			}
		}
		// if the code made it this far, then the two integers must be equal, therefore return true as well
		return true;
	}

	// determine whether the integer that calls this method is less than the integer that is passed into the method
	public boolean lessThan(LargeInteger other)
	{
		// max will be the maxiumum of the number of bytes within the two integers (in case one has more bytes than the other)
		int max = Math.max(this.length(), other.length());

		// these two integers will be clones of the two used when calling this method, so that padding can be done without changing the value of any integers
		LargeInteger a_ = this.clone();
		LargeInteger b_ = other.clone();

		// if a_ has less byte than the maximum number of bytes
		if(a_.length() < max)
		{
			// pad a_ with max - the number of bytes that a_ already has, therefore making it have the same number of bytes as b_
			a_.pad(max-a_.length());
		}
		// else b_ will have less than the maximum number of bytes
		else
		{
			// pad b_ with max - the number of bytes that b_ already has, therefore making it have the same number of bytes as a_
			b_.pad(max-b_.length());
		}

		// create new byte arrays that will hold the values of a_ and b_
		byte[] a = a_.getVal();
		byte[] b = b_.getVal();

		// for each index within the a and b byte arrays, from first index to last
		for(int i = 0; i < a.length; i++)
		{
			// if the integer version of a is less than the integer version of b at that index
			if((a[i]&0xFF) < (b[i]& 0xFF))
			{
				// then a is less than b, therefore return true
				return true;
			}
			// else if the integer version of a is greater than the integer version of b at that index
			else if((a[i]&0xFF) > (b[i]& 0xFF))
			{
				// then a is greater than b, therefore return false
				return false;
			}
			// else the integer version at this index for both are equal
			else
			{
				// continue to the next iteration of the loop
				continue;
			}
		}
		// if the code made it this far, then the two integers must be equal, therefore return false
		return false;
	}
}