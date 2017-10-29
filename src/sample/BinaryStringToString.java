package sample;

import java.nio.ByteBuffer;

public class BinaryStringToString {
    public static StringBuilder int2str( String s ) {
        StringBuilder s2 = new StringBuilder();
        char nextChar;
        for(int i = 0; i <= s.length() - 8; i += 8) //this is a little tricky.  we want [0, 7], [9, 16], etc (increment index by 9 if bytes are space-delimited)
        {
            nextChar = (char)Integer.parseInt(String.valueOf(s.substring(i, i + 8)), 2);
            s2.append(nextChar);
        }
        return s2;
    }
    public static Integer binaryToInteger(String binary){
        char[] numbers = binary.toCharArray();
        Integer result = 0;
        int count = 0;
        for(int i=numbers.length-1;i>=0;i--){
            if(numbers[i]=='1')result+=(int)Math.pow(2, count);
            count++;
        }
        return result;
    }
    private static String toASCII(int value) {
        int length = 4;
        StringBuilder builder = new StringBuilder(length);
        for (int i = length - 1; i >= 0; i--) {
            builder.append((char) ((value >> (8 * i)) & 0xFF));
        }
        return builder.toString();
    }
    public static void main(String[] args){
        byte[] test = new byte[] { (byte) 0x54, (byte) 0x45, (byte) 0x53, (byte) 0x54 };
        String s = "10101101";
        System.out.println(String.valueOf(binaryToInteger(s)));
        int value = ByteBuffer.wrap(test).getInt(); // 1413829460
        System.out.println(toASCII(value));
        String input = "011000010110000101100001";
        String output = "";
        for(int i = 0; i <= input.length() - 8; i+=8)
        {
            int k = Integer.parseInt(input.substring(i, i+8), 2);
            output += (char) k;
        }
        System.out.println(output);
        int i = 0;
        System.out.println(Integer.toBinaryString(i));
        byte b = 5;
        String k =("0000000" + Integer.toBinaryString(0xFF & b)).replaceAll(".*(.{8})$", "$1");
        System.out.println(k);
        b++;
        String m =("0000000" + Integer.toBinaryString(0xFF & b)).replaceAll(".*(.{8})$", "$1");
        System.out.println(m);

    }
}
