import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Scanner;

public class RsaSign
{
    public RsaSign(String signVerify, String filename)
    {
        // privScan is a scanner for the privkey.rsa file
        Scanner privScan = null;
        // pubScan is a scanner for the pubkey.rsa file
        Scanner pubScan = null;

        // code should be run as java RsaSign _ <filename>
        // therefore signVerify should be s or v, is the file being signed or verified
        // if user entered s
        if(signVerify.equals("s"))
        {
            // try catch block in case the privkey.rsa file can not be found
            try
            {
                privScan = new Scanner(new File("privkey.rsa"));
            }
            catch(FileNotFoundException err)
            {
                // if file can not be found, then print the stack trace
                err.printStackTrace();
            }
            // if the file could not be found, then privScan is null
            if(privScan == null)
            {
                // return because the rest of the program can not run without the privkey.rsa file
                return;
            }

            // program only gets this far if the privkey.rsa file does exist
            // the first line of the privkey.rsa file is the d value
            LargeInteger d = new LargeInteger(privScan.nextLine());
            // the second line of the privkey.rsa file is the n value
            LargeInteger n = new LargeInteger(privScan.nextLine());

            // sign the file
            sign(d, n, filename);
        }
        // if user entered v
        else if(signVerify.equals("v"))
        {
            // try catch block in case the pubkey.rsa file can not be file
            try
            {
                pubScan = new Scanner(new File("pubkey.rsa"));
            }
            catch(FileNotFoundException err)
            {
                // if file can not be found, then print the stack trace
                err.printStackTrace();
            }
            // if the file could not be found then pubScan is null
            if(pubScan == null)
            {
                // return because the rest of the program can not run without the pubkey.rsa file
                return;
            }

            // program only gets this far is the pubkey.rsa file does exist
            // the first line of the pubkey.rsa file is the e value
            LargeInteger e = new LargeInteger(pubScan.nextLine());
            // the second line of the pubkey.rsa file is the n value
            LargeInteger n = new LargeInteger(pubScan.nextLine());
            
            // verify the file signiture
            verify(e, n, filename);
        }
        // else user did not enter s or v
        else
        {
            System.out.println("Please retry and choose Sign Mode (s) or Verify Mode (v).");
            return;
        }
        
        // therefore filename should be the filename for the file that is going to be signed or verified
        if(filename == null)
        {
            System.out.println("Please enter the name of the file that you wish to sign or verify.");
            return;
        }
    }

    // private method that sha-256 hashes the file that the user passed into the program
    private LargeInteger getSha256(String filename)
    {
        // try catch block for a bunch of different exceptions
        try
        {
            // this code came from he HashEx file that was provided for the class
            Path path = Paths.get(filename);
            byte[] data = Files.readAllBytes(path);

            // create class instance to create SHA-256 hash
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            // process the file
            md.update(data);
            // generate a has of the file
            byte[] digest = md.digest();

            // after digesting everything, use the new byte array to create a large int
            LargeInteger hashed = new LargeInteger(digest);
            // pad the hashed value value to ensure that it is read as a positive number
            hashed.pad(1);
            // return the hashed value
            return hashed;
        }
        // if any of the exceptions occur
        catch(Exception e)
        {
            // print the stack trace
            e.printStackTrace();
        }
        // program only gets this far when there has been an exception, return null
        return null;
    }

    // sign the file
    public void sign(LargeInteger d, LargeInteger n, String filename)
    {
        // get the hash of the file the user provided
        LargeInteger hash = getSha256(filename);

        // signed = hash^d mod n
        LargeInteger signed = hash.modularExp(d, n);
     
        // try catch block when trying to write to a new file, creating a .sig file
        try
        {
            // printwriter that create filename + .sig file
            // if user gives hello.txt, the printwriter makes a hello.txt.sig file
            PrintWriter writer = new PrintWriter(new File(filename + ".sig"));
            // write signed to filename + .sig
            writer.println(signed.toStringWithoutSpace());
            // close writer so no issues arise
            writer.close();
        }
        catch(FileNotFoundException err)
        {
            // print stack trace if a filenotfoundexception occurs
            err.printStackTrace();
        }
    }

    // erify the signed file
    public void verify(LargeInteger e, LargeInteger n, String filename)
    {
        // create a new scanner
        Scanner scan = null;

        // try catch block to make sure that the .sig version of the file the user entered can be found
        try
        {
            scan = new Scanner(new File(filename + ".sig"));
        }
        // if the file can not be found
        catch(FileNotFoundException err)
        {
            // print the stack trace
            err.printStackTrace();
        }
        // if scan is null, then the file + .sig could not be found
        if(scan == null)
        {
            // return
            return;
        }

        // file only gets this far is the filename + .sig exists
        // get the sha256 hash of the original filename
        LargeInteger hash = getSha256(filename);

        // signiture is the first line of the file + .sig file
        LargeInteger signiture = new LargeInteger(scan.nextLine());

        // close the scanner so that no issue arise
        scan.close();

        // verify = signiture^e mod n
        LargeInteger verify = signiture.modularExp(e, n);

        // of hash = verify
        if(hash.toStringWithoutSpace().equals(verify.toStringWithoutSpace()))
        {
            // file was successfully verified
            System.out.println(filename + ": verification successful.");
        }
        else
        {
            // else the file failed to be verified
            System.out.println(filename + ": verificaiton failed.");
        }
    }

    public static void main(String args[])
    {
        // index will iterate through every value in the args[]
        int index;
        for(index = 0; index < args.length; index++)
        {
            // if index is 0 and args[0] is null
            if(index == 0 && args[index] == null)
            {
                // then the user did not enter a mode or a file to use
                System.out.println("Please choose either Sign mode (s) or Verify mode (v), and enter the name of the file you wish to sign or verify.");
                return;
            }
            // if index is 1 and args[1] is null
            if(index == 1 && args[index] == null)
            {
                // then the user did not enter one of the parameters
                System.out.println("Please enter the name of the file that you wish to sign or verify.");
                return;
            }
        }
        // program gets this far when the for loop ends
        if(index != 2)
        {
            // if index does not equal 2, then the user either enterd too many or too few parameters
            // either way, do not run the program, and tell the user to enter the mode and file name that they wish to run the program on
            System.out.println("Please enter the name of the file that you wish to sign or verify.");
            return;
        }
        // the program only makes it this far when the user has definitely entered enough parameters to try to run the program
        new RsaSign(args[0], args[1]);
    }
}