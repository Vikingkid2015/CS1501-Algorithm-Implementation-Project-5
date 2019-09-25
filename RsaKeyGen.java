import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Random;


public class RsaKeyGen
{
    public RsaKeyGen()
    {
        // create a new random to fo large int
        Random random = new Random();
        // p and q will take the random and create new primes to be used in the rest of the calculations
        // p and q are 256 bit large ints
        LargeInteger p = new LargeInteger(256, random);
        LargeInteger q = new LargeInteger(256, random);
        // one_ is the byte array holding 00000001 for the one large int
        byte[] one_ = {(byte) 0x01};
        // large int that only holds value 00000001
        LargeInteger one = new LargeInteger(one_);
        // n = p * q
        LargeInteger n = p.multiply(q);
        // phiN = (p-1)*(q-1)
        LargeInteger phiN = p.subtract(one).multiply(q.subtract(one));
        // e starts as a 512 bit random prime large int
        LargeInteger e = new LargeInteger(512, random);
        // count of the number of iterations it takes to find an e value that works
        int count = 0;
        // start the current bit size for e at 512
        int curBitSize = 512;

        // while not 1 < e < phiN and gcd(e, phiN) != 1
        while(!(one.lessThan(e) && e.lessThan(phiN) && e.XGCD(phiN)[0].isOne()))
        {
            // create a new random prime e with curBitSize bit size
            e = new LargeInteger(curBitSize, random);
            // incriment count
            count++;

            // if count exceeds 50 iterations
            if(count > 50)
            {
                // decriment curBitSize
                curBitSize -= 1;
                // set count back to 0
                count = 0;
            }
        }

        // after the while loop ends, find xgcd(e, phiN) again and save it
        LargeInteger[] XGCDResult = e.XGCD(phiN);
        // set d to be the s value from xgcd
        LargeInteger d = XGCDResult[1];
        if(d.isNegative())
        {
            d = d.add(phiN);
        }

        // try catch block to make sure that no exceptions go unchecked
        try
        {
            // printwriter that will write the public key to file pubkey.rsa
            PrintWriter writer = new PrintWriter(new File("pubkey.rsa"));
            // write e to pubkey.rsa
            writer.println(e.toStringWithoutSpace());
            // write n to pubkey.rsa
            writer.println(n.toStringWithoutSpace());
            // close writer so no issues arise
            writer.close();

            // printwriter again, this time writing private key information to privkey.rsa
            writer = new PrintWriter(new File("privkey.rsa"));
            // write d to privkey.rsa
            writer.println(d.toStringWithoutSpace());
            // write n to privkey.rsa
            writer.println(n.toStringWithoutSpace());
            // close writer so no issues arise
            writer.close();
        }
        // if an exception is found throw a new FileNotFoundException
        catch(FileNotFoundException err)
        {
            // printt the stack trace for the FileNotFoundException
            err.printStackTrace();
        }
    }

    public static void main(String args[])
    {
        new RsaKeyGen();
    }
}